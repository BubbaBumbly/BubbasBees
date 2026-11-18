package net.bubba.bubbasbees.registries;

import net.bubba.bubbasbees.BubbasBees;
import net.bubba.bubbasbees.bees.BeeSpec;
import net.bubba.bubbasbees.bees.BeeSpecs;
import net.bubba.bubbasbees.entity.custom.BubbasBee;
import net.bubba.bubbasbees.entity.custom.ModBee;
import net.bubba.bubbasbees.entity.custom.DarkBee;
import net.bubba.bubbasbees.util.Reg;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ModEntityTypes {
    private static ResourceLocation rl(String name) {
        return ResourceLocation.fromNamespaceAndPath(BubbasBees.MOD_ID, name);
    }

    /** Dev-only base bee (diamond look), not obtainable. */
    public static final Reg<EntityType<BubbasBee>> BUBBAS_BEE = Reg.of(rl("bubbas_bee"));

    /** Night-ritual pest: snuffs candles (not a roster bee: no gem, comb, hive or spawn egg). */
    public static final Reg<EntityType<DarkBee>> DARK_BEE = Reg.of(rl("dark_bee"));

    /** Every roster bee by spec id ("iron" -> bubbasbees:iron_bee). */
    private static final Map<String, Reg<EntityType<ModBee>>> BEES = new LinkedHashMap<>();

    static {
        for (BeeSpec spec : BeeSpecs.ALL) {
            BEES.put(spec.id(), Reg.of(rl(spec.entityId())));
        }
    }

    public static Reg<EntityType<ModBee>> bee(String id) {
        Reg<EntityType<ModBee>> bee = BEES.get(id);
        if (bee == null) throw new IllegalArgumentException("No bee " + id);
        return bee;
    }

    public static Map<String, Reg<EntityType<ModBee>>> bees() {
        return Collections.unmodifiableMap(BEES);
    }

    public static void register() {
        BUBBAS_BEE.set(Registry.register(BuiltInRegistries.ENTITY_TYPE, BUBBAS_BEE.id(),
                EntityType.Builder.of(BubbasBee::new, MobCategory.CREATURE)
                        .sized(0.7f, 0.6f)
                        .build(ResourceLocation.fromNamespaceAndPath(BubbasBees.MOD_ID, "bubbas_bee").toString())));

        DARK_BEE.set(Registry.register(BuiltInRegistries.ENTITY_TYPE, DARK_BEE.id(),
                EntityType.Builder.of(DarkBee::new, MobCategory.MISC)
                        .sized(0.7f, 0.6f).fireImmune()
                        .build(ResourceLocation.fromNamespaceAndPath(BubbasBees.MOD_ID, "dark_bee").toString())));

        for (BeeSpec spec : BeeSpecs.ALL) {
            Reg<EntityType<ModBee>> reg = BEES.get(spec.id());
            EntityType.Builder<ModBee> builder = EntityType.Builder
                    .<ModBee>of((type, level) -> new ModBee(type, level, spec), MobCategory.CREATURE)
                    .sized(0.7f * spec.scale(), 0.6f * spec.scale());
            if (spec.fireImmune()) builder.fireImmune();
            reg.set(Registry.register(BuiltInRegistries.ENTITY_TYPE, reg.id(), builder.build(spec.entityKey().toString())));
        }
    }
}
