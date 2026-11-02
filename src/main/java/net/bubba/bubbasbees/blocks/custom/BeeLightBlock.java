package net.bubba.bubbasbees.blocks.custom;

import net.bubba.bubbasbees.bees.BeeSpec;
import net.bubba.bubbasbees.entity.custom.BubbasBee;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

/**
 * The glow bee's moving light: an invisible air block that gives light 10. The bee places one where it flies through
 * the dark; the block removes itself once no glow bee is close, so none are left behind (even after a crash: the
 * check is a scheduled tick saved with the chunk).
 */
public class BeeLightBlock extends AirBlock {
    public static final int CHECK_TICKS = 10;

    public BeeLightBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void onPlace(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState old, boolean moved) {
        level.scheduleTick(pos, this, CHECK_TICKS);
    }

    @Override
    public void tick(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random) {
        boolean glowBeeHere = !level.getEntitiesOfClass(BubbasBee.class, new AABB(pos).inflate(1.0D),
                bee -> bee.getSpec() != null && bee.getSpec().has(BeeSpec.Trait.GLOW) && bee.isAlive()).isEmpty();
        if (glowBeeHere) {
            level.scheduleTick(pos, this, CHECK_TICKS);
        } else {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }
}
