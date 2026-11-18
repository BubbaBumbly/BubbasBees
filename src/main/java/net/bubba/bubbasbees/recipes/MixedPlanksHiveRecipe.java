package net.bubba.bubbasbees.recipes;

import net.bubba.bubbasbees.registries.ModBlocks;
import net.bubba.bubbasbees.registries.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * The advanced hive recipe with two different planks either side of the beehive, at least one of
 * them a hive wood (#bubbasbees:hive_planks): mixed woods make the default oak hive. Matching hive
 * planks are the per-wood JSON recipes; planks of no hive wood are the oak JSON recipe.
 * <pre>
 *   # $ #     # honeycomb, $ honey block, H beehive, A and B different planks
 *   A H B
 *   # $ #
 * </pre>
 */
public class MixedPlanksHiveRecipe extends CustomRecipe {
    public static final TagKey<Item> HIVE_PLANKS =
            ItemTags.create(ResourceLocation.fromNamespaceAndPath("bubbasbees", "hive_planks"));

    public MixedPlanksHiveRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.width() != 3 || input.height() != 3) return false;
        for (int i : new int[]{0, 2, 6, 8}) if (!input.getItem(i).is(Items.HONEYCOMB)) return false;
        for (int i : new int[]{1, 7}) if (!input.getItem(i).is(Items.HONEY_BLOCK)) return false;
        if (!input.getItem(4).is(Items.BEEHIVE)) return false;
        ItemStack a = input.getItem(3), b = input.getItem(5);
        return a.is(ItemTags.PLANKS) && b.is(ItemTags.PLANKS) && !a.is(b.getItem())
                && (a.is(HIVE_PLANKS) || b.is(HIVE_PLANKS));
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        // keep the bees and honey level of a beehive that was picked up with silk touch
        ItemStack result = new ItemStack(ModBlocks.ADVANCED_BEEHIVE.get());
        ItemStack hive = input.getItem(4);
        result.set(DataComponents.BEES, hive.get(DataComponents.BEES));
        result.set(DataComponents.BLOCK_STATE, hive.get(DataComponents.BLOCK_STATE));
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.MIXED_PLANKS_HIVE.get();
    }
}
