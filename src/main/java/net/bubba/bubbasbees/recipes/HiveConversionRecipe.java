package net.bubba.bubbasbees.recipes;

import com.google.gson.JsonObject;
import net.bubba.bubbasbees.blocks.custom.AdvancedBeehiveBlock;
import net.bubba.bubbasbees.registries.ModRecipes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

/**
 * A shaped recipe that turns one advanced beehive into another (another wood, or the reinforced
 * hive), keeping the hive's bees, combs and honey level (the item's NBT) instead of handing back
 * an empty hive.
 */
public class HiveConversionRecipe extends ShapedRecipe {

    public HiveConversionRecipe(ShapedRecipe recipe) {
        super(recipe.getId(), recipe.getGroup(), recipe.getWidth(), recipe.getHeight(),
                recipe.getIngredients(), recipe.getResultItem());
    }

    @Override
    public ItemStack assemble(CraftingContainer container) {
        ItemStack result = super.assemble(container);
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.getItem() instanceof BlockItem item && item.getBlock() instanceof AdvancedBeehiveBlock
                    && stack.hasTag()) {
                result.setTag(stack.getTag().copy());
                break;
            }
        }
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.HIVE_CONVERSION.get();
    }

    public static class Serializer implements RecipeSerializer<HiveConversionRecipe> {
        @Override
        public HiveConversionRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new HiveConversionRecipe(RecipeSerializer.SHAPED_RECIPE.fromJson(id, json));
        }

        @Override
        public HiveConversionRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new HiveConversionRecipe(RecipeSerializer.SHAPED_RECIPE.fromNetwork(id, buffer));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, HiveConversionRecipe recipe) {
            RecipeSerializer.SHAPED_RECIPE.toNetwork(buffer, recipe);
        }
    }
}
