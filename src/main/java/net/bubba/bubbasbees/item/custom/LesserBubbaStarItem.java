package net.bubba.bubbasbees.item.custom;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Reusable catalyst for the Lesser-tier gems (every gem recipe gives it back); used up in the Bubba Star recipe. */
public class LesserBubbaStarItem extends Item {
    public LesserBubbaStarItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
            java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        tooltip.add(net.minecraft.network.chat.Component.translatable("item.bubbasbees.bubba_star.hive_mode").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
