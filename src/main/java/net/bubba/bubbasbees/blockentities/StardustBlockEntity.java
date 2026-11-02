package net.bubba.bubbasbees.blockentities;

import net.bubba.bubbasbees.registries.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Holds no data: it only lets StardustPileRenderer draw the end-rod pile, which reaches into the rod's block. */
public class StardustBlockEntity extends BlockEntity {
    public StardustBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STARDUST.get(), pos, state);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(1.0, 0.0, 1.0).expandTowards(0.0, 1.0, 0.0);
    }
}
