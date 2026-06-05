package net.bubba.bubbasbees.entity.goals;

import net.bubba.bubbasbees.entity.custom.BubbasBee;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class BeeAnchorHomingGoal extends Goal {

    private static final double HOME_RADIUS = 24.0;
    private static final double RELEASE_RADIUS = 5.0;
    private static final double LOST_RADIUS = 48.0;
    private final BubbasBee bee;

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

        return !bee.blockPosition().closerThan(anchor, HOME_RADIUS);
    }

    @Override
    public boolean canContinueToUse() {
        if (bee.isAngry()) return false;
        BlockPos anchor = bee.getHomeAnchorPos();
        return anchor != null && !bee.blockPosition().closerThan(anchor, RELEASE_RADIUS);
    }

    @Override
    public void start() {
        BlockPos anchor = bee.getHomeAnchorPos();
        if (anchor != null) {
            bee.getNavigation().moveTo(
                    anchor.getX() + 0.5D,
                    anchor.getY(),
                    anchor.getZ() + 0.5D,
                    1.2D);
        }
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
            anchor = bee.getHomeAnchorPos();
            bee.getNavigation().moveTo(
                    anchor.getX() + 0.5D,
                    anchor.getY(),
                    anchor.getZ() + 0.5D,
                    1.2D);
        } else if (bee.getNavigation().isDone()) {
            bee.getNavigation().moveTo(
                    anchor.getX() + 0.5D,
                    anchor.getY(),
                    anchor.getZ() + 0.5D,
                    1.2D);
        }
    }

    @Override
    public void stop() {
        bee.getNavigation().stop();
    }
}
