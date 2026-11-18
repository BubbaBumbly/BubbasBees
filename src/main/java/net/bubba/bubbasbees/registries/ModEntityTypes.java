package net.bubba.bubbasbees.registries;

import net.bubba.bubbasbees.BubbasBees;
import net.bubba.bubbasbees.bees.BeeSpec;
import net.bubba.bubbasbees.bees.BeeSpecs;
import net.bubba.bubbasbees.entity.custom.BubbasBee;
import net.bubba.bubbasbees.entity.custom.ModBee;
import net.bubba.bubbasbees.entity.custom.DarkBee;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ModEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, BubbasBees.MOD_ID);

    /** Dev-only base bee (diamond look), not obtainable. */
    public static final DeferredHolder<EntityType<?>, EntityType<BubbasBee>> BUBBAS_BEE =
            ENTITY_TYPES.register("bubbas_bee",
                    () -> EntityType.Builder.of(BubbasBee::new, MobCategory.CREATURE)
                            .sized(0.7f, 0.6f)
                            .build(ResourceLocation.fromNamespaceAndPath(BubbasBees.MOD_ID, "bubbas_bee").toString()));

    /** Night-ritual pest: snuffs candles (not a roster bee: no gem, comb, hive or spawn egg). */
    public static final DeferredHolder<EntityType<?>, EntityType<DarkBee>> DARK_BEE =
            ENTITY_TYPES.register("dark_bee",
                    () -> EntityType.Builder.of(DarkBee::new, MobCategory.MISC)
                            .sized(0.7f, 0.6f).fireImmune()
                            .build(ResourceLocation.fromNamespaceAndPath(BubbasBees.MOD_ID, "dark_bee").toString()));

    /** Every roster bee by spec id ("iron" -> bubbasbees:iron_bee). */
    private static final Map<String, DeferredHolder<EntityType<?>, EntityType<ModBee>>> BEES = new LinkedHashMap<>();

    static {
        for (BeeSpec spec : BeeSpecs.ALL) {
            BEES.put(spec.id(), ENTITY_TYPES.register(spec.entityId(), () -> {
                EntityType.Builder<ModBee> builder = EntityType.Builder
                        .<ModBee>of((type, level) -> new ModBee(type, level, spec), MobCategory.CREATURE)
                        .sized(0.7f * spec.scale(), 0.6f * spec.scale());
                if (spec.fireImmune()) builder.fireImmune();
                return builder.build(spec.entityKey().toString());
            }));
        }
        // The sandy bee became the desert bee in 2.0.0; old worlds keep their bees under the new id.
        ENTITY_TYPES.addAlias(ResourceLocation.fromNamespaceAndPath(BubbasBees.MOD_ID, "sandy_bee"),
                ResourceLocation.fromNamespaceAndPath(BubbasBees.MOD_ID, "desert_bee"));
    }

    public static DeferredHolder<EntityType<?>, EntityType<ModBee>> bee(String id) {
        DeferredHolder<EntityType<?>, EntityType<ModBee>> bee = BEES.get(id);
        if (bee == null) throw new IllegalArgumentException("No bee " + id);
        return bee;
    }

    public static Map<String, DeferredHolder<EntityType<?>, EntityType<ModBee>>> bees() {
        return Collections.unmodifiableMap(BEES);
    }

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
