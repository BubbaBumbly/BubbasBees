package net.bubba.bubbasbees.bees;

import net.bubba.bubbasbees.BubbasBees;
import net.bubba.bubbasbees.registries.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Set;
import java.util.List;

/**
 * Everything that makes one mod bee different from another, as data. The entity, comb, gem and spawn egg of every
 * bee are registered from these (see BeeSpecs), so a new bee is one entry there plus its art and data files.
 *
 * Comb roll on every pollination trip (finish-spec.md "Balance numbers"): each rare comb rolls first (1 in N), then
 * the bee's own comb at combChance, picked by weight when the bee has several; a failed roll gives nothing.
 */
public record BeeSpec(String id, Tier tier, float combChance, List<WeightedComb> combs, List<RareComb> rareCombs,
                      @Nullable ResourceKey<Level> home, boolean fireImmune, boolean deepslateLook, boolean translucent,
                      String model, Set<Trait> traits, double maxHealth,
                      int eggBase, int eggSpots) {

    /** Optional abilities (finish-spec.md bee cards). Fire immunity is its own flag (entity type property). */
    public enum Trait {
        /** Works in rain and takes no damage from water. */
        WATER_SAFE,
        /** Takes no damage from water (but still stops for rain). */
        NO_WATER_DAMAGE,
        /** Works while it snows (rain in a snowy biome) and can't freeze in powder snow. */
        SNOWPROOF,
        /** Works only at night, and only from a hive and at flowers with open sky above. */
        NOCTURNAL,
        BLAST_PROOF,
        /** Its flapping makes no vibrations. */
        SILENT_WINGS,
        /** Dives into nearby sand or gravel when hurt and pops out later. */
        BURROWER,
        /** Teleports a short way when hurt. */
        SHORT_TELEPORT,
        /** Animals near its hive grow up and lose breeding cooldown 25% faster. */
        HERDER,
        /** Grows crops it flies over (vanilla bee behaviour; every other mod bee has it removed). */
        GREEN_THUMB,
        /** Glows and lights dark spots it flies through. */
        GLOW,
        /** 25% chance per cauldron fly-over to add a quarter of lava. */
        LAVA_FILLER,
        ROYAL_GUARD,
        ROYAL_QUEEN
    }

    public boolean has(Trait trait) {
        return traits.contains(trait);
    }

    /** Home biomes: data/bubbasbees/tags/worldgen/biome/home_biomes/<id>.json (missing file = none). */
    public TagKey<Biome> homeBiomeTag() {
        return TagKey.create(Registry.BIOME_REGISTRY, new ResourceLocation(BubbasBees.MOD_ID, "home_biomes/" + id));
    }

    public boolean isHomeBiome(Level level, BlockPos pos) {
        return level.getBiome(pos).is(homeBiomeTag());
    }

    /**
     * Whether weather and time let this bee work at pos: vanilla bees stop at night and in rain. Water-safe bees work
     * in rain, snowproof bees while it snows there, nocturnal bees only at night (and, with checkSky, only from a
     * spot under open sky: the hive's front, not the flying bee).
     */
    public boolean canWorkAt(Level level, BlockPos pos, boolean checkSky) {
        if (has(Trait.NOCTURNAL)) {
            if (!level.isNight() || (checkSky && !level.canSeeSky(pos))) return false;
        } else if (level.isNight()) {
            return false;
        }
        if (level.isRaining()) {
            if (has(Trait.WATER_SAFE)) return true;
            return has(Trait.SNOWPROOF) && level.getBiome(pos).value().coldEnoughToSnow(pos);
        }
        return true;
    }

    /** Which star its conversion gem needs; NONE = no gem (the royal queen is summoned). */
    public enum Tier { LESSER, BUBBA, CELESTIAL, NONE }

    /** Comb ids name items "bubbasbees:<comb>_honeycomb"; VANILLA_HONEYCOMB is the plain vanilla honeycomb. */
    public static final String VANILLA_HONEYCOMB = "minecraft:honeycomb";

    public record WeightedComb(String comb, int weight) {}

    public record RareComb(String comb, int oneIn) {}

    public String entityId() {
        return id + "_bee";
    }

    public ResourceLocation entityKey() {
        return new ResourceLocation(BubbasBees.MOD_ID, entityId());
    }

    public boolean hasGem() {
        return tier != Tier.NONE;
    }

    /** The blocks this bee pollinates: data/bubbasbees/tags/blocks/bee_flowers/<id>.json (compat packs can add). */
    public TagKey<Block> flowerTag() {
        return TagKey.create(Registry.BLOCK_REGISTRY, new ResourceLocation(BubbasBees.MOD_ID, "bee_flowers/" + id));
    }

    /** Spends twice as long in a hive outside its home dimension (the dimension rule). */
    public boolean isAwayFromHome(Level level) {
        return home != null && !level.dimension().equals(home);
    }

    public ItemStack rollComb(RandomSource random) {
        for (RareComb rare : rareCombs) {
            if (random.nextInt(rare.oneIn()) == 0) {
                return combStack(rare.comb());
            }
        }
        if (combs.isEmpty() || random.nextFloat() >= combChance) {
            return ItemStack.EMPTY;
        }
        int total = 0;
        for (WeightedComb comb : combs) total += comb.weight();
        int pick = random.nextInt(total);
        for (WeightedComb comb : combs) {
            pick -= comb.weight();
            if (pick < 0) return combStack(comb.comb());
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack combStack(String comb) {
        if (VANILLA_HONEYCOMB.equals(comb)) return new ItemStack(Items.HONEYCOMB);
        return new ItemStack(ModItems.comb(comb));
    }

    /** Every mod comb id a spec can drop (its own combs and its rare combs). */
    public List<String> modCombIds() {
        List<String> ids = new ArrayList<>();
        for (WeightedComb comb : combs) if (!VANILLA_HONEYCOMB.equals(comb.comb())) ids.add(comb.comb());
        for (RareComb rare : rareCombs) ids.add(rare.comb());
        return ids;
    }

    public static Builder of(String id, Tier tier) {
        return new Builder(id, tier);
    }

    public static final class Builder {
        private final String id;
        private final Tier tier;
        private float combChance = 1.0F;
        private final List<WeightedComb> combs = new ArrayList<>();
        private final List<RareComb> rareCombs = new ArrayList<>();
        private ResourceKey<Level> home;
        private boolean fireImmune;
        private boolean deepslateLook;
        private boolean translucent;
        private String model = "ore";
        private final EnumSet<Trait> traits = EnumSet.noneOf(Trait.class);
        private double maxHealth = 10.0D;
        private int eggBase = 0xffffff;
        private int eggSpots = 0x000000;

        private Builder(String id, Tier tier) {
            this.id = id;
            this.tier = tier;
        }

        /** Chance of the bee's own comb per trip; the default is 100% (only some ore bees roll lower). */
        public Builder combChance(float chance) {
            this.combChance = chance;
            return this;
        }

        public Builder comb(String comb, int weight) {
            combs.add(new WeightedComb(comb, weight));
            return this;
        }

        public Builder rare(String comb, int oneIn) {
            rareCombs.add(new RareComb(comb, oneIn));
            return this;
        }

        public Builder home(ResourceKey<Level> dimension) {
            this.home = dimension;
            return this;
        }

        public Builder fireImmune() {
            this.fireImmune = true;
            return this;
        }

        /** Rolls a stone or a deepslate look per bee (textures/entity/<id>_deepslate_bee.png). */
        public Builder deepslateLook() {
            this.deepslateLook = true;
            return this;
        }

        /** See-through body (the soul bee): rendered with the entityTranslucent render type. */
        public Builder translucent() {
            this.translucent = true;
            return this;
        }

        /** geo/<model>.geo.json; the default is the plain bee body ("ore"). */
        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder traits(Trait... traits) {
            this.traits.addAll(List.of(traits));
            return this;
        }

        public Builder health(double maxHealth) {
            this.maxHealth = maxHealth;
            return this;
        }

        public Builder egg(int base, int spots) {
            this.eggBase = base;
            this.eggSpots = spots;
            return this;
        }

        public BeeSpec build() {
            if (combs.isEmpty()) combs.add(new WeightedComb(id, 1));
            return new BeeSpec(id, tier, combChance, List.copyOf(combs), List.copyOf(rareCombs), home, fireImmune,
                    deepslateLook, translucent, model, Set.copyOf(traits), maxHealth, eggBase, eggSpots);
        }
    }
}
