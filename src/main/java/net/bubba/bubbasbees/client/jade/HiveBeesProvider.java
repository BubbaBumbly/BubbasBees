package net.bubba.bubbasbees.client.jade;

import net.bubba.bubbasbees.BubbasBees;
import net.bubba.bubbasbees.blockentities.HiveOccupant;
import net.bubba.bubbasbees.client.HiveInfoOverlay;
import net.bubba.bubbasbees.util.HiveInfo;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.TooltipPosition;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;

import java.util.List;

/** Calm/busy mode and smoke countdown, then one Jade tooltip line per stored bee (face icon, name, release countdown), like HiveInfoOverlay. */
public enum HiveBeesProvider implements IBlockComponentProvider {
    INSTANCE;

    private static final ResourceLocation UID = new ResourceLocation(BubbasBees.MOD_ID, "hive_bees");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!HiveInfo.isHive(accessor.getBlockState())) return;
        IElementHelper helper = tooltip.getElementHelper();
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
