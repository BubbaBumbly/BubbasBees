package net.bubba.bubbasbees.registries;

import net.bubba.bubbasbees.BubbasBees;
import net.bubba.bubbasbees.item.ModCreativeModeTab;
import net.bubba.bubbasbees.item.custom.*;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, BubbasBees.MOD_ID);

    /// General
    public static final RegistryObject<Item> SMOKER = ITEMS.register("smoker",
            () -> new SmokerItem(new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB)));
    public static final RegistryObject<Item> BUBBA_STAR = ITEMS.register("bubba_star",
            () -> new BubbaStarItem(new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB)));
    public static final RegistryObject<Item> LESSER_BUBBA_STAR = ITEMS.register("lesser_bubba_star",
            () -> new LesserBubbaStarItem(new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB)));
    /// Combs
    public static final RegistryObject<Item> COPPER_HONEYCOMB = ITEMS.register("copper_honeycomb",
            () -> new CopperHoneycombItem(new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB)));
    public static final RegistryObject<Item> SANDY_HONEYCOMB = ITEMS.register("sandy_honeycomb",
            () -> new SandyHoneycombItem(new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB)));
    public static final RegistryObject<Item> REDSTONE_HONEYCOMB = ITEMS.register("redstone_honeycomb",
            () -> new RedstoneHoneycombItem(new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB)));
    public static final RegistryObject<Item> GOLD_HONEYCOMB = ITEMS.register("gold_honeycomb",
            () -> new GoldHoneycombItem(new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB)));
    public static final RegistryObject<Item> IRON_HONEYCOMB = ITEMS.register("iron_honeycomb",
            () -> new IronHoneycombItem(new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB)));
    public static final RegistryObject<Item> NATURE_HONEYCOMB = ITEMS.register("nature_honeycomb",
            () -> new NatureHoneycombItem(new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB)));
    public static final RegistryObject<Item> ENDER_HONEYCOMB = ITEMS.register("ender_honeycomb",
            () -> new EnderHoneycombItem(new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB)));
    public static final RegistryObject<Item> STONE_HONEYCOMB = ITEMS.register("stone_honeycomb",
            () -> new StoneHoneycombItem(new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB)));

    /// Eggs
    public static final RegistryObject<Item> COPPER_BEE_SPAWN_EGG = ITEMS.register("copper_bee_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.COPPER_BEE, 0x7d2c0f, 0xe36336,
                    new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB)));
    public static final RegistryObject<Item> SANDY_BEE_SPAWN_EGG = ITEMS.register("sandy_bee_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.SANDY_BEE, 0xd4be8c, 0xb5aa91,
                    new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB)));
    public static final RegistryObject<Item> REDSTONE_BEE_SPAWN_EGG = ITEMS.register("redstone_bee_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.REDSTONE_BEE, 0x470e0a, 0x7a1710,
                    new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB)));
    public static final RegistryObject<Item> GOLD_BEE_SPAWN_EGG = ITEMS.register("gold_bee_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.GOLD_BEE, 0xeb990c, 0xc97c00,
                    new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB)));
    public static final RegistryObject<Item> IRON_BEE_SPAWN_EGG = ITEMS.register("iron_bee_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.IRON_BEE, 0xf0e7da, 0xbfaa88,
                    new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB)));
    public static final RegistryObject<Item> NATURE_BEE_SPAWN_EGG = ITEMS.register("nature_bee_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.NATURE_BEE, 0x074a10, 0x0e871e,
                    new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB)));
    public static final RegistryObject<Item> ENDER_BEE_SPAWN_EGG = ITEMS.register("ender_bee_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.ENDER_BEE, 0xd36be3, 0x682a73,
                    new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB)));
    public static final RegistryObject<Item> STONE_BEE_SPAWN_EGG = ITEMS.register("stone_bee_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.STONE_BEE, 0x4e4a4f, 0x8b858c,
                    new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB)));

    /// Jars
    public static final RegistryObject<Item> BEE_JAR = ITEMS.register("bee_jar",
            () -> new BeeJarItem(new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB).stacksTo(16)));

    /// Conversion Items
    public static final RegistryObject<Item> COPPER_CONVERSION_GEM = ITEMS.register("copper_conversion_gem",
            () -> new CopperConversionGemItem(new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB)));
    public static final RegistryObject<Item> SANDY_CONVERSION_GEM = ITEMS.register("sandy_conversion_gem",
            () -> new SandyConversionGemItem(new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB)));
    public static final RegistryObject<Item> REDSTONE_CONVERSION_GEM = ITEMS.register("redstone_conversion_gem",
            () -> new RedstoneConversionGemItem(new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB)));
    public static final RegistryObject<Item> GOLD_CONVERSION_GEM = ITEMS.register("gold_conversion_gem",
            () -> new GoldConversionGemItem(new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB)));
    public static final RegistryObject<Item> IRON_CONVERSION_GEM = ITEMS.register("iron_conversion_gem",
            () -> new IronConversionGemItem(new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB)));
    public static final RegistryObject<Item> NATURE_CONVERSION_GEM = ITEMS.register("nature_conversion_gem",
            () -> new NatureConversionGemItem(new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB)));
    public static final RegistryObject<Item> ENDER_CONVERSION_GEM = ITEMS.register("ender_conversion_gem",
            () -> new EnderConversionGemItem(new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB)));
    public static final RegistryObject<Item> STONE_CONVERSION_GEM = ITEMS.register("stone_conversion_gem",
            () -> new StoneConversionGemItem(new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB)));

    public static void register(IEventBus eventBus){
        ITEMS.register(eventBus);
    }
}
