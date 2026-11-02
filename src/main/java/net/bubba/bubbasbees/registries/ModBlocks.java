package net.bubba.bubbasbees.registries;

import net.bubba.bubbasbees.blocks.custom.PartialLavaCauldronBlock;
import net.bubba.bubbasbees.blocks.custom.PedestalBlock;
import net.bubba.bubbasbees.blocks.custom.StardustBlock;
import net.bubba.bubbasbees.blocks.custom.RoyalJellyBlock;
import net.bubba.bubbasbees.blocks.custom.PinkPistonHeadBlock;
import net.bubba.bubbasbees.blocks.custom.BeeLightBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;

import net.bubba.bubbasbees.BubbasBees;
import net.bubba.bubbasbees.blocks.HiveWood;
import net.bubba.bubbasbees.blocks.custom.AdvancedBeehiveBlock;
import net.bubba.bubbasbees.item.ModCreativeModeTab;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, BubbasBees.MOD_ID);

    /// Start Mod blocks
    // A plain and a reinforced hive per wood (HiveWood); oak's plain hive is the default
    // "advanced_beehive". Reinforced hives take iron and resist explosions like obsidian. Neither
    // model fills the whole block (glass panes, raised bands), hence noOcclusion.
    public static final RegistryObject<Block> ADVANCED_BEEHIVE = registerHive("advanced_beehive", 1f);

    public static final List<RegistryObject<Block>> HIVES = new ArrayList<>();

    static {
        HIVES.add(ADVANCED_BEEHIVE);
        for (HiveWood wood : HiveWood.ALL) {
            if (!wood.isAvailable()) continue;
            if (!wood.id().equals("oak")) HIVES.add(registerHive(wood.plainName(), 1f));
            HIVES.add(registerHive(wood.reinforcedName(), 1200f));
        }
    }

    private static RegistryObject<Block> registerHive(String name, float blastResistance) {
        return registerBlock(name, () -> new AdvancedBeehiveBlock(BlockBehaviour.Properties.of(Material.WOOD)
                .strength(1f, blastResistance).requiresCorrectToolForDrops().noOcclusion()),
                ModCreativeModeTab.BUBBASBEES_TAB);
    }



    /// Pedestals: display blocks (item floats above), and the Celestial Altar at the centre of the rituals
    public static final RegistryObject<Block> STONE_PEDESTAL = registerPedestal("stone_pedestal", Blocks.STONE_BRICKS, false);
    public static final RegistryObject<Block> DEEPSLATE_PEDESTAL = registerPedestal("deepslate_pedestal", Blocks.POLISHED_DEEPSLATE, false);
    public static final RegistryObject<Block> BLACKSTONE_PEDESTAL = registerPedestal("blackstone_pedestal", Blocks.POLISHED_BLACKSTONE, false);
    public static final RegistryObject<Block> QUARTZ_PEDESTAL = registerPedestal("quartz_pedestal", Blocks.QUARTZ_BLOCK, false);
    public static final RegistryObject<Block> CELESTIAL_ALTAR = registerPedestal("celestial_altar", Blocks.CRYING_OBSIDIAN, true);
    public static final List<RegistryObject<Block>> PEDESTALS = List.of(STONE_PEDESTAL, DEEPSLATE_PEDESTAL,
            BLACKSTONE_PEDESTAL, QUARTZ_PEDESTAL, CELESTIAL_ALTAR);

    /** Stardust laid like redstone dust (its item is ModItems.STARDUST). */
    public static final RegistryObject<Block> STARDUST = BLOCKS.register("stardust",
            () -> new StardustBlock(BlockBehaviour.Properties.of(Material.DECORATION).noCollission().instabreak()
                    .lightLevel(state -> 7).sound(SoundType.AMETHYST_CLUSTER)));

    /** Lava cauldron at 1/4, 2/4, 3/4 (the magma bee fills it); no item, it drops a cauldron. */
    public static final RegistryObject<Block> PARTIAL_LAVA_CAULDRON = BLOCKS.register("partial_lava_cauldron",
            () -> new PartialLavaCauldronBlock(BlockBehaviour.Properties.copy(Blocks.CAULDRON).lightLevel(state -> 15)));

    /** Royal queen products: the jelly block (sink, no bounce, piston-sticky) and the pink sticky piston. */
    public static final RegistryObject<Block> ROYAL_JELLY_BLOCK = registerBlock("royal_jelly_block",
            () -> new RoyalJellyBlock(BlockBehaviour.Properties.copy(Blocks.HONEY_BLOCK).noOcclusion()
                    .isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false)
                    .sound(SoundType.HONEY_BLOCK)), ModCreativeModeTab.BUBBASBEES_TAB);
    public static final RegistryObject<Block> PINK_STICKY_PISTON = registerBlock("pink_sticky_piston",
            () -> new PistonBaseBlock(true, BlockBehaviour.Properties.copy(Blocks.STICKY_PISTON)),
            ModCreativeModeTab.BUBBASBEES_TAB);
    public static final RegistryObject<Block> PINK_PISTON_HEAD = BLOCKS.register("pink_piston_head",
            () -> new PinkPistonHeadBlock(BlockBehaviour.Properties.copy(Blocks.PISTON_HEAD).noLootTable()));

    /** The glow bee's moving light (invisible, removes itself). */
    public static final RegistryObject<Block> BEE_LIGHT = BLOCKS.register("bee_light",
            () -> new BeeLightBlock(BlockBehaviour.Properties.of(Material.AIR).noCollission().noLootTable().air()
                    .lightLevel(state -> 10)));

    private static RegistryObject<Block> registerPedestal(String name, Block material, boolean altar) {
        return registerBlock(name, () -> new PedestalBlock(BlockBehaviour.Properties.copy(material).noOcclusion()
                .lightLevel(state -> altar ? 10 : 0), altar), ModCreativeModeTab.BUBBASBEES_TAB);
    }

    /// End Mod blocks
    private static <T extends Block>RegistryObject<T>
    registerBlock(String name, Supplier<T> block, CreativeModeTab tab){
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn, tab);
        return toReturn;
    }

    private static <T extends Block>RegistryObject<Item>
    registerBlockItem(String name, RegistryObject<T> block, CreativeModeTab tab){
        return ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties().tab(tab)));
    }

    public static void register(IEventBus eventBus){
        BLOCKS.register(eventBus);


    }
}
