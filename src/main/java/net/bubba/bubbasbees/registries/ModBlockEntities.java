package net.bubba.bubbasbees.registries;

import net.bubba.bubbasbees.blockentities.PartialLavaCauldronBlockEntity;
import net.bubba.bubbasbees.blockentities.PedestalBlockEntity;
import net.bubba.bubbasbees.blockentities.StardustBlockEntity;

import net.bubba.bubbasbees.BubbasBees;
import net.bubba.bubbasbees.blockentities.AdvancedBeehiveBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, BubbasBees.MOD_ID);


    public static final RegistryObject<BlockEntityType<AdvancedBeehiveBlockEntity>> ADVANCED_BEEHIVE =
            BLOCK_ENTITIES.register("advanced_beehive", () ->
                    BlockEntityType.Builder.of(AdvancedBeehiveBlockEntity::new,
                            ModBlocks.HIVES.stream().map(RegistryObject::get).toArray(Block[]::new)).build(null));



    public static final RegistryObject<BlockEntityType<PedestalBlockEntity>> PEDESTAL =
            BLOCK_ENTITIES.register("pedestal", () -> BlockEntityType.Builder.of(PedestalBlockEntity::new,
                    ModBlocks.PEDESTALS.stream().map(RegistryObject::get).toArray(Block[]::new)).build(null));

    public static final RegistryObject<BlockEntityType<PartialLavaCauldronBlockEntity>> PARTIAL_LAVA_CAULDRON =
            BLOCK_ENTITIES.register("partial_lava_cauldron", () -> BlockEntityType.Builder.of(
                    PartialLavaCauldronBlockEntity::new, ModBlocks.PARTIAL_LAVA_CAULDRON.get()).build(null));

    public static final RegistryObject<BlockEntityType<StardustBlockEntity>> STARDUST =
            BLOCK_ENTITIES.register("stardust", () -> BlockEntityType.Builder.of(
                    StardustBlockEntity::new, ModBlocks.STARDUST.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
