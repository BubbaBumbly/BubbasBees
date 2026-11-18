package net.bubba.bubbasbees.registries;

import net.bubba.bubbasbees.BubbasBees;
import net.bubba.bubbasbees.bees.BeeSpec;
import net.bubba.bubbasbees.bees.BeeSpecs;
import net.bubba.bubbasbees.item.custom.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(BuiltInRegistries.ITEM, BubbasBees.MOD_ID);

    /// General
    public static final DeferredHolder<Item, Item> COPPER_SMOKER = ITEMS.register("copper_smoker",
            () -> new SmokerItem(new Item.Properties(), SmokerItem.COPPER_SMOKE_TICKS));
    /** The iron smoker keeps the original id, so smokers already in worlds stay. */
    public static final DeferredHolder<Item, Item> SMOKER = ITEMS.register("smoker",
            () -> new SmokerItem(new Item.Properties(), SmokerItem.IRON_SMOKE_TICKS));
    public static final DeferredHolder<Item, Item> BUBBA_STAR = ITEMS.register("bubba_star",
            () -> new BubbaStarItem(new Item.Properties()));
    public static final DeferredHolder<Item, Item> LESSER_BUBBA_STAR = ITEMS.register("lesser_bubba_star",
            () -> new LesserBubbaStarItem(new Item.Properties()));
    /** Top-tier star, made by the night ritual; reusable catalyst for the ore-bee gems. */
    public static final DeferredHolder<Item, Item> CELESTIAL_BUBBA_STAR = ITEMS.register("celestial_bubba_star",
            () -> new CelestialBubbaStarItem(new Item.Properties()));

    /** The starlight bee's product; placed like redstone dust (the ritual circle). */
    public static final DeferredHolder<Item, Item> STARDUST = ITEMS.register("stardust",
            () -> new ItemNameBlockItem(ModBlocks.STARDUST.get(), new Item.Properties()));
    /** Comb chute block (placed under a hive, see CombChuteBlock); flat icon in the inventory. */
    public static final DeferredHolder<Item, Item> COMB_CHUTE = ITEMS.register("comb_chute",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.COMB_CHUTE.get(),
                    new Item.Properties()));
    /** The royal queen's product: 1 hunger bar, 1.5 saturation (tagged c:slimeballs). */
    public static final DeferredHolder<Item, Item> ROYAL_JELLY = ITEMS.register("royal_jelly",
            () -> new Item(new Item.Properties()
                    .food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.375F).build())));

    /// Combs, gems and spawn eggs: one of each per roster bee (BeeSpecs), ids <comb>_honeycomb,
    /// <bee>_conversion_gem, <bee>_bee_spawn_egg
    private static final Map<String, DeferredHolder<Item, Item>> COMBS = new LinkedHashMap<>();
    private static final Map<String, DeferredHolder<Item, Item>> GEMS = new LinkedHashMap<>();
    private static final Map<String, DeferredHolder<Item, Item>> EGGS = new LinkedHashMap<>();

    static {
        for (BeeSpec spec : BeeSpecs.ALL) {
            for (String comb : spec.modCombIds()) {
                COMBS.computeIfAbsent(comb, c -> ITEMS.register(c + "_honeycomb",
                        () -> new Item(new Item.Properties())));
            }
        }
        for (BeeSpec spec : BeeSpecs.ALL) {
            if (spec.hasGem()) {
                GEMS.put(spec.id(), ITEMS.register(spec.id() + "_conversion_gem",
                        () -> new Item(new Item.Properties())));
            }
        }
        for (BeeSpec spec : BeeSpecs.ALL) {
            EGGS.put(spec.id(), ITEMS.register(spec.entityId() + "_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntityTypes.bee(spec.id()), spec.eggBase(), spec.eggSpots(),
                            new Item.Properties())));
        }
    }

    public static Item comb(String id) {
        DeferredHolder<Item, Item> comb = COMBS.get(id);
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
        return COMBS.values().stream().map(DeferredHolder::get).toList();
    }

    /** Every spawn egg, in roster order. */
    public static java.util.List<Item> allSpawnEggs() {
        return EGGS.values().stream().map(DeferredHolder::get).toList();
    }

    /** Every conversion gem, for the vanilla bee's tempt goal. */
    public static Item[] allGems() {
        return GEMS.values().stream().map(DeferredHolder::get).toArray(Item[]::new);
    }

    /** The bee a conversion gem turns a vanilla bee into, or null if the item is not a gem. */
    public static @Nullable BeeSpec specForGem(ItemStack stack) {
        for (Map.Entry<String, DeferredHolder<Item, Item>> gem : GEMS.entrySet()) {
            if (stack.is(gem.getValue().get())) return BeeSpecs.byId(gem.getKey());
        }
        return null;
    }

    /// Guide
    /** The Encyclobeedia: the mod's own guide book (client/guide), crafted from a book and a honeycomb. */
    public static final DeferredHolder<Item, Item> ENCYCLOBEEDIA = ITEMS.register("encyclobeedia",
            () -> new EncyclobeediaItem(new Item.Properties().stacksTo(1)));

    /// Jars
    public static final DeferredHolder<Item, Item> BEE_JAR = ITEMS.register("bee_jar",
            () -> new BeeJarItem(new Item.Properties().stacksTo(16)));

    static {
        // The sandy bee became the desert bee in 2.0.0; old worlds keep their items under the new ids.
        ITEMS.addAlias(ResourceLocation.fromNamespaceAndPath(BubbasBees.MOD_ID, "sandy_honeycomb"),
                ResourceLocation.fromNamespaceAndPath(BubbasBees.MOD_ID, "desert_honeycomb"));
        ITEMS.addAlias(ResourceLocation.fromNamespaceAndPath(BubbasBees.MOD_ID, "sandy_conversion_gem"),
                ResourceLocation.fromNamespaceAndPath(BubbasBees.MOD_ID, "desert_conversion_gem"));
        ITEMS.addAlias(ResourceLocation.fromNamespaceAndPath(BubbasBees.MOD_ID, "sandy_bee_spawn_egg"),
                ResourceLocation.fromNamespaceAndPath(BubbasBees.MOD_ID, "desert_bee_spawn_egg"));
    }

    public static void register(IEventBus eventBus){
        ITEMS.register(eventBus);
    }
}
