package net.bubba.bubbasbees.registries;

import net.bubba.bubbasbees.blockentities.CombChuteBlockEntity;

import net.bubba.bubbasbees.blockentities.PartialLavaCauldronBlockEntity;
import net.bubba.bubbasbees.blockentities.PedestalBlockEntity;
import net.bubba.bubbasbees.blockentities.StardustBlockEntity;

import net.bubba.bubbasbees.BubbasBees;
import net.bubba.bubbasbees.blockentities.AdvancedBeehiveBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, BubbasBees.MOD_ID);


    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AdvancedBeehiveBlockEntity>> ADVANCED_BEEHIVE =
            BLOCK_ENTITIES.register("advanced_beehive", () ->
                    BlockEntityType.Builder.of(AdvancedBeehiveBlockEntity::new,
                            ModBlocks.HIVES.stream().map(DeferredHolder::get).toArray(Block[]::new)).build(null));



    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PedestalBlockEntity>> PEDESTAL =
            BLOCK_ENTITIES.register("pedestal", () -> BlockEntityType.Builder.of(PedestalBlockEntity::new,
                    ModBlocks.PEDESTALS.stream().map(DeferredHolder::get).toArray(Block[]::new)).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PartialLavaCauldronBlockEntity>> PARTIAL_LAVA_CAULDRON =
            BLOCK_ENTITIES.register("partial_lava_cauldron", () -> BlockEntityType.Builder.of(
                    PartialLavaCauldronBlockEntity::new, ModBlocks.PARTIAL_LAVA_CAULDRON.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StardustBlockEntity>> STARDUST =
            BLOCK_ENTITIES.register("stardust", () -> BlockEntityType.Builder.of(
                    StardustBlockEntity::new, ModBlocks.STARDUST.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CombChuteBlockEntity>> COMB_CHUTE =
            BLOCK_ENTITIES.register("comb_chute", () -> BlockEntityType.Builder.of(
                    CombChuteBlockEntity::new, ModBlocks.COMB_CHUTE.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
