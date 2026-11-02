package net.bubba.bubbasbees.recipes;

import com.google.gson.JsonObject;
import net.bubba.bubbasbees.registries.ModRecipes;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

/**
 * A shaped recipe that uses up its catalysts: no crafting remainders. The Lesser Bubba Star is a reusable catalyst
 * in gem recipes but is consumed when crafted into the Bubba Star (finish-spec.md "Star recipes").
 */
public class ConsumingShapedRecipe extends ShapedRecipe {

    public ConsumingShapedRecipe(ShapedRecipe recipe) {
        super(recipe.getId(), recipe.getGroup(), recipe.getWidth(), recipe.getHeight(),
                recipe.getIngredients(), recipe.getResultItem());
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer container) {
        return NonNullList.withSize(container.getContainerSize(), ItemStack.EMPTY);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.CONSUMING_SHAPED.get();
    }

    public static class Serializer implements RecipeSerializer<ConsumingShapedRecipe> {
        @Override
        public ConsumingShapedRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new ConsumingShapedRecipe(RecipeSerializer.SHAPED_RECIPE.fromJson(id, json));
        }

        @Override
        public ConsumingShapedRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new ConsumingShapedRecipe(RecipeSerializer.SHAPED_RECIPE.fromNetwork(id, buffer));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ConsumingShapedRecipe recipe) {
            RecipeSerializer.SHAPED_RECIPE.toNetwork(buffer, recipe);
        }
    }
}
