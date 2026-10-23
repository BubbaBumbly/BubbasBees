package net.bubba.bubbasbees.registries;

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
