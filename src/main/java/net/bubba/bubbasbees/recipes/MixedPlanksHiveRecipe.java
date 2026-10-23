package net.bubba.bubbasbees.recipes;

import net.bubba.bubbasbees.registries.ModBlocks;
import net.bubba.bubbasbees.registries.ModRecipes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
    public static final TagKey<Item> HIVE_PLANKS = ItemTags.create(new ResourceLocation("bubbasbees", "hive_planks"));

    public MixedPlanksHiveRecipe(ResourceLocation id) {
        super(id);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        if (container.getWidth() != 3 || container.getHeight() != 3) return false;
        for (int i : new int[]{0, 2, 6, 8}) if (!container.getItem(i).is(Items.HONEYCOMB)) return false;
        for (int i : new int[]{1, 7}) if (!container.getItem(i).is(Items.HONEY_BLOCK)) return false;
        if (!container.getItem(4).is(Items.BEEHIVE)) return false;
        ItemStack a = container.getItem(3), b = container.getItem(5);
        return a.is(ItemTags.PLANKS) && b.is(ItemTags.PLANKS) && !a.is(b.getItem())
                && (a.is(HIVE_PLANKS) || b.is(HIVE_PLANKS));
    }

    @Override
    public ItemStack assemble(CraftingContainer container) {
        return new ItemStack(ModBlocks.ADVANCED_BEEHIVE.get());
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
