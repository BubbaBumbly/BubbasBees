package net.bubba.bubbasbees.entity.goals;

import net.bubba.bubbasbees.entity.custom.BubbasBee;
import net.bubba.bubbasbees.entity.custom.CopperBee;
import net.bubba.bubbasbees.util.ListUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.Optional;
import java.util.function.Predicate;

public class ModBeePollinateGoal extends Bee.BeePollinateGoal {

    private final BubbasBee bubbasBee;
    private int nextSearchTick = 0;

    public ModBeePollinateGoal(BubbasBee bee) {
        bee.super();
        this.bubbasBee = bee;
        this.VALID_POLLINATION_BLOCKS = this.createPredicate();
    }

    private Predicate<BlockState> createPredicate() {
        return (state) -> {
            if (state.hasProperty(BlockStateProperties.WATERLOGGED)
                    && state.getValue(BlockStateProperties.WATERLOGGED)) {
                return false;
            }
            return ListUtils.withinBlockList(bubbasBee.validFlowerList, state.getBlock());
        };
    }

    @Override
    public Optional<BlockPos> findNearbyFlower() {
        BlockPos beePos = bubbasBee.blockPosition();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        double radius = 5.0D;

        for (int i = 0; (double)i <= radius; i = i > 0 ? -i : 1 - i) {
            for (int j = 0; (double)j < radius; ++j) {
                for (int k = 0; k <= j; k = k > 0 ? -k : 1 - k) {
                    for (int l = k < j && k > -j ? j : 0; l <= j; l = l > 0 ? -l : 1 - l) {
                        mutablePos.setWithOffset(beePos, k, i - 1, l);
                        if (beePos.closerThan(mutablePos, radius)
                                && this.VALID_POLLINATION_BLOCKS.test(bubbasBee.level.getBlockState(mutablePos))
                                && bubbasBee.level.getBlockState(mutablePos.above()).isAir()) {
                            return Optional.of(mutablePos);
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean canBeeUse() {
        if (bubbasBee.tickCount < this.nextSearchTick) return false;
        if (bubbasBee.hasNectar()) return false;
        if (bubbasBee.level.isRaining()) return false;

        Optional<BlockPos> optional = this.findNearbyFlower();
        if (optional.isPresent()) {
            bubbasBee.savedFlowerPos = optional.get();
            bubbasBee.getNavigation().moveTo(
                    (double)bubbasBee.savedFlowerPos.getX() + 0.5D,
                    (double)bubbasBee.savedFlowerPos.getY() + 0.5D,
                    (double)bubbasBee.savedFlowerPos.getZ() + 0.5D,
                    (double)1.2F);
            this.nextSearchTick = bubbasBee.tickCount;
            return true;
        }
        this.nextSearchTick = bubbasBee.tickCount + Mth.nextInt(bubbasBee.level.getRandom(), 20, 60);
        return false;
    }

    @Override
    public boolean canBeeContinueToUse() {
        if (!this.isPollinating()) return false;
        if (!bubbasBee.hasSavedFlowerPos()) return false;
        if (bubbasBee.level.isRaining()) return false;
        if (this.hasPollinatedLongEnough()) return bubbasBee.level.getRandom().nextFloat() < 0.2F;
        if (bubbasBee.tickCount % 20 == 0 && !bubbasBee.isFlowerValid(bubbasBee.savedFlowerPos)) {
            bubbasBee.savedFlowerPos = null;
            return false;
        }
        if (bubbasBee.tickCount % 20 == 0 && !bubbasBee.level.getBlockState(bubbasBee.savedFlowerPos.above()).isAir()) {
            bubbasBee.savedFlowerPos = null;
            return false;
        }
        return true;
    }

    @Override
    public void setWantedPos() {
        bubbasBee.getMoveControl().setWantedPosition(
                this.hoverPos.x(), this.hoverPos.y() + 0.5, this.hoverPos.z(), 0.3499999940395355D);
    }

    @Override
    public void stop() {
        super.stop();
        this.nextSearchTick = bubbasBee.tickCount + 200;
        if (bubbasBee instanceof CopperBee copperBee) {
            this.copperAging(copperBee);
        }
    }

    private void copperAging(BubbasBee bee) {
        BlockPos blockpos = bee.blockPosition().below(1);
        BlockState blockstate = bee.level.getBlockState(blockpos);
        BlockPos particlesPos = blockpos.above(1);
        double xpos = particlesPos.getX();
        double ypos = particlesPos.getY();
        double zpos = particlesPos.getZ();
        if (blockstate.is(Blocks.COPPER_BLOCK)) {
            bee.level.setBlockAndUpdate(blockpos, Blocks.EXPOSED_COPPER.defaultBlockState());
            if (bee.level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, xpos + 0.5, ypos + 0.2, zpos + 0.5, 5, 0.2, 0.2, 0.2, 0);
            }
        } else if (blockstate.is(Blocks.EXPOSED_COPPER)) {
            bee.level.setBlockAndUpdate(blockpos, Blocks.WEATHERED_COPPER.defaultBlockState());
            if (bee.level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, xpos + 0.5, ypos + 0.2, zpos + 0.5, 5, 0.2, 0.2, 0.2, 0);
            }
        } else if (blockstate.is(Blocks.WEATHERED_COPPER)) {
            bee.level.setBlockAndUpdate(blockpos, Blocks.OXIDIZED_COPPER.defaultBlockState());
            if (bee.level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, xpos + 0.5, ypos + 0.2, zpos + 0.5, 5, 0.2, 0.2, 0.2, 0);
            }
        }
    }
}
