package net.bubba.bubbasbees.registries;

import net.bubba.bubbasbees.BubbasBees;
import net.bubba.bubbasbees.entity.custom.CopperBee;
import net.bubba.bubbasbees.entity.custom.BubbasBee;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, BubbasBees.MOD_ID);

    public static final RegistryObject<EntityType<BubbasBee>> BUBBAS_BEE =
            ENTITY_TYPES.register("bubbas_bee",
                    () -> EntityType.Builder.of(BubbasBee::new, MobCategory.CREATURE)
                            .sized(0.5f, 0.5f)
                            .build(new ResourceLocation(BubbasBees.MOD_ID, "bubbas_bee").toString()));

    /// SubClassers of Bubba's Bee:

    public static final RegistryObject<EntityType<CopperBee>> COPPER_BEE =
            ENTITY_TYPES.register("copper_bee",
                    () -> EntityType.Builder.of(CopperBee::new, MobCategory.CREATURE)
                            .sized(0.5f, 0.5f)
                            .build(new ResourceLocation(BubbasBees.MOD_ID, "copper_bee").toString()));
    ///

    /// End SubClassers of Bubba's Bee.

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}