package net.bubba.bubbasbees.client.jade;

import net.bubba.bubbasbees.BubbasBees;
import net.bubba.bubbasbees.client.LookInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.TooltipPosition;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElementHelper;

import java.util.List;

/**
 * Jade tooltip provider for the mod's non-hive blocks. It reuses {@link LookInfo#lines} so Jade shows the same
 * lines as the in-world HUD.
 */
public enum LookInfoBlockProvider implements IBlockComponentProvider {
    INSTANCE;

    private static final ResourceLocation UID = new ResourceLocation(BubbasBees.MOD_ID, "look_info_block");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        List<Component> lines = LookInfo.lines(Minecraft.getInstance(), accessor.getHitResult());
        if (lines.isEmpty()) return;
        IElementHelper helper = tooltip.getElementHelper();
        for (Component line : lines.subList(1, lines.size())) {   // Jade already shows the name
            tooltip.add(helper.text(line));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public int getDefaultPriority() {
        return TooltipPosition.BODY;
    }
}
