package net.bubba.bubbasbees.recipes;

import com.mojang.serialization.MapCodec;
import net.bubba.bubbasbees.registries.ModRecipes;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

/**
 * A shaped recipe that uses up its catalysts: no crafting remainders. The Lesser Bubba Star is a reusable catalyst
 * in gem recipes but is consumed when crafted into the Bubba Star.
 */
public class ConsumingShapedRecipe extends ShapedRecipe {
    private final ShapedRecipe delegate;

    // 1.21 only hands a crafting table recipes that are CraftingRecipes, so this must be a real ShapedRecipe
    public ConsumingShapedRecipe(ShapedRecipe delegate) {
        super(delegate.getGroup(), delegate.category(),
                new ShapedRecipePattern(delegate.getWidth(), delegate.getHeight(), delegate.getIngredients(), java.util.Optional.empty()),
                delegate.getResultItem(null), delegate.showNotification());
        this.delegate = delegate;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        return NonNullList.withSize(input.size(), ItemStack.EMPTY);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.CONSUMING_SHAPED.get();
    }

    public static class Serializer implements RecipeSerializer<ConsumingShapedRecipe> {
        @Override
        public MapCodec<ConsumingShapedRecipe> codec() {
            return ShapedRecipe.Serializer.CODEC.xmap(ConsumingShapedRecipe::new, recipe -> recipe.delegate);
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ConsumingShapedRecipe> streamCodec() {
            return ShapedRecipe.Serializer.STREAM_CODEC.map(ConsumingShapedRecipe::new, recipe -> recipe.delegate);
        }
    }
}
