package net.bubba.bubbasbees.item;

import net.bubba.bubbasbees.registries.ModBlocks;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ModCreativeModeTab {
    public static final CreativeModeTab BUBBASBEES_TAB = new CreativeModeTab("bubbasbeestab") {
        @Override
        public @NotNull ItemStack makeIcon() {
            return new ItemStack(ModBlocks.ADVANCED_BEEHIVE.get());
        }

        @Override
        public boolean hasSearchBar() {
            return false;
        }
    };
}
