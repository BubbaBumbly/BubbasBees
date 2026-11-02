package net.bubba.bubbasbees.registries;

import net.bubba.bubbasbees.BubbasBees;
import net.bubba.bubbasbees.bees.BeeSpec;
import net.bubba.bubbasbees.bees.BeeSpecs;
import net.bubba.bubbasbees.item.ModCreativeModeTab;
import net.bubba.bubbasbees.item.custom.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.food.FoodProperties;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, BubbasBees.MOD_ID);

    /// General
    public static final RegistryObject<Item> COPPER_SMOKER = ITEMS.register("copper_smoker",
            () -> new SmokerItem(new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB), SmokerItem.COPPER_SMOKE_TICKS));
    /** The iron smoker keeps the original id, so smokers already in worlds stay. */
    public static final RegistryObject<Item> SMOKER = ITEMS.register("smoker",
            () -> new SmokerItem(new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB), SmokerItem.IRON_SMOKE_TICKS));
    public static final RegistryObject<Item> BUBBA_STAR = ITEMS.register("bubba_star",
            () -> new BubbaStarItem(new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB)));
    public static final RegistryObject<Item> LESSER_BUBBA_STAR = ITEMS.register("lesser_bubba_star",
            () -> new LesserBubbaStarItem(new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB)));
    /** Top-tier star, made by the night ritual; reusable catalyst for the ore-bee gems. */
    public static final RegistryObject<Item> CELESTIAL_BUBBA_STAR = ITEMS.register("celestial_bubba_star",
            () -> new CelestialBubbaStarItem(new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB)));

    /** The starlight bee's product; placed like redstone dust (the ritual circle). */
    public static final RegistryObject<Item> STARDUST = ITEMS.register("stardust",
            () -> new ItemNameBlockItem(ModBlocks.STARDUST.get(), new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB)));
    /** Comb chute block (placed under a hive, see CombChuteBlock); flat icon in the inventory. */
    public static final RegistryObject<Item> COMB_CHUTE = ITEMS.register("comb_chute",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.COMB_CHUTE.get(),
                    new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB)));
    /** The royal queen's product: 1 hunger bar, 1.5 saturation (tagged forge:slimeballs). */
    public static final RegistryObject<Item> ROYAL_JELLY = ITEMS.register("royal_jelly",
            () -> new Item(new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB)
                    .food(new FoodProperties.Builder().nutrition(2).saturationMod(0.375F).build())));

    /// Combs, gems and spawn eggs: one of each per roster bee (BeeSpecs), ids <comb>_honeycomb,
    /// <bee>_conversion_gem, <bee>_bee_spawn_egg
    private static final Map<String, RegistryObject<Item>> COMBS = new LinkedHashMap<>();
    private static final Map<String, RegistryObject<Item>> GEMS = new LinkedHashMap<>();
    private static final Map<String, RegistryObject<Item>> EGGS = new LinkedHashMap<>();

    static {
        for (BeeSpec spec : BeeSpecs.ALL) {
            for (String comb : spec.modCombIds()) {
                COMBS.computeIfAbsent(comb, c -> ITEMS.register(c + "_honeycomb",
                        () -> new Item(new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB))));
            }
        }
        for (BeeSpec spec : BeeSpecs.ALL) {
            if (spec.hasGem()) {
                GEMS.put(spec.id(), ITEMS.register(spec.id() + "_conversion_gem",
                        () -> new Item(new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB))));
            }
        }
        for (BeeSpec spec : BeeSpecs.ALL) {
            EGGS.put(spec.id(), ITEMS.register(spec.entityId() + "_spawn_egg",
                    () -> new ForgeSpawnEggItem(ModEntityTypes.bee(spec.id()), spec.eggBase(), spec.eggSpots(),
                            new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB))));
        }
    }

    public static Item comb(String id) {
        RegistryObject<Item> comb = COMBS.get(id);
        if (comb == null) throw new IllegalArgumentException("No comb " + id);
        return comb.get();
    }

    public static Item gem(String beeId) {
        return GEMS.get(beeId).get();
    }

    public static Item spawnEgg(String beeId) {
        return EGGS.get(beeId).get();
    }

    /** Every conversion gem, for the vanilla bee's tempt goal. */
    public static Item[] allGems() {
        return GEMS.values().stream().map(RegistryObject::get).toArray(Item[]::new);
    }

    /** The bee a conversion gem turns a vanilla bee into, or null if the item is not a gem. */
    public static @Nullable BeeSpec specForGem(ItemStack stack) {
        for (Map.Entry<String, RegistryObject<Item>> gem : GEMS.entrySet()) {
            if (stack.is(gem.getValue().get())) return BeeSpecs.byId(gem.getKey());
        }
        return null;
    }

    /// Jars
    public static final RegistryObject<Item> BEE_JAR = ITEMS.register("bee_jar",
            () -> new BeeJarItem(new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB).stacksTo(16)));

    public static void register(IEventBus eventBus){
        ITEMS.register(eventBus);
    }
}
