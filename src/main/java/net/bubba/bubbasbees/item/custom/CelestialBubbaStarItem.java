package net.bubba.bubbasbees.item.custom;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** The top-tier star, made by the night ritual. A catalyst: every recipe gives it back. */
public class CelestialBubbaStarItem extends Item {
    public CelestialBubbaStarItem(Properties properties) {
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

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
