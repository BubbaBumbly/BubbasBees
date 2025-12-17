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
    /// Combs
    public static final RegistryObject<Item> COPPER_HONEYCOMB = ITEMS.register("copper_honeycomb",
            () -> new CopperHoneycombItem(new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB)));
    public static final RegistryObject<Item> SANDY_HONEYCOMB = ITEMS.register("sandy_honeycomb",
            () -> new SandyHoneycombItem(new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB)));
    /// Eggs
    public static final RegistryObject<Item> COPPER_BEE_SPAWN_EGG = ITEMS.register("copper_bee_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.COPPER_BEE, 0x7d2c0f, 0xe36336,
                    new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB)));
    public static final RegistryObject<Item> SANDY_BEE_SPAWN_EGG = ITEMS.register("sandy_bee_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.SANDY_BEE, 0xd4be8c, 0xb5aa91,
                    new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB)));
    /// Conversion Items
    public static final RegistryObject<Item> BUBBA_STAR = ITEMS.register("bubba_star",
            () -> new BubbaStarItem(new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB)));
    public static final RegistryObject<Item> COPPER_CONVERSION_GEM = ITEMS.register("copper_conversion_gem",
            () -> new CopperConversionGemItem(new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB)));
    public static final RegistryObject<Item> SANDY_CONVERSION_GEM = ITEMS.register("sandy_conversion_gem",
            () -> new SandyConversionGemItem(new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB)));


    public static void register(IEventBus eventBus){
        ITEMS.register(eventBus);
    }
}
