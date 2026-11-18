package net.bubba.bubbasbees.item.custom;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** The top-tier star, made by the night ritual. A catalyst: every recipe gives it back. */
public class CelestialBubbaStarItem extends Item {
    public CelestialBubbaStarItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
            java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        tooltip.add(net.minecraft.network.chat.Component.translatable("item.bubbasbees.bubba_star.hive_mode").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
