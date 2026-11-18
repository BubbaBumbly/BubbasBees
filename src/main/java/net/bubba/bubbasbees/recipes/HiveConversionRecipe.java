package net.bubba.bubbasbees.recipes;

import com.mojang.serialization.MapCodec;
import net.bubba.bubbasbees.registries.ModDataComponents;
import net.bubba.bubbasbees.registries.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

/**
 * A shaped recipe that turns a hive into another one (a vanilla beehive into an advanced hive, an
 * advanced hive into another wood or the reinforced hive), keeping the hive's bees, combs and honey level (the item's data components) instead of handing back
 * an empty hive.
 */
public class HiveConversionRecipe extends ShapedRecipe {
    private final ShapedRecipe delegate;

    // 1.21 only hands a crafting table recipes that are CraftingRecipes, so this must be a real ShapedRecipe
    public HiveConversionRecipe(ShapedRecipe delegate) {
        super(delegate.getGroup(), delegate.category(),
                new ShapedRecipePattern(delegate.getWidth(), delegate.getHeight(), delegate.getIngredients(), java.util.Optional.empty()),
                delegate.getResultItem(null), delegate.showNotification());
        this.delegate = delegate;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack result = super.assemble(input, registries);
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.getItem() instanceof BlockItem item && item.getBlock() instanceof BeehiveBlock) {
                // Copy the stored bees, combs and block state (honey level, facing) onto the result
                result.set(DataComponents.BEES, stack.get(DataComponents.BEES));
                result.set(ModDataComponents.BEEHIVE_COMBS.get(), stack.get(ModDataComponents.BEEHIVE_COMBS.get()));
                result.set(DataComponents.BLOCK_STATE, stack.get(DataComponents.BLOCK_STATE));
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
        public MapCodec<HiveConversionRecipe> codec() {
            return ShapedRecipe.Serializer.CODEC.xmap(HiveConversionRecipe::new, recipe -> recipe.delegate);
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, HiveConversionRecipe> streamCodec() {
            return ShapedRecipe.Serializer.STREAM_CODEC.map(HiveConversionRecipe::new, recipe -> recipe.delegate);
        }
    }
}
