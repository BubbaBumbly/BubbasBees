package net.bubba.bubbasbees.item.custom;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class BubbaStarItem extends Item {
    public BubbaStarItem(Properties properties) {
        super(properties.craftRemainder(Items.BEDROCK));
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return new ItemStack(this);  // Should return self
    }
}