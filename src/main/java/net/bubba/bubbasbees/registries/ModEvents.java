package net.bubba.bubbasbees.registries;

import net.bubba.bubbasbees.BubbasBees;
import net.bubba.bubbasbees.bees.BeeSpec;
import net.bubba.bubbasbees.bees.BeeSpecs;
import net.bubba.bubbasbees.entity.custom.BubbasBee;
import net.bubba.bubbasbees.entity.custom.DarkBee;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

public class ModEvents {
    @EventBusSubscriber(modid = BubbasBees.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
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
