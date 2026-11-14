package net.bubba.bubbasbees.client.jade;

import net.bubba.bubbasbees.BubbasBees;
import net.bubba.bubbasbees.client.LookInfo;
import net.bubba.bubbasbees.entity.custom.BubbasBee;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.TooltipPosition;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;

import java.util.List;

/**
 * Jade tooltip provider for the mod's bees (and vanilla bees). It reuses {@link LookInfo#lines} so Jade shows the
 * same lines as the in-world HUD. On a baby Bubba's bee, Jade's own "Growing time: Ns" line would repeat our
 * "Baby: grows up in m:ss" line, so our first line takes over its slot (fix list item 53).
 */
public enum LookInfoEntityProvider implements IEntityComponentProvider {
    INSTANCE;

    private static final ResourceLocation UID = new ResourceLocation(BubbasBees.MOD_ID, "look_info_entity");

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        List<Component> lines = LookInfo.lines(Minecraft.getInstance(), accessor.getHitResult());
        if (lines.isEmpty()) return;
        IElementHelper helper = tooltip.getElementHelper();
        List<Component> ours = lines.subList(1, lines.size());   // Jade already shows the name
        int start = 0;
        if (accessor.getEntity() instanceof BubbasBee && !ours.isEmpty() && replaceJadeGrowth(tooltip, helper, ours.get(0))) {
            start = 1;
        }
        for (Component line : ours.subList(start, ours.size())) {
            tooltip.add(helper.text(line));
        }
    }

    /** Puts line in place of Jade's untagged "Growing time" line (found by its text key); false when it isn't there. */
    private static boolean replaceJadeGrowth(ITooltip tooltip, IElementHelper helper, Component line) {
        try {
            for (int i = 0; i < tooltip.size(); i++) {
                List<IElement> left = tooltip.get(i, IElement.Align.LEFT);
                for (int j = 0; j < left.size(); j++) {
                    Component message = left.get(j).getMessage();
                    if (message != null && message.getContents() instanceof TranslatableContents key
                            && key.getKey().equals("jade.mobgrowth.time")) {
                        left.set(j, helper.text(line));
                        return true;
                    }
                }
            }
        } catch (RuntimeException ignored) {
            // another Jade version keeps its lines read-only: both lines show
        }
        return false;
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public int getDefaultPriority() {
        return TooltipPosition.BODY + 1;   // after Jade's growth line (BODY), so it is there to replace
    }
}
