package net.bubba.bubbasbees.entity.goals;

import net.bubba.bubbasbees.entity.custom.BubbasBee;
import net.bubba.bubbasbees.mixin.BeeEntityAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;

/** Keeps a bee within a radius of its home anchor (its hive, or where it was released). */
public class BeeAnchorHomingGoal extends Goal {

    private static final double HOME_RADIUS = 24.0;
    private static final double RELEASE_RADIUS = 5.0;
    private static final double LOST_RADIUS = 48.0;
    // Vanilla's go-to-hive goal handles a bee that wants in and is within this range of its hive
    private static final double VANILLA_HOME_RANGE = 32.0;
    private static final double DIRECT_PATH_RADIUS = 16.0;
    private static final int REPATH_INTERVAL = 20;
    private final BubbasBee bee;
    private int ticksRunning;

    public BeeAnchorHomingGoal(BubbasBee bee) {
        this.bee = bee;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (bee.isAngry()) return false;

        BlockPos anchor = bee.getHomeAnchorPos();

        if (anchor == null) {
            BlockPos hivePos = bee.getHivePos();
            bee.setHomeAnchorPos(hivePos != null ? hivePos : bee.blockPosition());
            return false;
        }

        if (bee.getHivePos() != null && !bee.getHivePos().equals(anchor)) {
            bee.setHomeAnchorPos(bee.getHivePos());
            anchor = bee.getHomeAnchorPos();
        }

        if (bee.getHivePos() == null && !bee.blockPosition().closerThan(anchor, LOST_RADIUS)) {
            bee.setHomeAnchorPos(null);
            return false;
        }

        if (yieldsToVanillaHoming()) return false;
        return !bee.blockPosition().closerThan(anchor, HOME_RADIUS);
    }

    @Override
    public boolean canContinueToUse() {
        if (bee.isAngry()) return false;
        BlockPos anchor = bee.getHomeAnchorPos();
        if (anchor == null || yieldsToVanillaHoming()) return false;
        return !bee.blockPosition().closerThan(anchor, RELEASE_RADIUS);
    }

    // A bee that wants to enter its hive must be left to vanilla's go-to-hive and enter-hive goals
    private boolean yieldsToVanillaHoming() {
        BlockPos hivePos = bee.getHivePos();
        return hivePos != null
                && ((BeeEntityAccessor) bee).callWantsToEnterHive()
                && bee.blockPosition().closerThan(hivePos, VANILLA_HOME_RANGE);
    }

    @Override
    public void start() {
        ticksRunning = 0;
        navigateTowardsAnchor();
    }

    @Override
    public void tick() {
        BlockPos anchor = bee.getHomeAnchorPos();
        if (anchor == null) return;

        if (bee.getHivePos() == null && !bee.blockPosition().closerThan(anchor, LOST_RADIUS)) {
            bee.setHomeAnchorPos(null);
            return;
        }

        if (bee.getHivePos() != null && !bee.getHivePos().equals(anchor)) {
            bee.setHomeAnchorPos(bee.getHivePos());
            navigateTowardsAnchor();
        } else if (bee.getNavigation().isDone() || ++ticksRunning % REPATH_INTERVAL == 0) {
            navigateTowardsAnchor();
        }
    }

    // The anchor is usually the hive block itself, so aim for the air above it; far away, or when no path
    // reaches it, take vanilla's random step towards the target instead of re-requesting a dead path each tick
    private void navigateTowardsAnchor() {
        BlockPos anchor = bee.getHomeAnchorPos();
        if (anchor == null) return;
        PathNavigation navigation = bee.getNavigation();
        if (bee.blockPosition().closerThan(anchor, DIRECT_PATH_RADIUS)) {
            BlockPos above = anchor.above();
            navigation.moveTo(above.getX() + 0.5D, above.getY(), above.getZ() + 0.5D, 1.2D);
            Path path = navigation.getPath();
            if (path != null && path.canReach()) return;
        }
        ((BeeEntityAccessor) bee).callPathfindRandomlyTowards(anchor);
    }

    @Override
    public void stop() {
        bee.getNavigation().stop();
        bee.getNavigation().resetMaxVisitedNodesMultiplier();
    }
}
