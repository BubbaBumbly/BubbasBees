package net.bubba.bubbasbees.item;

import net.bubba.bubbasbees.registries.ModBlocks;
import net.bubba.bubbasbees.registries.ModItems;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

        /** Grouped order (JEI's item list follows it too): see {@link #order()}. */
        @Override
        public void fillItemList(@NotNull NonNullList<ItemStack> items) {
            for (Item item : order()) item.fillItemCategory(this, items);
        }
    };

    /**
     * The tab's items in order: getting started, hives (plain and reinforced paired per wood), comb chute, stars,
     * gems by tier, ritual blocks, royal jelly things, combs, spawn eggs. Any other item of this tab (one added later
     * and not placed here yet) goes at the end, so nothing is ever left out.
     */
    public static List<Item> order() {
        Set<Item> order = new LinkedHashSet<>();
        add(order, ModItems.ENCYCLOBEEDIA, ModItems.COPPER_SMOKER, ModItems.SMOKER, ModItems.BEE_JAR);
        for (RegistryObject<Block> hive : ModBlocks.HIVES) order.add(hive.get().asItem());
        add(order, ModItems.COMB_CHUTE, ModItems.LESSER_BUBBA_STAR, ModItems.BUBBA_STAR, ModItems.CELESTIAL_BUBBA_STAR);
        for (Item gem : ModItems.allGems()) order.add(gem);
        add(order, ModBlocks.CELESTIAL_ALTAR, ModBlocks.STONE_PEDESTAL, ModBlocks.DEEPSLATE_PEDESTAL,
                ModBlocks.BLACKSTONE_PEDESTAL, ModBlocks.QUARTZ_PEDESTAL, ModItems.STARDUST);
        add(order, ModItems.ROYAL_JELLY, ModBlocks.ROYAL_JELLY_BLOCK, ModBlocks.PINK_STICKY_PISTON);
        order.addAll(ModItems.allCombs());
        order.addAll(ModItems.allSpawnEggs());
        for (Item item : ForgeRegistries.ITEMS) {
            if (item.getItemCategory() == BUBBASBEES_TAB) order.add(item);
        }
        return new ArrayList<>(order);
    }

    @SafeVarargs
    private static void add(Set<Item> order, RegistryObject<? extends ItemLike>... entries) {
        for (RegistryObject<? extends ItemLike> entry : entries) order.add(entry.get().asItem());
    }
}
