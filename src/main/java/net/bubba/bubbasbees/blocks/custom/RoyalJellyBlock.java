package net.bubba.bubbasbees.blocks.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

/**
 * Royal jelly block: anything inside moves through it as if it weren't there, at about half speed, and falls slowly.
 * Entering it resets a fall. Sticks to blocks on pistons like slime, but not to slime or honey blocks.
 */
public class RoyalJellyBlock extends HalfTransparentBlock {
    /** Multiplied into velocity each tick inside: about half walking speed, and a steady slow fall. */
    private static final double SLOW_SIDEWAYS = 0.7D, SLOW_VERTICAL = 0.5D;

    public RoyalJellyBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos,
                                                 @NotNull CollisionContext context) {
        // Entities pass through; everything else (falling blocks, pathfinding checks without an entity) sees a full block
        return context instanceof EntityCollisionContext entityContext && entityContext.getEntity() != null
                ? net.minecraft.world.phys.shapes.Shapes.empty() : super.getCollisionShape(state, level, pos, context);
    }

    @Override
    public @NotNull VoxelShape getVisualShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos,
                                              @NotNull CollisionContext context) {
        return net.minecraft.world.phys.shapes.Shapes.empty();
    }

    @Override
    public void entityInside(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Entity entity) {
        var velocity = entity.getDeltaMovement();
        entity.setDeltaMovement(velocity.x * SLOW_SIDEWAYS, velocity.y * SLOW_VERTICAL, velocity.z * SLOW_SIDEWAYS);
        entity.resetFallDistance();
    }

    @Override
    public boolean isStickyBlock(BlockState state) {
        return true;
    }

    @Override
    public boolean canStickTo(BlockState state, BlockState other) {
        return !other.is(Blocks.SLIME_BLOCK) && !other.is(Blocks.HONEY_BLOCK);
    }
}
