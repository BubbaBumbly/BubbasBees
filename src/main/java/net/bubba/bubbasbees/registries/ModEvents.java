package net.bubba.bubbasbees.registries;

import net.bubba.bubbasbees.BubbasBees;
import net.bubba.bubbasbees.entity.custom.BubbasBee;
import net.bubba.bubbasbees.entity.custom.CopperBee;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class ModEvents {
    @Mod.EventBusSubscriber(modid = BubbasBees.MOD_ID)
    public static class ForgeEvents {
        @Mod.EventBusSubscriber(modid = BubbasBees.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
        public static class ModEventBusEvents {
            @SubscribeEvent
            public static void entityAttributeEvent(EntityAttributeCreationEvent event) {
                event.put(ModEntityTypes.BUBBAS_BEE.get(), BubbasBee.setAttributes());
                event.put(ModEntityTypes.COPPER_BEE.get(), CopperBee.setAttributes());
                event.put(ModEntityTypes.SANDY_BEE.get(), CopperBee.setAttributes());
            }
        }
    }
}