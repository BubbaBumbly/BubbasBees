package net.bubba.bubbasbees.entity.goals;

import net.bubba.bubbasbees.entity.custom.BubbasBee;
import net.bubba.bubbasbees.entity.custom.CopperBee;
import net.bubba.bubbasbees.mixin.BeePollinateGoalAccessor;
import net.bubba.bubbasbees.util.ListUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.function.Predicate;

public class ModBeePollinateGoal extends Bee.BeePollinateGoal {

    private final BubbasBee bubbasBee;
    private int nextSearchTick = 0;
    private boolean rolledThisCycle = false;
    private boolean exploring = false;
    private int exploreTimer = 0;
    private boolean seekingNewFlower = false;
    private boolean reachedFlower = false;
    private BlockPos lastPollinatedPos = null;
    private static final int MAX_POLLINATING_TICKS = 600;
    private static final double SOLID_HOVER_HEIGHT = 0.5D;
    private static final double FLOWER_HOVER_HEIGHT = 0.6D;

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

        if (bubbasBee.savedFlowerPos != null && bubbasBee.isFlowerValid(bubbasBee.savedFlowerPos)
                && bubbasBee.level.getBlockState(bubbasBee.savedFlowerPos.above()).isAir()) {
            navigateToPos(bubbasBee.savedFlowerPos);
            return true;
        }

        if (!rolledThisCycle) {
            rolledThisCycle = true;
            if (bubbasBee.getRandom().nextFloat() < 0.2F) {
                exploring = true;
                exploreTimer = 20;
            }
        }

        if (exploring) {
            if (exploreTimer > 0) {
                exploreTimer--;
                return false;
            }
            exploring = false;
            seekingNewFlower = true;
        }

        if (seekingNewFlower) {
            Optional<BlockPos> newFlower = findFlowerExcludingHistory();
            if (newFlower.isPresent()) {
                bubbasBee.savedFlowerPos = newFlower.get();
                navigateToPos(bubbasBee.savedFlowerPos);
                seekingNewFlower = false;
                return true;
            }
            seekingNewFlower = false;
        }

        Optional<BlockPos> flower = findNearbyFlower();
        if (flower.isPresent()) {
            bubbasBee.savedFlowerPos = flower.get();
            navigateToPos(bubbasBee.savedFlowerPos);
            return true;
        }

        this.nextSearchTick = bubbasBee.tickCount + Mth.nextInt(bubbasBee.getRandom(), 20, 60);
        return false;
    }

    // Ore-style flowers are solid, so the bee must fly to the air block above them, not into them
    private boolean isSolidFlower(BlockPos pos) {
        return !bubbasBee.level.getBlockState(pos).getCollisionShape(bubbasBee.level, pos).isEmpty();
    }

    private Vec3 hoverTarget(BlockPos flower) {
        if (isSolidFlower(flower)) {
            return Vec3.atBottomCenterOf(flower.above()).add(0.0D, SOLID_HOVER_HEIGHT, 0.0D);
        }
        return Vec3.atBottomCenterOf(flower).add(0.0D, FLOWER_HOVER_HEIGHT, 0.0D);
    }

    private void navigateToPos(BlockPos pos) {
        Vec3 target = hoverTarget(pos);
        bubbasBee.getNavigation().moveTo(target.x, target.y, target.z, (double)1.2F);
    }

    private float randomOffset() {
        return (bubbasBee.getRandom().nextFloat() * 2.0F - 1.0F) * 0.33333334F;
    }

    private boolean isOverFlower() {
        return bubbasBee.savedFlowerPos != null
                && bubbasBee.distanceToSqr(
                        bubbasBee.savedFlowerPos.getX(),
                        bubbasBee.savedFlowerPos.getY(),
                        bubbasBee.savedFlowerPos.getZ()) <= 4.0;
    }

    private Optional<BlockPos> findFlowerExcludingHistory() {
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

    // Mirrors Bee.BeePollinateGoal.tick but hovers above solid flower blocks instead of inside them
    @Override
    public void tick() {
        BeePollinateGoalAccessor counters = (BeePollinateGoalAccessor) this;
        counters.setPollinatingTicks(counters.getPollinatingTicks() + 1);
        if (counters.getPollinatingTicks() > MAX_POLLINATING_TICKS) {
            bubbasBee.savedFlowerPos = null;
            return;
        }
        if (bubbasBee.savedFlowerPos == null) return;
        Vec3 target = hoverTarget(bubbasBee.savedFlowerPos);
        // A flying bee that touched down walks slowly; lift it back into the air
        if (bubbasBee.isOnGround()) {
            bubbasBee.setDeltaMovement(bubbasBee.getDeltaMovement().add(0.0D, 0.08D, 0.0D));
        }
        if (target.distanceTo(bubbasBee.position()) > 1.0D) {
            this.hoverPos = target;
            this.setWantedPos();
            return;
        }
        if (this.hoverPos == null) this.hoverPos = target;
        boolean arrived = bubbasBee.position().distanceTo(this.hoverPos) <= 0.1D;
        boolean updateWantedPos = true;
        if (arrived) {
            reachedFlower = true;
            lastPollinatedPos = bubbasBee.savedFlowerPos;
            if (bubbasBee.getRandom().nextInt(25) == 0) {
                this.hoverPos = new Vec3(target.x + randomOffset(), target.y, target.z + randomOffset());
                bubbasBee.getNavigation().stop();
            } else {
                updateWantedPos = false;
            }
            bubbasBee.getLookControl().setLookAt(target.x, target.y, target.z);
        }
        if (updateWantedPos) this.setWantedPos();
        counters.setSuccessfulPollinatingTicks(counters.getSuccessfulPollinatingTicks() + 1);
        if (bubbasBee.getRandom().nextFloat() < 0.05F
                && counters.getSuccessfulPollinatingTicks() > counters.getLastSoundPlayedTick() + 60) {
            counters.setLastSoundPlayedTick(counters.getSuccessfulPollinatingTicks());
            bubbasBee.playSound(SoundEvents.BEE_POLLINATE, 1.0F, 1.0F);
        }
    }

    @Override
    public boolean canBeeContinueToUse() {
        if (!this.isPollinating()) return false;
        if (!bubbasBee.hasSavedFlowerPos()) return false;
        if (bubbasBee.level.isRaining()) return false;
        if (this.hasPollinatedLongEnough()) {
            if (isOverFlower()) return true;
            return bubbasBee.level.getRandom().nextFloat() < 0.2F;
        }
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
                this.hoverPos.x(), this.hoverPos.y(), this.hoverPos.z(), 0.3499999940395355D);
    }

    @Override
    public void stop() {
        // Read before super.stop() resets the goal; only a completed pollination ages copper
        boolean pollinated = this.hasPollinatedLongEnough();
        BlockPos flower = lastPollinatedPos != null ? lastPollinatedPos : bubbasBee.savedFlowerPos;
        super.stop();

        exploring = true;
        exploreTimer = 20;
        rolledThisCycle = true;
        seekingNewFlower = false;
        reachedFlower = false;
        lastPollinatedPos = null;
        this.nextSearchTick = bubbasBee.tickCount;

        if (pollinated && bubbasBee instanceof CopperBee copperBee) {
            this.copperAging(copperBee, flower);
        }
    }

    private void copperAging(BubbasBee bee, BlockPos flower) {
        BlockPos blockpos = flower != null ? flower : bee.blockPosition().below(1);
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
