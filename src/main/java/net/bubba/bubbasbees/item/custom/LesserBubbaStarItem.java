package net.bubba.bubbasbees.item.custom;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Reusable catalyst for the Lesser-tier gems (every gem recipe gives it back); used up in the Bubba Star recipe. */
public class LesserBubbaStarItem extends Item {
    public LesserBubbaStarItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return new ItemStack(this);
    }
}
