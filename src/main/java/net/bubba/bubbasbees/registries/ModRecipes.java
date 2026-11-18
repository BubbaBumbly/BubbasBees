package net.bubba.bubbasbees.registries;

import net.bubba.bubbasbees.BubbasBees;
import net.bubba.bubbasbees.recipes.ConsumingShapedRecipe;
import net.bubba.bubbasbees.recipes.HiveConversionRecipe;
import net.bubba.bubbasbees.recipes.MixedPlanksHiveRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, BubbasBees.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<HiveConversionRecipe>> HIVE_CONVERSION =
            RECIPE_SERIALIZERS.register("hive_conversion", HiveConversionRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MixedPlanksHiveRecipe>> MIXED_PLANKS_HIVE =
            RECIPE_SERIALIZERS.register("mixed_planks_hive", () -> new SimpleCraftingRecipeSerializer<>(MixedPlanksHiveRecipe::new));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ConsumingShapedRecipe>> CONSUMING_SHAPED =
            RECIPE_SERIALIZERS.register("consuming_shaped", ConsumingShapedRecipe.Serializer::new);

    public static void register(IEventBus eventBus) {
        RECIPE_SERIALIZERS.register(eventBus);
    }
}
