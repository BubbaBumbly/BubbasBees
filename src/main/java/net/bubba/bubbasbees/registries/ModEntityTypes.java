package net.bubba.bubbasbees.registries;

import net.bubba.bubbasbees.BubbasBees;
import net.bubba.bubbasbees.entity.custom.*;
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
                            .sized(0.7f, 0.6f)
                            .build(new ResourceLocation(BubbasBees.MOD_ID, "bubbas_bee").toString()));

    /// SubClassers of Bubba's Bee:

    public static final RegistryObject<EntityType<CopperBee>> COPPER_BEE =
            ENTITY_TYPES.register("copper_bee",
                    () -> EntityType.Builder.of(CopperBee::new, MobCategory.CREATURE)
                            .sized(0.7f, 0.6f)
                            .build(new ResourceLocation(BubbasBees.MOD_ID, "copper_bee").toString()));
    public static final RegistryObject<EntityType<SandyBee>> SANDY_BEE =
            ENTITY_TYPES.register("sandy_bee",
                    () -> EntityType.Builder.of(SandyBee::new, MobCategory.CREATURE)
                            .sized(0.7f, 0.6f)
                            .build(new ResourceLocation(BubbasBees.MOD_ID, "sandy_bee").toString()));
    public static final RegistryObject<EntityType<RedstoneBee>> REDSTONE_BEE =
            ENTITY_TYPES.register("redstone_bee",
                    () -> EntityType.Builder.of(RedstoneBee::new, MobCategory.CREATURE)
                            .sized(0.7f, 0.6f)
                            .build(new ResourceLocation(BubbasBees.MOD_ID, "redstone_bee").toString()));
    public static final RegistryObject<EntityType<GoldBee>> GOLD_BEE =
            ENTITY_TYPES.register("gold_bee",
                    () -> EntityType.Builder.of(GoldBee::new, MobCategory.CREATURE)
                            .sized(0.7f, 0.6f)
                            .build(new ResourceLocation(BubbasBees.MOD_ID, "gold_bee").toString()));
    public static final RegistryObject<EntityType<IronBee>> IRON_BEE =
            ENTITY_TYPES.register("iron_bee",
                    () -> EntityType.Builder.of(IronBee::new, MobCategory.CREATURE)
                            .sized(0.7f, 0.6f)
                            .build(new ResourceLocation(BubbasBees.MOD_ID, "iron_bee").toString()));
    public static final RegistryObject<EntityType<NatureBee>> NATURE_BEE =
            ENTITY_TYPES.register("nature_bee",
                    () -> EntityType.Builder.of(NatureBee::new, MobCategory.CREATURE)
                            .sized(0.7f, 0.6f)
                            .build(new ResourceLocation(BubbasBees.MOD_ID, "nature_bee").toString()));
    public static final RegistryObject<EntityType<EnderBee>> ENDER_BEE =
            ENTITY_TYPES.register("ender_bee",
                    () -> EntityType.Builder.of(EnderBee::new, MobCategory.CREATURE)
                            .sized(0.7f, 0.6f)
                            .build(new ResourceLocation(BubbasBees.MOD_ID, "ender_bee").toString()));
    public static final RegistryObject<EntityType<StoneBee>> STONE_BEE =
            ENTITY_TYPES.register("stone_bee",
                    () -> EntityType.Builder.of(StoneBee::new, MobCategory.CREATURE)
                            .sized(0.7f, 0.6f)
                            .build(new ResourceLocation(BubbasBees.MOD_ID, "stone_bee").toString()));

    ///

    /// End SubClassers of Bubba's Bee.

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}