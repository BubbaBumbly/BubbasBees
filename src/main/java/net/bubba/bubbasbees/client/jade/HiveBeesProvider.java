// DEVIATION: Jade 15 removed ITooltip.getElementHelper; the shared factory is IElementHelper.get().
package net.bubba.bubbasbees.client.jade;

import net.bubba.bubbasbees.BubbasBees;
import net.bubba.bubbasbees.blockentities.HiveOccupant;
import net.bubba.bubbasbees.client.HiveInfoOverlay;
import net.bubba.bubbasbees.util.HiveInfo;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.bubba.bubbasbees.blocks.custom.AdvancedBeehiveBlock;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.TooltipPosition;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;
import snownee.jade.impl.ui.TextElement;

import java.util.List;

/** Calm/busy mode and smoke countdown, then one Jade tooltip line per stored bee (face icon, name, release countdown), like
 *  HiveInfoOverlay; while sneaking (or Jade's details key), one line per comb type in an advanced hive. */
public enum HiveBeesProvider implements IBlockComponentProvider {
    INSTANCE;

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(BubbasBees.MOD_ID, "hive_bees");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!HiveInfo.isHive(accessor.getBlockState())) return;
        IElementHelper helper = IElementHelper.get();
        if (accessor.getBlockState().getBlock() instanceof AdvancedBeehiveBlock) showCombs(tooltip, helper, HiveInfo.honeyLevel(accessor.getBlockState()));
        long gameTime = Minecraft.getInstance().level == null ? 0L : Minecraft.getInstance().level.getGameTime();
        Component mode = HiveInfo.modeText(accessor.getBlockEntity());
        if (mode != null) {
            tooltip.add(helper.text(mode.copy().withStyle(ChatFormatting.GRAY)));
        }
        long smoke = HiveInfo.smokeRemaining(accessor.getBlockEntity(), gameTime);
        if (smoke > 0L) {
            tooltip.add(helper.text(HiveInfo.smokeText(smoke).copy()
                    .withStyle(HiveInfo.smokeEnding(smoke) ? ChatFormatting.GOLD : ChatFormatting.GRAY)));
        }
        List<HiveOccupant> bees = HiveInfo.occupants(accessor.getBlockEntity());
        for (HiveOccupant bee : bees) {
            tooltip.add(new BeeIconElement(bee.id()));
            tooltip.append(helper.text(HiveInfo.beeName(bee.id())));
            tooltip.append(helper.spacer(HiveInfoOverlay.TIMER_GAP, 0));
            tooltip.append(helper.text(HiveInfo.timerText(bee, gameTime).copy().withStyle(ChatFormatting.GRAY))
                    .align(IElement.Align.RIGHT));
        }
        if (accessor.showDetails() || HiveInfoOverlay.showCombs(Minecraft.getInstance())) {
            for (ItemStack comb : HiveInfo.combs(accessor.getBlockEntity())) {
                tooltip.add(helper.item(new ItemStack(comb.getItem()), 0.5F));
                tooltip.append(helper.text(comb.getHoverName()));
                tooltip.append(helper.spacer(HiveInfoOverlay.TIMER_GAP, 0));
                tooltip.append(helper.text(HiveInfo.countText(comb.getCount()).copy().withStyle(ChatFormatting.GRAY))
                        .align(IElement.Align.RIGHT));
            }
        }
    }

    /** Jade's own beehive line reads vanilla's 0-5 honey level; on an advanced hive it shows the comb count, 0-8, instead.
     *  Jade 15 keeps a line's Component in TextElement's public text field (IElement.getMessage()
     *  returns the rendered string), so the line is found by its text key there and swapped in place. */
    private static void showCombs(ITooltip tooltip, IElementHelper helper, int combs) {
        Component line = Component.translatable("jade.beehive.honey", Component.translatable("jade.fraction", combs, 8)
                .withStyle(combs >= 8 ? ChatFormatting.GREEN : ChatFormatting.WHITE));
        try {
            for (int i = 0; i < tooltip.size(); i++) {
                List<IElement> left = tooltip.get(i, IElement.Align.LEFT);
                for (int j = 0; j < left.size(); j++) {
                    if (left.get(j) instanceof TextElement text && text.text instanceof Component message
                            && message.getContents() instanceof TranslatableContents key
                            && key.getKey().equals("jade.beehive.honey")) {
                        left.set(j, helper.text(line));
                        return;
                    }
                }
            }
        } catch (RuntimeException | LinkageError ignored) {
            // another Jade version keeps its lines read-only: leave its line as it is
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    /** After Jade's own beehive lines (honey, bee count), which sit at BODY. */
    @Override
    public int getDefaultPriority() {
        return TooltipPosition.BODY + 1000;
    }
}
