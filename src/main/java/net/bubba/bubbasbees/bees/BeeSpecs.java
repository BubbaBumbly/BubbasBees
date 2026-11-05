package net.bubba.bubbasbees.bees;

import net.bubba.bubbasbees.BubbasBees;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static net.bubba.bubbasbees.bees.BeeSpec.Tier.*;
import static net.bubba.bubbasbees.bees.BeeSpec.Trait.*;

/**
 * The bee roster. Order = creative tab order. Numbers: finish-spec.md "Balance numbers" (busy mode).
 * 41 bees in 1.19.2 (cherry grove joins from 1.20); traits per the bee cards.
 */
public final class BeeSpecs {
    private BeeSpecs() {}

    private static final ResourceKey<Level> NETHER = Level.NETHER;
    private static final ResourceKey<Level> END = Level.END;

    // Spawn egg colours are picked from each bee's texture (tools/gen_bee_data.py prints them).
    public static final List<BeeSpec> ALL = List.of(
            // Lesser star (cherry grove is 1.20+ only)
            BeeSpec.of("stone", LESSER).deepslateLook().egg(0x4e4a4f, 0x8b858c).build(),
            BeeSpec.of("dirt", LESSER).model("dirt").egg(0x8a6343, 0x302218).build(),
            BeeSpec.of("nature", LESSER).model("nature").traits(GREEN_THUMB).egg(0x074a10, 0x0e871e).build(),
            BeeSpec.of("forest", LESSER).model("forest").egg(0x485725, 0x43371e).build(),
            BeeSpec.of("desert", LESSER).model("desert").rare("bone", 20).traits(BURROWER).egg(0xd4be8c, 0xb5aa91).build(),
            BeeSpec.of("river", LESSER).model("river").comb("river", 1).comb("gravel", 1).traits(WATER_SAFE).egg(0xa1a7b1, 0xccd5d3).build(),
            BeeSpec.of("meadow", LESSER).model("meadow").egg(0xedd28c, 0x505330).build(),
            BeeSpec.of("jungle", LESSER).model("jungle").egg(0x679130, 0x322712).build(),
            BeeSpec.of("taiga", LESSER).model("taiga").egg(0x7e521e, 0x49663b).build(),
            BeeSpec.of("savanna", LESSER).model("savanna").traits(HERDER).egg(0xaba64a, 0x6d3d23).build(),
            // Bubba Star
            BeeSpec.of("amethyst", BUBBA).model("amethyst").rare("budding_amethyst", 150).egg(0x423b5a, 0xdcd2e3).build(),
            BeeSpec.of("obsidian", BUBBA).model("obsidian").traits(BLAST_PROOF).egg(0x231437, 0x64388d).build(),
            BeeSpec.of("glow", BUBBA).model("glow").traits(GLOW).egg(0x826131, 0xf0e0b0).build(),
            BeeSpec.of("mesa", BUBBA).model("mesa").traits(BURROWER).egg(0xb66d2e, 0x452f22).build(),
            BeeSpec.of("swamp", BUBBA).model("swamp").egg(0x3e6838, 0x241f18).build(),
            BeeSpec.of("frost", BUBBA).model("frost").traits(SNOWPROOF).egg(0xeaf2f5, 0xf6fbfe).build(),
            BeeSpec.of("lush_caves", BUBBA).model("lush_caves").egg(0x5c552f, 0x6d7737).build(),
            BeeSpec.of("deep_dark", BUBBA).model("deep_dark").rare("echo", 60).traits(SILENT_WINGS).egg(0x45494d, 0x1b363e).build(),
            BeeSpec.of("mushroom_fields", BUBBA).model("mushroom_fields").egg(0x6b6167, 0x8a3534).build(),
            BeeSpec.of("sea", BUBBA).model("sea").rare("nautilus", 20).traits(WATER_SAFE).egg(0x3b65bb, 0xded8c1).build(),
            BeeSpec.of("coral", BUBBA).model("coral").traits(WATER_SAFE).egg(0x454890, 0xc4b68c).build(),
            BeeSpec.of("prismarine", BUBBA).model("prismarine").traits(WATER_SAFE).egg(0xc0dcd6, 0x6da99d).build(),
            BeeSpec.of("corrupt_stone", BUBBA).model("corrupt_stone").home(NETHER).fireImmune().egg(0x6d3132, 0x422021).build(),
            BeeSpec.of("soul", BUBBA).model("soul").home(NETHER).fireImmune().translucent().rare("sad", 20)
                    .egg(0xeef1f2, 0xb0bdc5).build(),
            BeeSpec.of("corrupt_nature", BUBBA).model("corrupt_nature").home(NETHER).fireImmune().egg(0x35786c, 0xa01d1d).build(),
            BeeSpec.of("magma", BUBBA).model("magma").home(NETHER).fireImmune().traits(LAVA_FILLER).egg(0x3d1614, 0xc46724).build(),
            BeeSpec.of("ender", BUBBA).home(END).traits(SHORT_TELEPORT, NO_WATER_DAMAGE).egg(0xd36be3, 0x682a73).build(),
            BeeSpec.of("royal_guard", BUBBA).model("royal_guard").comb(BeeSpec.VANILLA_HONEYCOMB, 1).traits(ROYAL_GUARD).egg(0xdadcdd, 0x352f27).build(),
            BeeSpec.of("starlight", BUBBA).model("starlight").traits(NOCTURNAL).egg(0x352c7c, 0x383888).build(),
            // Celestial Bubba Star: ore bees are 100% except diamond (1 in 4) and ancient debris (1 in 6)
            BeeSpec.of("coal", CELESTIAL).model("coal").deepslateLook().egg(0x7e7e7d, 0x686869).build(),
            BeeSpec.of("copper", CELESTIAL).deepslateLook().egg(0x7d2c0f, 0xe36336).build(),
            BeeSpec.of("iron", CELESTIAL).deepslateLook().egg(0xf0e7da, 0xbfaa88).build(),
            BeeSpec.of("gold", CELESTIAL).deepslateLook().egg(0xeb990c, 0xc97c00).build(),
            BeeSpec.of("redstone", CELESTIAL).deepslateLook().egg(0x470e0a, 0x7a1710).build(),
            BeeSpec.of("lapis", CELESTIAL).model("lapis").deepslateLook().egg(0x7e7e7d, 0x666970).build(),
            BeeSpec.of("diamond", CELESTIAL).deepslateLook().combChance(1.0F / 4).egg(0x4ad0d0, 0x7a7a7a).build(),
            BeeSpec.of("emerald", CELESTIAL).model("emerald").deepslateLook().egg(0x727772, 0x1bc855).build(),
            BeeSpec.of("quartz", CELESTIAL).model("quartz").home(NETHER).fireImmune().egg(0xdcd9d2, 0x783e3a).build(),
            BeeSpec.of("ancient_debris", CELESTIAL).model("ancient_debris").home(NETHER).fireImmune()
                    .combChance(1.0F / 6).egg(0x624844, 0x252022).build(),
            // Royal queen: no gem (summoned by her ritual), one per world
            BeeSpec.of("royal_queen", NONE).model("royal_queen").traits(ROYAL_QUEEN).health(20).scale(1.25F).egg(0xf5ae2b, 0x541b71).build()
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
