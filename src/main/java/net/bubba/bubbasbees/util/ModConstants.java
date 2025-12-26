package net.bubba.bubbasbees.util;

import net.bubba.bubbasbees.registries.ModItems;
import net.minecraft.world.item.Item;

import java.util.List;

public class ModConstants {
    public static final List<Item> CONVERSION_ITEMS = List.of(
            ModItems.COPPER_CONVERSION_GEM.get(), ModItems.SANDY_CONVERSION_GEM.get(), ModItems.ENDER_CONVERSION_GEM.get(),
            ModItems.GOLD_CONVERSION_GEM.get(), ModItems.NATURE_CONVERSION_GEM.get(), ModItems.IRON_CONVERSION_GEM.get(),
            ModItems.REDSTONE_CONVERSION_GEM.get(), ModItems.STONE_CONVERSION_GEM.get());
}
