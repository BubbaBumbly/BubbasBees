package net.bubba.bubbasbees.registries;

// DEVIATION: the stars' self-remainder is set through an accessor because vanilla has no stack-sensitive remainder hook.
import net.bubba.bubbasbees.BubbasBees;
import net.bubba.bubbasbees.bees.BeeSpec;
import net.bubba.bubbasbees.bees.BeeSpecs;
import net.bubba.bubbasbees.item.custom.*;
import net.bubba.bubbasbees.mixin.ItemCraftingRemainderAccessor;
import net.bubba.bubbasbees.util.Reg;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ModItems {
    private static ResourceLocation rl(String name) {
        return ResourceLocation.fromNamespaceAndPath(BubbasBees.MOD_ID, name);
    }

    /// General
    public static final Reg<Item> COPPER_SMOKER = Reg.of(rl("copper_smoker"));
    /** The iron smoker keeps the original id, so smokers already in worlds stay. */
    public static final Reg<Item> SMOKER = Reg.of(rl("smoker"));
    public static final Reg<Item> BUBBA_STAR = Reg.of(rl("bubba_star"));
    public static final Reg<Item> LESSER_BUBBA_STAR = Reg.of(rl("lesser_bubba_star"));
    /** Top-tier star, made by the night ritual; reusable catalyst for the ore-bee gems. */
    public static final Reg<Item> CELESTIAL_BUBBA_STAR = Reg.of(rl("celestial_bubba_star"));

    /** The starlight bee's product; placed like redstone dust (the ritual circle). */
    public static final Reg<Item> STARDUST = Reg.of(rl("stardust"));
    /** Comb chute block (placed under a hive, see CombChuteBlock); flat icon in the inventory. */
    public static final Reg<Item> COMB_CHUTE = Reg.of(rl("comb_chute"));
    /** The royal queen's product: 1 hunger bar, 1.5 saturation (tagged c:slimeballs). */
    public static final Reg<Item> ROYAL_JELLY = Reg.of(rl("royal_jelly"));

    /// Combs, gems and spawn eggs: one of each per roster bee (BeeSpecs), ids <comb>_honeycomb,
    /// <bee>_conversion_gem, <bee>_bee_spawn_egg
    private static final Map<String, Reg<Item>> COMBS = new LinkedHashMap<>();
    private static final Map<String, Reg<Item>> GEMS = new LinkedHashMap<>();
    private static final Map<String, Reg<Item>> EGGS = new LinkedHashMap<>();

    static {
        for (BeeSpec spec : BeeSpecs.ALL) {
            for (String comb : spec.modCombIds()) {
                COMBS.computeIfAbsent(comb, c -> Reg.of(rl(c + "_honeycomb")));
            }
        }
        for (BeeSpec spec : BeeSpecs.ALL) {
            if (spec.hasGem()) {
                GEMS.put(spec.id(), Reg.of(rl(spec.id() + "_conversion_gem")));
            }
        }
        for (BeeSpec spec : BeeSpecs.ALL) {
            EGGS.put(spec.id(), Reg.of(rl(spec.entityId() + "_spawn_egg")));
        }
    }

    /// Guide
    /** The Encyclobeedia: the mod's own guide book (client/guide), crafted from a book and a honeycomb. */
    public static final Reg<Item> ENCYCLOBEEDIA = Reg.of(rl("encyclobeedia"));

    /// Jars
    public static final Reg<Item> BEE_JAR = Reg.of(rl("bee_jar"));

    private static Item register(String name, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, rl(name), item);
    }

    /** Registers a block's item under the same id (called by ModBlocks). */
    static Reg<Item> registerBlockItem(String name, Supplier<Item> factory) {
        Reg<Item> reg = Reg.of(rl(name));
        reg.set(register(name, factory.get()));
        return reg;
    }

    public static Item comb(String id) {
        Reg<Item> comb = COMBS.get(id);
        if (comb == null) throw new IllegalArgumentException("No comb " + id);
        return comb.get();
    }

    public static Item gem(String beeId) {
        return GEMS.get(beeId).get();
    }

    public static Item spawnEgg(String beeId) {
        return EGGS.get(beeId).get();
    }

    /** Every mod comb, in roster order (each bee's own combs, then its rare ones). */
    public static java.util.List<Item> allCombs() {
        return COMBS.values().stream().map(Reg::get).toList();
    }

    /** Every spawn egg, in roster order. */
    public static java.util.List<Item> allSpawnEggs() {
        return EGGS.values().stream().map(Reg::get).toList();
    }

    /** Every conversion gem, for the vanilla bee's tempt goal. */
    public static Item[] allGems() {
        return GEMS.values().stream().map(Reg::get).toArray(Item[]::new);
    }

    /** The bee a conversion gem turns a vanilla bee into, or null if the item is not a gem. */
    public static @Nullable BeeSpec specForGem(ItemStack stack) {
        for (Map.Entry<String, Reg<Item>> gem : GEMS.entrySet()) {
            if (stack.is(gem.getValue().get())) return BeeSpecs.byId(gem.getKey());
        }
        return null;
    }

    public static void register() {
        COPPER_SMOKER.set(register("copper_smoker", new SmokerItem(new Item.Properties(), SmokerItem.COPPER_SMOKE_TICKS)));
        SMOKER.set(register("smoker", new SmokerItem(new Item.Properties(), SmokerItem.IRON_SMOKE_TICKS)));
        BubbaStarItem bubbaStar = new BubbaStarItem(new Item.Properties());
        BUBBA_STAR.set(register("bubba_star", bubbaStar));
        LesserBubbaStarItem lesserStar = new LesserBubbaStarItem(new Item.Properties());
        LESSER_BUBBA_STAR.set(register("lesser_bubba_star", lesserStar));
        CelestialBubbaStarItem celestialStar = new CelestialBubbaStarItem(new Item.Properties());
        CELESTIAL_BUBBA_STAR.set(register("celestial_bubba_star", celestialStar));
        // The three stars are reusable catalysts: vanilla exposes only a fixed remainder item, so point it at the star itself.
        ((ItemCraftingRemainderAccessor) bubbaStar).bubbasBees$setCraftingRemainder(bubbaStar);
        ((ItemCraftingRemainderAccessor) lesserStar).bubbasBees$setCraftingRemainder(lesserStar);
        ((ItemCraftingRemainderAccessor) celestialStar).bubbasBees$setCraftingRemainder(celestialStar);
        STARDUST.set(register("stardust", new ItemNameBlockItem(ModBlocks.STARDUST.get(), new Item.Properties())));
        COMB_CHUTE.set(register("comb_chute", new net.minecraft.world.item.BlockItem(ModBlocks.COMB_CHUTE.get(),
                new Item.Properties())));
        ROYAL_JELLY.set(register("royal_jelly", new Item(new Item.Properties()
                .food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.375F).build()))));

        COMBS.forEach((id, reg) -> reg.set(register(reg.id().getPath(), new Item(new Item.Properties()))));
        GEMS.forEach((id, reg) -> reg.set(register(reg.id().getPath(), new Item(new Item.Properties()))));
        for (BeeSpec spec : BeeSpecs.ALL) {
            Reg<Item> egg = EGGS.get(spec.id());
            egg.set(register(egg.id().getPath(), new SpawnEggItem(ModEntityTypes.bee(spec.id()).get(),
                    spec.eggBase(), spec.eggSpots(), new Item.Properties())));
        }

        ENCYCLOBEEDIA.set(register("encyclobeedia", new EncyclobeediaItem(new Item.Properties().stacksTo(1))));
        BEE_JAR.set(register("bee_jar", new BeeJarItem(new Item.Properties().stacksTo(16))));
    }
}
