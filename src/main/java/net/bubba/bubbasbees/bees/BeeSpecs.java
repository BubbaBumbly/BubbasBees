package net.bubba.bubbasbees.bees;

import net.bubba.bubbasbees.BubbasBees;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static net.bubba.bubbasbees.bees.BeeSpec.Tier.*;

/**
 * The bee roster. Order = creative tab order. Numbers: finish-spec.md "Balance numbers" (busy mode).
 * Phase 2 step 1 holds the bees that already existed; the other 33 arrive in step 2.
 */
public final class BeeSpecs {
    private BeeSpecs() {}

    public static final List<BeeSpec> ALL = List.of(
            // Lesser star
            BeeSpec.of("stone", LESSER).deepslateLook().egg(0x4e4a4f, 0x8b858c).build(),
            BeeSpec.of("nature", LESSER).model("nature").egg(0x074a10, 0x0e871e).build(),
            BeeSpec.of("desert", LESSER).model("desert").egg(0xd4be8c, 0xb5aa91).build(),
            // Bubba Star
            BeeSpec.of("ender", BUBBA).egg(0xd36be3, 0x682a73).build(),
            // Celestial Bubba Star (ore bees: 100% except diamond and ancient debris)
            BeeSpec.of("copper", CELESTIAL).deepslateLook().egg(0x7d2c0f, 0xe36336).build(),
            BeeSpec.of("iron", CELESTIAL).deepslateLook().egg(0xf0e7da, 0xbfaa88).build(),
            BeeSpec.of("gold", CELESTIAL).deepslateLook().egg(0xeb990c, 0xc97c00).build(),
            BeeSpec.of("redstone", CELESTIAL).deepslateLook().egg(0x470e0a, 0x7a1710).build()
    );

    private static final Map<String, BeeSpec> BY_ID = new LinkedHashMap<>();
    private static final Map<ResourceLocation, BeeSpec> BY_ENTITY = new LinkedHashMap<>();

    static {
        for (BeeSpec spec : ALL) {
            if (BY_ID.put(spec.id(), spec) != null) throw new IllegalStateException("Duplicate bee " + spec.id());
            BY_ENTITY.put(spec.entityKey(), spec);
        }
    }

    public static @Nullable BeeSpec byId(String id) {
        return BY_ID.get(id);
    }

    /** The spec for a stored or live bee's entity id ("bubbasbees:iron_bee"), or null for other bees. */
    public static @Nullable BeeSpec byEntity(@Nullable ResourceLocation entityId) {
        if (entityId == null || !BubbasBees.MOD_ID.equals(entityId.getNamespace())) return null;
        return BY_ENTITY.get(entityId);
    }
}
