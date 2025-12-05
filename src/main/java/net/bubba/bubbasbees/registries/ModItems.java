package net.bubba.bubbasbees.registries;

import net.bubba.bubbasbees.BubbasBees;
import net.bubba.bubbasbees.item.ModCreativeModeTab;
import net.bubba.bubbasbees.item.custom.CopperHoneycombItem;
import net.bubba.bubbasbees.item.custom.CopperStarItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, BubbasBees.MOD_ID);


    public static final RegistryObject<Item> COPPER_HONEYCOMB = ITEMS.register("copper_honeycomb",
            () -> new CopperHoneycombItem(new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB)));
    public static final RegistryObject<Item> COPPER_STAR = ITEMS.register("copper_star",
            () -> new CopperStarItem(new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB)));

    ///Eggs
    public static final RegistryObject<Item> COPPER_BEE_SPAWN_EGG = ITEMS.register("copper_bee_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.COPPER_BEE, 0x7d2c0f, 0xe36336,
                    new Item.Properties().tab(ModCreativeModeTab.BUBBASBEES_TAB)));


    public static void register(IEventBus eventBus){
        ITEMS.register(eventBus);
    }
}
