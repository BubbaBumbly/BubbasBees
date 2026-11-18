package net.bubba.bubbasbees.registries;

import net.bubba.bubbasbees.BubbasBees;
import net.bubba.bubbasbees.blocks.HiveWood;
import net.bubba.bubbasbees.blocks.custom.AdvancedBeehiveBlock;
import net.bubba.bubbasbees.blocks.custom.BeeLightBlock;
import net.bubba.bubbasbees.blocks.custom.CombChuteBlock;
import net.bubba.bubbasbees.blocks.custom.PartialLavaCauldronBlock;
import net.bubba.bubbasbees.blocks.custom.PedestalBlock;
import net.bubba.bubbasbees.blocks.custom.PinkPistonHeadBlock;
import net.bubba.bubbasbees.blocks.custom.RoyalJellyBlock;
import net.bubba.bubbasbees.blocks.custom.StardustBlock;
import net.bubba.bubbasbees.util.Reg;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ModBlocks {

    private static ResourceLocation rl(String name) {
        return ResourceLocation.fromNamespaceAndPath(BubbasBees.MOD_ID, name);
    }

    /// Start Mod blocks
    // A plain and a reinforced hive per wood (HiveWood); oak's plain hive is the default
    // "advanced_beehive". Reinforced hives take iron and resist explosions like obsidian. Neither
    // model fills the whole block (glass panes, raised bands), hence noOcclusion.
    public static final Reg<Block> ADVANCED_BEEHIVE = Reg.of(rl("advanced_beehive"));

    public static final List<Reg<Block>> HIVES = new ArrayList<>();
    private static final Map<Reg<Block>, Float> HIVE_BLAST = new LinkedHashMap<>();

    static {
        addHive(ADVANCED_BEEHIVE, 1f);
        for (HiveWood wood : HiveWood.ALL) {
            if (!wood.isAvailable()) continue;
            if (!wood.id().equals("oak")) addHive(Reg.of(rl(wood.plainName())), 1f);
            addHive(Reg.of(rl(wood.reinforcedName())), 1200f);
        }
    }

    private static void addHive(Reg<Block> reg, float blastResistance) {
        HIVES.add(reg);
        HIVE_BLAST.put(reg, blastResistance);
    }

    /// Pedestals: display blocks (item floats above), and the Celestial Altar at the centre of the rituals
    public static final Reg<Block> STONE_PEDESTAL = Reg.of(rl("stone_pedestal"));
    public static final Reg<Block> DEEPSLATE_PEDESTAL = Reg.of(rl("deepslate_pedestal"));
    public static final Reg<Block> BLACKSTONE_PEDESTAL = Reg.of(rl("blackstone_pedestal"));
    public static final Reg<Block> QUARTZ_PEDESTAL = Reg.of(rl("quartz_pedestal"));
    public static final Reg<Block> CELESTIAL_ALTAR = Reg.of(rl("celestial_altar"));
    public static final List<Reg<Block>> PEDESTALS = List.of(STONE_PEDESTAL, DEEPSLATE_PEDESTAL,
            BLACKSTONE_PEDESTAL, QUARTZ_PEDESTAL, CELESTIAL_ALTAR);

    /** Stardust laid like redstone dust (its item is ModItems.STARDUST). */
    public static final Reg<Block> STARDUST = Reg.of(rl("stardust"));

    /** Lava cauldron at 1/4, 2/4, 3/4 (the magma bee fills it); no item, it drops a cauldron. */
    public static final Reg<Block> PARTIAL_LAVA_CAULDRON = Reg.of(rl("partial_lava_cauldron"));

    /** Royal queen products: the jelly block (sink, no bounce, piston-sticky) and the pink sticky piston. */
    public static final Reg<Block> ROYAL_JELLY_BLOCK = Reg.of(rl("royal_jelly_block"));
    public static final Reg<Block> PINK_STICKY_PISTON = Reg.of(rl("pink_sticky_piston"));
    public static final Reg<Block> PINK_PISTON_HEAD = Reg.of(rl("pink_piston_head"));

    /** Funnel under a hive: harvests a full hive (combs, or honey bottles in honey mode) and pushes down like a hopper.
     *  Its item keeps the flat icon (ModItems.COMB_CHUTE), like the hopper's. */
    public static final Reg<Block> COMB_CHUTE = Reg.of(rl("comb_chute"));

    /** The glow bee's moving light (invisible, removes itself). */
    public static final Reg<Block> BEE_LIGHT = Reg.of(rl("bee_light"));

    /// End Mod blocks
    private static <T extends Block> Reg<T> registerBlock(Reg<T> reg, Supplier<T> block) {
        reg.set(Registry.register(BuiltInRegistries.BLOCK, reg.id(), block.get()));
        ModItems.registerBlockItem(reg.id().getPath(), () -> new BlockItem(reg.get(), new Item.Properties()));
        return reg;
    }

    private static void registerPedestal(Reg<Block> reg, Block material, boolean altar) {
        registerBlock(reg, () -> new PedestalBlock(BlockBehaviour.Properties.ofFullCopy(material).noOcclusion()
                .lightLevel(state -> altar ? 10 : 0), altar));
    }

    public static void register() {
        for (Reg<Block> hive : HIVES) {
            float blastResistance = HIVE_BLAST.get(hive);
            registerBlock(hive, () -> new AdvancedBeehiveBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .ignitedByLava()
                    .strength(1f, blastResistance)
                    .noOcclusion()));
        }

        registerPedestal(STONE_PEDESTAL, Blocks.STONE_BRICKS, false);
        registerPedestal(DEEPSLATE_PEDESTAL, Blocks.POLISHED_DEEPSLATE, false);
        registerPedestal(BLACKSTONE_PEDESTAL, Blocks.POLISHED_BLACKSTONE, false);
        registerPedestal(QUARTZ_PEDESTAL, Blocks.QUARTZ_BLOCK, false);
        registerPedestal(CELESTIAL_ALTAR, Blocks.CRYING_OBSIDIAN, true);

        STARDUST.set(Registry.register(BuiltInRegistries.BLOCK, STARDUST.id(),
                new StardustBlock(BlockBehaviour.Properties.of().mapColor(MapColor.NONE).replaceable()
                        .noCollission().instabreak().lightLevel(state -> 7).sound(SoundType.AMETHYST_CLUSTER)
                    // 1.19.2 Material.DECORATION breaks when a piston pushes it
                    .pushReaction(PushReaction.DESTROY))));

        PARTIAL_LAVA_CAULDRON.set(Registry.register(BuiltInRegistries.BLOCK, PARTIAL_LAVA_CAULDRON.id(),
                new PartialLavaCauldronBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CAULDRON).lightLevel(state -> 15))));

        registerBlock(ROYAL_JELLY_BLOCK, () -> new RoyalJellyBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.HONEY_BLOCK).noOcclusion()
                .isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false)
                .sound(SoundType.HONEY_BLOCK)));
        registerBlock(PINK_STICKY_PISTON, () -> new PistonBaseBlock(true, BlockBehaviour.Properties.ofFullCopy(Blocks.STICKY_PISTON)));
        PINK_PISTON_HEAD.set(Registry.register(BuiltInRegistries.BLOCK, PINK_PISTON_HEAD.id(),
                new PinkPistonHeadBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.PISTON_HEAD).noLootTable())));

        COMB_CHUTE.set(Registry.register(BuiltInRegistries.BLOCK, COMB_CHUTE.id(),
                new CombChuteBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.HOPPER).noOcclusion())));

        BEE_LIGHT.set(Registry.register(BuiltInRegistries.BLOCK, BEE_LIGHT.id(),
                new BeeLightBlock(BlockBehaviour.Properties.of().noCollission().noLootTable().air()
                    // 1.19.2 Material.AIR is replaceable and stops pistons
                    .replaceable().pushReaction(PushReaction.BLOCK)
                        .lightLevel(state -> 10))));
    }
}
