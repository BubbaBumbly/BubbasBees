package net.bubba.bubbasbees.registries;

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



    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
