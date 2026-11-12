package net.bubba.bubbasbees.client.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.bubba.bubbasbees.BubbasBees;
import net.bubba.bubbasbees.bees.BeeSpec;
import net.bubba.bubbasbees.bees.BeeSpecs;
import net.bubba.bubbasbees.recipes.MixedPlanksHiveRecipe;
import net.bubba.bubbasbees.registries.ModBlocks;
import net.bubba.bubbasbees.registries.ModItems;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JEI support (JEI is optional; this class only loads when it is installed): "how to get / what it does" info pages
 * for every Bubba's Bees item, built from the bee roster (BeeSpecs) so comb rates never go out of date, and the
 * mixed-planks hive recipe, which JEI skips because it is a special recipe. Texts: lang keys jei.bubbasbees.*.
 * The item list's order comes from the creative tab (ModCreativeModeTab.order).
 */
@JeiPlugin
public class BubbasBeesJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = new ResourceLocation(BubbasBees.MOD_ID, "jei_plugin");

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        Map<Item, List<Component>> info = new LinkedHashMap<>();

        page(info, ModItems.ENCYCLOBEEDIA.get(), "encyclobeedia");
        page(info, ModItems.COPPER_SMOKER.get(), "smoker");
        page(info, ModItems.SMOKER.get(), "smoker");
        page(info, ModItems.BEE_JAR.get(), "bee_jar");
        for (RegistryObject<Block> hive : ModBlocks.HIVES) {
            Item item = hive.get().asItem();
            page(info, item, "hive");
            page(info, item, "hive.modes");
            if (hive.getId().getPath().startsWith("reinforced_")) page(info, item, "hive.reinforced");
        }
        page(info, ModItems.COMB_CHUTE.get(), "comb_chute");
        page(info, ModItems.COMB_CHUTE.get(), "comb_chute.modes");

        page(info, ModItems.LESSER_BUBBA_STAR.get(), "star");
        page(info, ModItems.LESSER_BUBBA_STAR.get(), "lesser_bubba_star");
        page(info, ModItems.BUBBA_STAR.get(), "star");
        page(info, ModItems.BUBBA_STAR.get(), "bubba_star");
        page(info, ModItems.CELESTIAL_BUBBA_STAR.get(), "celestial_bubba_star");
        page(info, ModItems.CELESTIAL_BUBBA_STAR.get(), "star");

        page(info, ModBlocks.CELESTIAL_ALTAR.get(), "celestial_altar");
        for (RegistryObject<Block> pedestal : ModBlocks.PEDESTALS) {
            if (pedestal != ModBlocks.CELESTIAL_ALTAR) page(info, pedestal.get(), "pedestal");
        }
        page(info, ModItems.STARDUST.get(), "stardust");
        page(info, ModItems.ROYAL_JELLY.get(), "royal_jelly");
        page(info, ModBlocks.ROYAL_JELLY_BLOCK.get(), "royal_jelly_block");
        page(info, ModBlocks.PINK_STICKY_PISTON.get(), "pink_sticky_piston");

        for (BeeSpec spec : BeeSpecs.ALL) {
            Component bee = Component.translatable("entity.bubbasbees." + spec.entityId());
            // Combs: which bee brings each one home, and how often
            float rareTotal = 0;
            for (BeeSpec.RareComb rare : spec.rareCombs()) {
                rareTotal += 1F / rare.oneIn();
                line(info, combItem(rare.comb()), Component.translatable("jei.bubbasbees.comb.rare", bee,
                        Component.translatable("jei.bubbasbees.chance.one_in", rare.oneIn())));
            }
            int weights = spec.combs().stream().mapToInt(BeeSpec.WeightedComb::weight).sum();
            for (BeeSpec.WeightedComb comb : spec.combs()) {
                float chance = spec.combChance() * comb.weight() / weights;
                line(info, combItem(comb.comb()), Component.translatable(
                        rareTotal > 0 ? "jei.bubbasbees.comb.own_with_rare" : "jei.bubbasbees.comb.own", bee,
                        chance(chance)));
            }
            // Gems and spawn eggs: how to get the bee
            if (spec.hasGem()) {
                Item gem = ModItems.gem(spec.id());
                line(info, gem, Component.translatable("jei.bubbasbees.gem", bee));
                page(info, gem, "gem.tempt");
                line(info, ModItems.spawnEgg(spec.id()), Component.translatable("jei.bubbasbees.spawn_egg",
                        gem.getDescription()));
            } else {
                page(info, ModItems.spawnEgg(spec.id()), "spawn_egg.royal_queen");
            }
        }
        page(info, Items.HONEYCOMB, "honeycomb");
        page(info, Items.CAULDRON, "cauldron");

        info.forEach((item, lines) -> registration.addIngredientInfo(new ItemStack(item), VanillaTypes.ITEM_STACK,
                lines.toArray(Component[]::new)));

        registration.addRecipes(RecipeTypes.CRAFTING, List.of(mixedPlanksHive()));
    }

    /** The mixed-planks hive recipe as JEI can show it: two different planks, at least one a hive wood, make oak. */
    private static CraftingRecipe mixedPlanksHive() {
        Ingredient comb = Ingredient.of(Items.HONEYCOMB), honey = Ingredient.of(Items.HONEY_BLOCK);
        NonNullList<Ingredient> grid = NonNullList.of(Ingredient.EMPTY,
                comb, honey, comb,
                Ingredient.of(MixedPlanksHiveRecipe.HIVE_PLANKS), Ingredient.of(Items.BEEHIVE), Ingredient.of(ItemTags.PLANKS),
                comb, honey, comb);
        return new ShapedRecipe(new ResourceLocation(BubbasBees.MOD_ID, "jei/advanced_beehive_mixed_planks"), "",
                3, 3, grid, new ItemStack(ModBlocks.ADVANCED_BEEHIVE.get()));
    }

    private static Item combItem(String comb) {
        return BeeSpec.VANILLA_HONEYCOMB.equals(comb) ? Items.HONEYCOMB : ModItems.comb(comb);
    }

    /** "every trip", "1 in 4 trips", or a percentage when it isn't a whole 1 in N. */
    private static Component chance(float chance) {
        if (chance >= 0.999F) return Component.translatable("jei.bubbasbees.chance.every");
        float oneIn = 1F / chance;
        if (Math.abs(oneIn - Math.round(oneIn)) < 0.01F) {
            return Component.translatable("jei.bubbasbees.chance.one_in", Math.round(oneIn));
        }
        return Component.translatable("jei.bubbasbees.chance.percent", String.format("%.1f", chance * 100));
    }

    private static void page(Map<Item, List<Component>> info, ItemLike item, String key) {
        line(info, item.asItem(), Component.translatable("jei.bubbasbees." + key));
    }

    private static void line(Map<Item, List<Component>> info, Item item, Component text) {
        info.computeIfAbsent(item, i -> new ArrayList<>()).add(text);
    }
}
