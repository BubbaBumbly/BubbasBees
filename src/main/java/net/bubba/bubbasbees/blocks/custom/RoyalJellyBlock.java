package net.bubba.bubbasbees.blocks.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

/**
 * Royal jelly block (finish-spec.md, royal queen card): no bounce; entities sink into it over ~2 s and are held,
 * heavily slowed, until they have tried to move for ~1 s, then they break free for a moment. Sticks to blocks on
 * pistons like slime, but not to slime or honey blocks.
 */
public class RoyalJellyBlock extends HalfTransparentBlock {
    /** Entities rest on a thin floor near the bottom; the rest of the block is jelly they sink through. */
    private static final VoxelShape FLOOR = Block.box(0, 0, 0, 16, 1, 16);
    /** Held: velocity multiplier while stuck (sideways, down). ~0.32 down sinks one block in about 2 s. */
    private static final Vec3 HOLD = new Vec3(0.1D, 0.32D, 0.1D);
    private static final int STRUGGLE_TICKS = 20;
    private static final int FREE_TICKS = 40;
    private static final String STRUGGLE_TAG = "BubbasJellyStruggle";
    private static final String FREE_UNTIL_TAG = "BubbasJellyFreeUntil";

    public RoyalJellyBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos,
                                                 @NotNull CollisionContext context) {
        // Entities sink in; everything else (falling blocks, pathfinding checks without an entity) sees a full block
        return context instanceof EntityCollisionContext entityContext && entityContext.getEntity() != null
                ? FLOOR : super.getCollisionShape(state, level, pos, context);
    }

    @Override
    public @NotNull VoxelShape getVisualShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos,
                                              @NotNull CollisionContext context) {
        return net.minecraft.world.phys.shapes.Shapes.empty();
    }

    @Override
    public void entityInside(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Entity entity) {
        long now = level.getGameTime();
        var data = entity.getPersistentData();
        if (now < data.getLong(FREE_UNTIL_TAG)) return;
        // Movement input is known where the entity is simulated: the client for the local player, the server for mobs
        boolean trying = entity instanceof LivingEntity living && (living.xxa != 0.0F || living.zza != 0.0F);
        int struggle = trying ? data.getInt(STRUGGLE_TAG) + 1 : Math.max(0, data.getInt(STRUGGLE_TAG) - 1);
        if (struggle >= STRUGGLE_TICKS) {
            data.putInt(STRUGGLE_TAG, 0);
            data.putLong(FREE_UNTIL_TAG, now + FREE_TICKS);
            return;
        }
        data.putInt(STRUGGLE_TAG, struggle);
        entity.makeStuckInBlock(state, HOLD);
    }

    @Override
    public void fallOn(@NotNull Level level, @NotNull BlockState state, @NotNull BlockPos pos, @NotNull Entity entity, float distance) {
        // No bounce; the jelly cushions like a honey block
        entity.causeFallDamage(distance, 0.2F, net.minecraft.world.damagesource.DamageSource.FALL);
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
