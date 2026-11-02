package net.bubba.bubbasbees.registries;

import net.bubba.bubbasbees.BubbasBees;
import net.bubba.bubbasbees.bees.BeeSpec;
import net.bubba.bubbasbees.bees.BeeSpecs;
import net.bubba.bubbasbees.entity.custom.BubbasBee;
import net.bubba.bubbasbees.entity.custom.DarkBee;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.MissingMappingsEvent;

public class ModEvents {
    @Mod.EventBusSubscriber(modid = BubbasBees.MOD_ID)
    public static class ForgeEvents {
        /** Ids renamed in Phase 2: the sandy bee became the desert bee. Old worlds keep their bees and items. */
        @SubscribeEvent
        public static void remapRenamedIds(MissingMappingsEvent event) {
            for (MissingMappingsEvent.Mapping<EntityType<?>> mapping
                    : event.getMappings(ForgeRegistries.Keys.ENTITY_TYPES, BubbasBees.MOD_ID)) {
                if (mapping.getKey().getPath().equals("sandy_bee")) mapping.remap(ModEntityTypes.bee("desert").get());
            }
            for (MissingMappingsEvent.Mapping<Item> mapping
                    : event.getMappings(ForgeRegistries.Keys.ITEMS, BubbasBees.MOD_ID)) {
                switch (mapping.getKey().getPath()) {
                    case "sandy_honeycomb" -> mapping.remap(ModItems.comb("desert"));
                    case "sandy_conversion_gem" -> mapping.remap(ModItems.gem("desert"));
                    case "sandy_bee_spawn_egg" -> mapping.remap(ModItems.spawnEgg("desert"));
                    default -> { }
                }
            }
        }

        @Mod.EventBusSubscriber(modid = BubbasBees.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
        public static class ModEventBusEvents {
            @SubscribeEvent
            public static void entityAttributeEvent(EntityAttributeCreationEvent event) {
                event.put(ModEntityTypes.BUBBAS_BEE.get(), BubbasBee.setAttributes());
                for (BeeSpec spec : BeeSpecs.ALL) {
                    event.put(ModEntityTypes.bee(spec.id()).get(), BubbasBee.setAttributes(spec.maxHealth()));
                }
                event.put(ModEntityTypes.DARK_BEE.get(), DarkBee.setAttributes());
            }
        }
    }
}