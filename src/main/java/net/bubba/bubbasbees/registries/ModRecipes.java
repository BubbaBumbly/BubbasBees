package net.bubba.bubbasbees.registries;

import net.bubba.bubbasbees.BubbasBees;
import net.bubba.bubbasbees.recipes.ConsumingShapedRecipe;
import net.bubba.bubbasbees.recipes.HiveConversionRecipe;
import net.bubba.bubbasbees.recipes.MixedPlanksHiveRecipe;
import net.bubba.bubbasbees.util.Reg;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;

public class ModRecipes {
    private static ResourceLocation rl(String name) {
        return ResourceLocation.fromNamespaceAndPath(BubbasBees.MOD_ID, name);
    }

    public static final Reg<RecipeSerializer<HiveConversionRecipe>> HIVE_CONVERSION = Reg.of(rl("hive_conversion"));
    public static final Reg<RecipeSerializer<MixedPlanksHiveRecipe>> MIXED_PLANKS_HIVE = Reg.of(rl("mixed_planks_hive"));
    public static final Reg<RecipeSerializer<ConsumingShapedRecipe>> CONSUMING_SHAPED = Reg.of(rl("consuming_shaped"));

    public static void register() {
        HIVE_CONVERSION.set(Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, HIVE_CONVERSION.id(),
                new HiveConversionRecipe.Serializer()));
        MIXED_PLANKS_HIVE.set(Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, MIXED_PLANKS_HIVE.id(),
                new SimpleCraftingRecipeSerializer<>(MixedPlanksHiveRecipe::new)));
        CONSUMING_SHAPED.set(Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, CONSUMING_SHAPED.id(),
                new ConsumingShapedRecipe.Serializer()));
    }
}
