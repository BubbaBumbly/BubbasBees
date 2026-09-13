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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
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
    private int lineOfSightCheckTick = Integer.MIN_VALUE;
    private boolean hasLineOfSight = false;
    private static final int MAX_POLLINATING_TICKS = 600;
    // Vanilla hover point: 0.6 above the flower's bottom; setWantedPos lifts the wanted position another 0.5
    private static final double HOVER_HEIGHT = 0.6D;
    private static final double WANTED_POS_LIFT = 0.5D;
    private static final double NEAR_DISTANCE = 1.0D;
    // Straight-line approach: cruise one block over the flower's top at full flying speed, then drop onto it
    private static final double CRUISE_HEIGHT = 1.0D;
    private static final double CRUISE_SPEED = 1.5D;
    private static final double DESCENT_SPEED = 0.7D;
    private static final double DESCENT_RADIUS = 0.75D;
    private static final int LINE_OF_SIGHT_INTERVAL = 10;
    // Pollination length varies per trip so bees sharing a hive do not enter and leave in lockstep
    private static final int BASE_POLLINATION_TICKS = 400;
    private static final int POLLINATION_VARIANCE = 25;
    // Positions still examined after the first flower is found before choosing among the hits
    private static final int EXTRA_SCAN_AFTER_FIRST_HIT = 15;
    // Chance to forget the flower after a successful trip so bees spread out over time
    private static final float FORGET_FLOWER_CHANCE = 0.25F;
    private int requiredPollinationTicks = BASE_POLLINATION_TICKS;
    // Small lift each time the hovering bee touches the block keeps a gentle bob (0.5 from 1.20.6 on, where
    // vanilla damps vertical velocity much harder)
    private static final double HOVER_BOB_LIFT = 0.05D;

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

    // Scan the usual spiral, but after the first flower keep looking for a few more positions and pick one of
    // the hits at random, so bees released together do not all converge on the same block
    @Override
    public Optional<BlockPos> findNearbyFlower() {
        BlockPos beePos = bubbasBee.blockPosition();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        double radius = 5.0D;
        List<BlockPos> candidates = new ArrayList<>();
        int positionsAfterFirstHit = 0;

        for (int i = 0; (double)i <= radius; i = i > 0 ? -i : 1 - i) {
            for (int j = 0; (double)j < radius; ++j) {
                for (int k = 0; k <= j; k = k > 0 ? -k : 1 - k) {
                    for (int l = k < j && k > -j ? j : 0; l <= j; l = l > 0 ? -l : 1 - l) {
                        mutablePos.setWithOffset(beePos, k, i - 1, l);
                        if (!candidates.isEmpty() && ++positionsAfterFirstHit > EXTRA_SCAN_AFTER_FIRST_HIT) {
                            return Optional.of(candidates.get(bubbasBee.getRandom().nextInt(candidates.size())));
                        }
                        if (beePos.closerThan(mutablePos, radius)
                                && this.VALID_POLLINATION_BLOCKS.test(bubbasBee.level.getBlockState(mutablePos))
                                && bubbasBee.level.getBlockState(mutablePos.above()).isAir()) {
                            candidates.add(mutablePos.immutable());
                        }
                    }
                }
            }
        }
        if (candidates.isEmpty()) return Optional.empty();
        return Optional.of(candidates.get(bubbasBee.getRandom().nextInt(candidates.size())));
    }

    @Override
    public boolean canBeeUse() {
        if (bubbasBee.tickCount < this.nextSearchTick) return false;
        if (bubbasBee.hasNectar()) return false;
        if (bubbasBee.level.isRaining() || bubbasBee.level.isNight()) return false;

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

    private boolean isSolidFlower(BlockPos pos) {
        return !bubbasBee.level.getBlockState(pos).getCollisionShape(bubbasBee.level, pos).isEmpty();
    }

    private Vec3 hoverTarget(BlockPos flower) {
        return Vec3.atBottomCenterOf(flower).add(0.0D, HOVER_HEIGHT, 0.0D);
    }

    private double flowerTopY(BlockPos flower) {
        return isSolidFlower(flower) ? flower.getY() + 1.0D : flower.getY();
    }

    // Path nodes for solid flowers must end in the air block above; a node inside the block is unreachable
    private void navigateToPos(BlockPos pos) {
        double y = isSolidFlower(pos) ? pos.getY() + 1.5D : pos.getY() + 0.5D;
        bubbasBee.getNavigation().moveTo((double)pos.getX() + 0.5D, y, (double)pos.getZ() + 0.5D, (double)1.2F);
    }

    private boolean canSeePoint(Vec3 point) {
        ClipContext context = new ClipContext(bubbasBee.getEyePosition(), point,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, bubbasBee);
        return bubbasBee.level.clip(context).getType() == HitResult.Type.MISS;
    }

    // Far from the flower: fly straight at it when nothing is in the way, since path nodes hug the terrain
    // and a bee following them touches the ground and drops to walking speed
    private void approach(BlockPos flower, Vec3 target) {
        double cruiseY = flowerTopY(flower) + CRUISE_HEIGHT;
        if (bubbasBee.tickCount - lineOfSightCheckTick >= LINE_OF_SIGHT_INTERVAL) {
            lineOfSightCheckTick = bubbasBee.tickCount;
            hasLineOfSight = canSeePoint(new Vec3(target.x, cruiseY + 0.3D, target.z));
        }
        if (hasLineOfSight) {
            bubbasBee.getNavigation().stop();
            double dx = target.x - bubbasBee.getX();
            double dz = target.z - bubbasBee.getZ();
            if (Math.sqrt(dx * dx + dz * dz) > DESCENT_RADIUS) {
                bubbasBee.getMoveControl().setWantedPosition(target.x, cruiseY, target.z, CRUISE_SPEED);
            } else {
                bubbasBee.getMoveControl().setWantedPosition(target.x, target.y + WANTED_POS_LIFT, target.z, DESCENT_SPEED);
            }
        } else {
            if (bubbasBee.getNavigation().isDone()) navigateToPos(flower);
            this.setWantedPos();
        }
    }

    private float randomOffset() {
        return (bubbasBee.getRandom().nextFloat() * 2.0F - 1.0F) * 0.33333334F;
    }

    private Optional<BlockPos> findFlowerExcludingHistory() {
        return findNearbyFlower();
    }

    // Mirrors Bee.BeePollinateGoal.tick with a faster approach; hover point and speeds near the flower are vanilla
    @Override
    public void tick() {
        BeePollinateGoalAccessor counters = (BeePollinateGoalAccessor) this;
        counters.setPollinatingTicks(counters.getPollinatingTicks() + 1);
        if (counters.getPollinatingTicks() > MAX_POLLINATING_TICKS) {
            bubbasBee.savedFlowerPos = null;
            return;
        }
        BlockPos flower = bubbasBee.savedFlowerPos;
        if (flower == null) return;
        Vec3 target = hoverTarget(flower);
        if (target.distanceTo(bubbasBee.position()) > NEAR_DISTANCE) {
            // A flying bee that touched down walks slowly; lift it back into the air while approaching
            if (bubbasBee.isOnGround()) {
                bubbasBee.setDeltaMovement(bubbasBee.getDeltaMovement().add(0.0D, 0.08D, 0.0D));
            }
            this.hoverPos = target;
            approach(flower, target);
            return;
        }
        if (this.hoverPos == null) this.hoverPos = target;
        Vec3 wanted = this.hoverPos.add(0.0D, WANTED_POS_LIFT, 0.0D);
        // Hovering just above a solid block: a small lift each time the feet touch keeps a gentle bob
        if (bubbasBee.isOnGround()) {
            bubbasBee.setDeltaMovement(bubbasBee.getDeltaMovement().add(0.0D, HOVER_BOB_LIFT, 0.0D));
        }
        boolean arrived = bubbasBee.position().distanceTo(wanted) <= 0.1D;
        boolean updateWantedPos = true;
        if (arrived) {
            if (bubbasBee.getRandom().nextInt(25) == 0) {
                this.hoverPos = new Vec3(target.x + randomOffset(), target.y, target.z + randomOffset());
                bubbasBee.getNavigation().stop();
            } else {
                updateWantedPos = false;
            }
            bubbasBee.getLookControl().setLookAt(target.x, target.y, target.z);
        }
        if (updateWantedPos) this.setWantedPos();
        reachedFlower = true;
        lastPollinatedPos = flower;
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
    public void start() {
        super.start();
        requiredPollinationTicks = BASE_POLLINATION_TICKS
                + bubbasBee.getRandom().nextInt(POLLINATION_VARIANCE * 2 + 1) - POLLINATION_VARIANCE;
    }

    // Vanilla compares against a fixed 400; this run's rolled value decides when nectar is granted in stop()
    @Override
    public boolean hasPollinatedLongEnough() {
        return ((BeePollinateGoalAccessor) this).getSuccessfulPollinatingTicks() > requiredPollinationTicks;
    }

    @Override
    public void setWantedPos() {
        bubbasBee.getMoveControl().setWantedPosition(
                this.hoverPos.x(), this.hoverPos.y() + WANTED_POS_LIFT, this.hoverPos.z(), 0.3499999940395355D);
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
        hasLineOfSight = false;
        lineOfSightCheckTick = Integer.MIN_VALUE;
        this.nextSearchTick = bubbasBee.tickCount;

        if (pollinated && bubbasBee instanceof CopperBee copperBee) {
            this.copperAging(copperBee, flower);
        }
        if (pollinated && bubbasBee.getRandom().nextFloat() < FORGET_FLOWER_CHANCE) {
            bubbasBee.savedFlowerPos = null;
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
