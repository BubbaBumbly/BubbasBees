package net.bubba.bubbasbees.registries;

import net.bubba.bubbasbees.BubbasBees;
import net.bubba.bubbasbees.blockentities.AdvancedBeehiveBlockEntity;
import net.bubba.bubbasbees.blockentities.CombChuteBlockEntity;
import net.bubba.bubbasbees.blockentities.PartialLavaCauldronBlockEntity;
import net.bubba.bubbasbees.blockentities.PedestalBlockEntity;
import net.bubba.bubbasbees.blockentities.StardustBlockEntity;
import net.bubba.bubbasbees.util.Reg;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntities {
    private static ResourceLocation rl(String name) {
        return ResourceLocation.fromNamespaceAndPath(BubbasBees.MOD_ID, name);
    }

    public static final Reg<BlockEntityType<AdvancedBeehiveBlockEntity>> ADVANCED_BEEHIVE = Reg.of(rl("advanced_beehive"));
    public static final Reg<BlockEntityType<PedestalBlockEntity>> PEDESTAL = Reg.of(rl("pedestal"));
    public static final Reg<BlockEntityType<PartialLavaCauldronBlockEntity>> PARTIAL_LAVA_CAULDRON = Reg.of(rl("partial_lava_cauldron"));
    public static final Reg<BlockEntityType<StardustBlockEntity>> STARDUST = Reg.of(rl("stardust"));
    public static final Reg<BlockEntityType<CombChuteBlockEntity>> COMB_CHUTE = Reg.of(rl("comb_chute"));

    public static void register() {
        ADVANCED_BEEHIVE.set(Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ADVANCED_BEEHIVE.id(),
                BlockEntityType.Builder.of(AdvancedBeehiveBlockEntity::new,
                        ModBlocks.HIVES.stream().map(Reg::get).toArray(Block[]::new)).build(null)));

        PEDESTAL.set(Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, PEDESTAL.id(),
                BlockEntityType.Builder.of(PedestalBlockEntity::new,
                        ModBlocks.PEDESTALS.stream().map(Reg::get).toArray(Block[]::new)).build(null)));

        PARTIAL_LAVA_CAULDRON.set(Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, PARTIAL_LAVA_CAULDRON.id(),
                BlockEntityType.Builder.of(PartialLavaCauldronBlockEntity::new, ModBlocks.PARTIAL_LAVA_CAULDRON.get()).build(null)));

        STARDUST.set(Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, STARDUST.id(),
                BlockEntityType.Builder.of(StardustBlockEntity::new, ModBlocks.STARDUST.get()).build(null)));

        COMB_CHUTE.set(Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, COMB_CHUTE.id(),
                BlockEntityType.Builder.of(CombChuteBlockEntity::new, ModBlocks.COMB_CHUTE.get()).build(null)));
    }
}
