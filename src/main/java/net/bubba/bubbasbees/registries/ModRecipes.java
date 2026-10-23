package net.bubba.bubbasbees.registries;

import net.bubba.bubbasbees.BubbasBees;
import net.bubba.bubbasbees.recipes.HiveConversionRecipe;
import net.bubba.bubbasbees.recipes.MixedPlanksHiveRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleRecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, BubbasBees.MOD_ID);

    public static final RegistryObject<RecipeSerializer<HiveConversionRecipe>> HIVE_CONVERSION =
            RECIPE_SERIALIZERS.register("hive_conversion", HiveConversionRecipe.Serializer::new);

    public static final RegistryObject<RecipeSerializer<MixedPlanksHiveRecipe>> MIXED_PLANKS_HIVE =
            RECIPE_SERIALIZERS.register("mixed_planks_hive", () -> new SimpleRecipeSerializer<>(MixedPlanksHiveRecipe::new));

    public static void register(IEventBus eventBus) {
        RECIPE_SERIALIZERS.register(eventBus);
    }
}
