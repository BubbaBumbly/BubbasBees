package net.bubba.bubbasbees.entity.goals;

import net.bubba.bubbasbees.entity.custom.BubbasBee;
import net.bubba.bubbasbees.mixin.BeePollinateGoalAccessor;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class ModBeePollinateGoal extends Bee.BeePollinateGoal {

    private final BubbasBee bubbasBee;
    private int nextSearchTick = 0;
    private boolean reachedFlower = false;
    private BlockPos lastPollinatedPos = null;
    private int lineOfSightCheckTick = Integer.MIN_VALUE;
    private boolean hasLineOfSight = false;
    // How a calm bee starts its outing, rolled once when it comes out of the hive (the goal is recreated with
    // the bee each time): dawdle a while (25%), head for the remembered flower (35%), or look around briefly
    // (40%). Daft and Quick end in a full flower scan even when a remembered flower is close
    public enum Departure { DAFT, REMEMBERS, QUICK }
    private static final float CALM_DAFT_CHANCE = 0.25F;
    private static final float CALM_REMEMBERS_CHANCE = 0.35F;
    private static final int DAFT_WANDER_MIN_TICKS = 100;
    private static final int DAFT_WANDER_MAX_TICKS = 240;
    private static final int QUICK_WANDER_MIN_TICKS = 20;
    private static final int QUICK_WANDER_MAX_TICKS = 40;
    private boolean departureDecided = false;
    private Departure departure = null;
    private int wanderUntilTick = 0;
    private boolean scanAfterWander = false;
    // Busy bees head for their remembered flower, but this often scan around it and switch to a neighbour
    private static final float BUSY_RESCAN_CHANCE = 0.4F;
    // Before that, busy bees sometimes look for work right outside the hive instead, so they keep close to home.
    // Any flower in the area is equally likely, which keeps bees doing this from landing on the same one
    private static final float BUSY_HOME_SEARCH_CHANCE = 0.2F;
    private static final double HOME_SEARCH_RADIUS = 6.0D;
    // After a failed trip the bee waits this long before scanning for a new flower
    private static final int RETRY_SCAN_DELAY = 40;
    private int scanBlockedUntilTick = 0;
    // Flowers farther than this from the bee's home are ignored: the homing goal pulls the bee back at 24 blocks,
    // so a farther flower could never be reached and the bee would shuttle between it and the hive
    private static final double MAX_FLOWER_DISTANCE_FROM_HOME = 22.0D;
    // Give up after this many consecutive ticks away from the flower; every counted pollination tick resets it,
    // so approach time never eats into a pollination that has begun. Far flowers get their expected flight time
    // on top for the first approach (slowest straight-line speeds, in blocks per tick)
    private static final int MAX_TICKS_WITHOUT_POLLINATING = 300;
    private static final double CALM_BLOCKS_PER_TICK = 0.04D;
    private static final double BUSY_BLOCKS_PER_TICK = 0.1D;
    private int timeoutTicks = MAX_TICKS_WITHOUT_POLLINATING;
    private int ticksWithoutPollinating = 0;
    private boolean timedOut = false;
    // Hovering drives the move control directly, so the bee's pathfinder is paused only then (see BubbasBee)
    private boolean hoveringAtFlower = false;
    private int nextRepathTick = 0;
    // Vanilla hover point: 0.6 above the flower's bottom; setWantedPos lifts the wanted position another 0.5
    private static final double HOVER_HEIGHT = 0.6D;
    private static final double WANTED_POS_LIFT = 0.5D;
    private static final double NEAR_DISTANCE = 1.0D;
    // Straight-line approach: cruise one block over the flower's top, then drop onto it. Busy bees fly at about
    // two-thirds of full flying speed (air control saturates from ~1.2 up); calm bees drift at vanilla's hover speed
    private static final double CRUISE_HEIGHT = 1.0D;
    private static final double BUSY_CRUISE_SPEED = 0.8D;
    private static final double BUSY_DESCENT_SPEED = 0.7D;
    private static final double VANILLA_HOVER_SPEED = 0.3499999940395355D;
    // Pathfinder speed when a wall blocks the straight line; calm uses vanilla's usual travel speed
    private static final double BUSY_PATH_SPEED = 0.8D;
    private static final double CALM_PATH_SPEED = 1.0D;
    // Calm bees only use flowers within vanilla's search radius and wait vanilla's cooldown after a trip;
    // vanilla's go-to-known-flower goal still brings them back to a remembered flower farther away
    private static final double FLOWER_SEARCH_RADIUS = 5.0D;
    private static final int CALM_SEARCH_COOLDOWN = 200;
    private static final double DESCENT_RADIUS = 0.75D;
    private static final int LINE_OF_SIGHT_INTERVAL = 10;
    // Pollination length varies per trip so bees sharing a hive do not enter and leave in lockstep
    private static final int BASE_POLLINATION_TICKS = 400;
    private static final int POLLINATION_VARIANCE = 25;
    // Chance to forget the flower after a successful trip so bees spread out over time
    private static final float FORGET_FLOWER_CHANCE = 0.25F;
    private int requiredPollinationTicks = BASE_POLLINATION_TICKS;
    // Small lift each time the hovering bee touches the block (~0.4-block bob); same value as 1.20.6+ because tick()
    // emulates the newer vertical damping
    private static final double HOVER_BOB_LIFT = 0.13D;
    // Vanilla before 1.20.5 damps every mob's vertical velocity by 0.98/tick. 1.20.5+ damps a flying animal's like its
    // horizontal velocity: 0.91 in the air, block friction (0.6 for most blocks) x 0.91 on the ground. With 0.98 every
    // lift and climb below coasts ~5x farther, so the goal re-damps to the newer values and keeps the 1.21.1 tuning
    private static final double OLD_VERTICAL_DAMPING = 0.98D;
    private static final double AIR_DAMPING = 0.91D;
    private static final double GROUND_DAMPING = 0.6D * 0.91D;
    private boolean wasOnGround = false;
    // Approach: keep this much air under the bee so it does not scrape the ground and slow to walking speed.
    // Full clearance beyond CLEARANCE_FADE_START blocks (horizontal) from the flower, none within CLEARANCE_FADE_END
    private static final double APPROACH_GROUND_CLEARANCE = 0.5D;
    private static final double CLEARANCE_FADE_START = 2.5D;
    private static final double CLEARANCE_FADE_END = DESCENT_RADIUS;
    private static final double CLEARANCE_MAX_CLIMB = 0.1D;
    // Approach look-ahead for walls the eye-level line-of-sight ray passes over but the body does not
    private static final double OBSTACLE_LOOKAHEAD = 1.5D;
    private static final int MAX_OBSTACLE_CLIMB = 4;
    private static final double OBSTACLE_CLEARANCE = 0.3D;
    private static final double OBSTACLE_CLIMB_SPEED = 0.15D;
    private double obstacleClimbY = Double.NaN;

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
            return bubbasBee.isFlowerState(state);
        };
    }

    @Override
    public Optional<BlockPos> findNearbyFlower() {
        return findFlowerNear(bubbasBee.blockPosition(), null, FLOWER_SEARCH_RADIUS, true);
    }

    // Busy bees' home search: the area in front of the hive's entrance
    private Optional<BlockPos> findFlowerNearHive() {
        BlockPos hive = bubbasBee.getHivePos();
        if (hive == null || !bubbasBee.level.isLoaded(hive)) return Optional.empty();
        BlockState state = bubbasBee.level.getBlockState(hive);
        BlockPos front = state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? hive.relative(state.getValue(BlockStateProperties.HORIZONTAL_FACING)) : hive;
        return findFlowerNear(front, null, HOME_SEARCH_RADIUS, false);
    }

    // Scans the whole sphere around center and picks one of every flower found (nearer ones more likely when
    // favourNear), so bees spread over a patch instead of all taking its closest block
    private Optional<BlockPos> findFlowerNear(BlockPos center, BlockPos exclude, double radius, boolean favourNear) {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        int reach = Mth.ceil(radius) - 1;
        List<BlockPos> candidates = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        double totalWeight = 0.0D;
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dy = -reach; dy <= reach; dy++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    mutablePos.setWithOffset(center, dx, dy, dz);
                    if (!center.closerThan(mutablePos, radius) || mutablePos.equals(exclude)
                            || !this.VALID_POLLINATION_BLOCKS.test(bubbasBee.level.getBlockState(mutablePos))
                            || !bubbasBee.level.getBlockState(mutablePos.above()).isAir()
                            || !isWithinHomeRange(mutablePos)) {
                        continue;
                    }
                    double weight = favourNear ? 1.0D / (1.0D + Math.sqrt(dx * dx + dy * dy + dz * dz)) : 1.0D;
                    candidates.add(mutablePos.immutable());
                    weights.add(weight);
                    totalWeight += weight;
                }
            }
        }
        if (candidates.isEmpty()) return Optional.empty();
        double pick = bubbasBee.getRandom().nextDouble() * totalWeight;
        for (int i = 0; i < candidates.size(); i++) {
            pick -= weights.get(i);
            if (pick < 0.0D) return Optional.of(candidates.get(i));
        }
        return Optional.of(candidates.get(candidates.size() - 1));
    }

    private boolean isWithinHomeRange(BlockPos pos) {
        BlockPos home = bubbasBee.getHomeAnchorPos();
        return home == null || pos.closerThan(home, MAX_FLOWER_DISTANCE_FROM_HOME);
    }

    // Remembered flowers past the home range are forgotten, so vanilla's go-to-known-flower goal drops them too
    private boolean isUsableSavedFlower() {
        BlockPos saved = bubbasBee.savedFlowerPos;
        if (saved == null) return false;
        if (!isWithinHomeRange(saved)) {
            bubbasBee.savedFlowerPos = null;
            return false;
        }
        return bubbasBee.isFlowerValid(saved) && bubbasBee.level.getBlockState(saved.above()).isAir();
    }

    // Returns true when the bee heads straight for its remembered flower; otherwise it wanders first
    private boolean startCalmDeparture() {
        float roll = bubbasBee.getRandom().nextFloat();
        if (roll >= CALM_DAFT_CHANCE && roll < CALM_DAFT_CHANCE + CALM_REMEMBERS_CHANCE) {
            departure = Departure.REMEMBERS;
            if (isUsableSavedFlower()) {
                navigateToPos(bubbasBee.savedFlowerPos);
                return true;
            }
        }
        // Remembers without a usable flower falls back to Quick
        departure = roll < CALM_DAFT_CHANCE ? Departure.DAFT : Departure.QUICK;
        wanderUntilTick = bubbasBee.tickCount + (departure == Departure.DAFT
                ? Mth.nextInt(bubbasBee.getRandom(), DAFT_WANDER_MIN_TICKS, DAFT_WANDER_MAX_TICKS)
                : Mth.nextInt(bubbasBee.getRandom(), QUICK_WANDER_MIN_TICKS, QUICK_WANDER_MAX_TICKS));
        scanAfterWander = true;
        return false;
    }

    /** True while a calm bee that just left its hive is still wandering instead of working (see BeeDaftWanderGoal). */
    public boolean isDawdling() {
        return bubbasBee.tickCount < wanderUntilTick && !bubbasBee.hasNectar();
    }

    public Departure getDeparture() {
        return departure;
    }

    @Override
    public boolean canBeeUse() {
        if (bubbasBee.tickCount < this.nextSearchTick) return false;
        if (bubbasBee.hasNectar()) return false;
        if (bubbasBee.level.isRaining() || bubbasBee.level.isNight()) return false;

        boolean busy = bubbasBee.isBusy();
        if (!departureDecided) {
            departureDecided = true;
            if (!busy && startCalmDeparture()) return true;
        }
        if (bubbasBee.tickCount < wanderUntilTick) return false;

        if (busy && bubbasBee.getRandom().nextFloat() < BUSY_HOME_SEARCH_CHANCE) {
            Optional<BlockPos> homeFlower = findFlowerNearHive();
            if (homeFlower.isPresent()) {
                bubbasBee.savedFlowerPos = homeFlower.get();
                navigateToPos(bubbasBee.savedFlowerPos);
                return true;
            }
        }

        if (!scanAfterWander && isUsableSavedFlower()
                && (busy || bubbasBee.savedFlowerPos.closerThan(bubbasBee.blockPosition(), FLOWER_SEARCH_RADIUS))) {
            if (busy && bubbasBee.getRandom().nextFloat() < BUSY_RESCAN_CHANCE) {
                BlockPos remembered = bubbasBee.savedFlowerPos;
                findFlowerNear(remembered, remembered, FLOWER_SEARCH_RADIUS, true)
                        .ifPresent(flower -> bubbasBee.savedFlowerPos = flower);
            }
            navigateToPos(bubbasBee.savedFlowerPos);
            return true;
        }

        if (bubbasBee.tickCount < scanBlockedUntilTick) return false;
        scanAfterWander = false;
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
        double speed = bubbasBee.isBusy() ? BUSY_PATH_SPEED : CALM_PATH_SPEED;
        bubbasBee.getNavigation().moveTo((double)pos.getX() + 0.5D, y, (double)pos.getZ() + 0.5D, speed);
    }

    private boolean canSeePoint(Vec3 point) {
        ClipContext context = new ClipContext(bubbasBee.getEyePosition(), point,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, bubbasBee);
        return bubbasBee.level.clip(context).getType() == HitResult.Type.MISS;
    }

    // Lifts the bee whenever the surface straight below is closer than the (faded) clearance. Sets a minimum upward
    // speed instead of adding to it, so repeated ticks cannot stack into a launch
    private void keepGroundClearance(Vec3 target) {
        double dx = target.x - bubbasBee.getX();
        double dz = target.z - bubbasBee.getZ();
        double fade = Mth.clamp((Math.sqrt(dx * dx + dz * dz) - CLEARANCE_FADE_END)
                / (CLEARANCE_FADE_START - CLEARANCE_FADE_END), 0.0D, 1.0D);
        double wantedClearance = APPROACH_GROUND_CLEARANCE * fade;
        if (wantedClearance <= 0.0D) return;
        Vec3 feet = bubbasBee.position();
        HitResult below = bubbasBee.level.clip(new ClipContext(feet.add(0.0D, 0.25D, 0.0D),
                feet.subtract(0.0D, wantedClearance, 0.0D), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, bubbasBee));
        if (below.getType() == HitResult.Type.MISS) return;
        liftToward(wantedClearance - (feet.y - below.getLocation().y), CLEARANCE_MAX_CLIMB);
    }

    // Raises the upward speed to at least half the remaining height (capped); returns whether it had to
    private boolean liftToward(double deficit, double maxClimb) {
        if (deficit <= 0.0D) return false;
        Vec3 motion = bubbasBee.getDeltaMovement();
        double climb = Math.min(maxClimb, deficit * 0.5D);
        if (motion.y >= climb) return false;
        bubbasBee.setDeltaMovement(motion.x, climb, motion.z);
        return true;
    }

    // Casts two short rays toward the flower at foot and head height. On a hit, climbs to the lowest free block
    // above the blocking column (up to MAX_OBSTACLE_CLIMB); anything taller is left to the pathfinder
    private void checkObstacleAhead(BlockPos flower, double dx, double dz, double horizontalDistance) {
        if (horizontalDistance <= OBSTACLE_LOOKAHEAD) {
            obstacleClimbY = Double.NaN;
            return;
        }
        Vec3 ahead = new Vec3(dx / horizontalDistance * OBSTACLE_LOOKAHEAD, 0.0D, dz / horizontalDistance * OBSTACLE_LOOKAHEAD);
        BlockHitResult hit = null;
        for (double height : new double[]{0.1D, bubbasBee.getBbHeight() - 0.1D}) {
            Vec3 from = bubbasBee.position().add(0.0D, height, 0.0D);
            BlockHitResult ray = bubbasBee.level.clip(new ClipContext(from, from.add(ahead),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, bubbasBee));
            if (ray.getType() == HitResult.Type.BLOCK && !ray.getBlockPos().equals(flower)) {
                hit = ray;
                break;
            }
        }
        if (hit == null) {
            obstacleClimbY = Double.NaN;
            return;
        }
        for (int up = 1; up <= MAX_OBSTACLE_CLIMB; up++) {
            BlockPos above = hit.getBlockPos().above(up);
            if (bubbasBee.level.getBlockState(above).getCollisionShape(bubbasBee.level, above).isEmpty()) {
                obstacleClimbY = above.getY() + OBSTACLE_CLEARANCE;
                return;
            }
        }
        hasLineOfSight = false;
        obstacleClimbY = Double.NaN;
    }

    // Far from the flower: fly straight at it when nothing is in the way, since path nodes hug the terrain
    // and a bee following them touches the ground and drops to walking speed
    private void approach(BlockPos flower, Vec3 target) {
        keepGroundClearance(target);
        double cruiseY = flowerTopY(flower) + CRUISE_HEIGHT;
        double dx = target.x - bubbasBee.getX();
        double dz = target.z - bubbasBee.getZ();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        if (bubbasBee.tickCount - lineOfSightCheckTick >= LINE_OF_SIGHT_INTERVAL) {
            lineOfSightCheckTick = bubbasBee.tickCount;
            hasLineOfSight = canSeePoint(new Vec3(target.x, cruiseY + 0.3D, target.z));
            // The line-of-sight ray leaves from the eyes, so a low wall can pass under it and still stop the body
            if (hasLineOfSight) checkObstacleAhead(flower, dx, dz, horizontalDistance);
        } else if (hasLineOfSight && bubbasBee.horizontalCollision && Double.isNaN(obstacleClimbY)) {
            checkObstacleAhead(flower, dx, dz, horizontalDistance);
        }
        if (hasLineOfSight) {
            bubbasBee.getNavigation().stop();
            if (horizontalDistance > DESCENT_RADIUS) {
                double flyY = cruiseY;
                if (!Double.isNaN(obstacleClimbY)) {
                    flyY = Math.max(cruiseY, obstacleClimbY);
                    liftToward(obstacleClimbY - bubbasBee.getY(), OBSTACLE_CLIMB_SPEED);
                }
                bubbasBee.getMoveControl().setWantedPosition(target.x, flyY, target.z,
                        bubbasBee.isBusy() ? BUSY_CRUISE_SPEED : VANILLA_HOVER_SPEED);
            } else {
                obstacleClimbY = Double.NaN;
                bubbasBee.getMoveControl().setWantedPosition(target.x, target.y + WANTED_POS_LIFT, target.z,
                        bubbasBee.isBusy() ? BUSY_DESCENT_SPEED : VANILLA_HOVER_SPEED);
            }
        } else {
            obstacleClimbY = Double.NaN;
            // The pathfinder overrides this wanted position while it has a path; an unreachable flower would
            // otherwise trigger a fresh path search every tick
            if (bubbasBee.getNavigation().isDone() && bubbasBee.tickCount >= nextRepathTick) {
                nextRepathTick = bubbasBee.tickCount + LINE_OF_SIGHT_INTERVAL;
                navigateToPos(flower);
            }
            this.setWantedPos();
        }
    }

    private float randomOffset() {
        return (bubbasBee.getRandom().nextFloat() * 2.0F - 1.0F) * 0.33333334F;
    }

    // Mirrors Bee.BeePollinateGoal.tick with a pathfinding approach (faster for busy bees); hover point and speeds
    // near the flower are vanilla
    @Override
    public void tick() {
        BeePollinateGoalAccessor counters = (BeePollinateGoalAccessor) this;
        hoveringAtFlower = false;
        // Last tick's travel damped vertical velocity by 0.98; swap in the 1.20.5+ factor for that tick's ground state
        Vec3 motion = bubbasBee.getDeltaMovement();
        double damping = wasOnGround ? GROUND_DAMPING : AIR_DAMPING;
        bubbasBee.setDeltaMovement(motion.x, motion.y * damping / OLD_VERTICAL_DAMPING, motion.z);
        wasOnGround = bubbasBee.isOnGround();
        counters.setPollinatingTicks(counters.getPollinatingTicks() + 1);
        if (++ticksWithoutPollinating > timeoutTicks) {
            timedOut = true;
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
        hoveringAtFlower = true;
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
        ticksWithoutPollinating = 0;
        timeoutTicks = MAX_TICKS_WITHOUT_POLLINATING;
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
        ticksWithoutPollinating = 0;
        timedOut = false;
        hoveringAtFlower = false;
        nextRepathTick = 0;
        wasOnGround = false;
        obstacleClimbY = Double.NaN;
        BlockPos flower = bubbasBee.savedFlowerPos;
        double flightDistance = flower != null ? hoverTarget(flower).distanceTo(bubbasBee.position()) : 0.0D;
        timeoutTicks = MAX_TICKS_WITHOUT_POLLINATING + Mth.ceil(flightDistance
                / (bubbasBee.isBusy() ? BUSY_BLOCKS_PER_TICK : CALM_BLOCKS_PER_TICK));
        requiredPollinationTicks = BASE_POLLINATION_TICKS
                + bubbasBee.getRandom().nextInt(POLLINATION_VARIANCE * 2 + 1) - POLLINATION_VARIANCE;
    }

    // Vanilla compares against a fixed 400; this run's rolled value decides when nectar is granted in stop()
    @Override
    public boolean hasPollinatedLongEnough() {
        return ((BeePollinateGoalAccessor) this).getSuccessfulPollinatingTicks() > requiredPollinationTicks;
    }

    public boolean isHoveringAtFlower() {
        return this.isPollinating() && hoveringAtFlower;
    }

    @Override
    public void setWantedPos() {
        bubbasBee.getMoveControl().setWantedPosition(
                this.hoverPos.x(), this.hoverPos.y() + WANTED_POS_LIFT, this.hoverPos.z(), VANILLA_HOVER_SPEED);
    }

    @Override
    public void stop() {
        // Read before super.stop() resets the goal; only a completed pollination ages copper
        boolean pollinated = this.hasPollinatedLongEnough();
        BlockPos flower = lastPollinatedPos != null ? lastPollinatedPos : bubbasBee.savedFlowerPos;
        super.stop();

        scanBlockedUntilTick = bubbasBee.tickCount + RETRY_SCAN_DELAY;
        reachedFlower = false;
        lastPollinatedPos = null;
        hasLineOfSight = false;
        lineOfSightCheckTick = Integer.MIN_VALUE;
        obstacleClimbY = Double.NaN;
        hoveringAtFlower = false;
        this.nextSearchTick = bubbasBee.tickCount + (bubbasBee.isBusy() ? 0 : CALM_SEARCH_COOLDOWN);

        // The copper bee ages the copper it pollinates
        if (pollinated && "copper".equals(bubbasBee.getVariant())) {
            this.copperAging(bubbasBee, flower);
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
