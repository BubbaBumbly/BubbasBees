package net.bubba.bubbasbees.entity.goals;

import net.bubba.bubbasbees.entity.custom.BubbasBee;
import net.bubba.bubbasbees.mixin.BeeEntityAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.entity.ai.util.HoverRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Short, slow hops with no pause between them while a calm bee dawdles after leaving its hive. */
public class BeeDaftWanderGoal extends Goal {

    private static final int HOP_DISTANCE = 6;
    private static final int HOP_HEIGHT = 4;
    // Half of vanilla's wander speed, about 1.3 blocks/s
    private static final double SPEED = 0.5D;
    // As in vanilla's wander goal, hops lean back towards the hive beyond this distance
    private static final int WANDER_THRESHOLD = 22;
    private final BubbasBee bee;

    public BeeDaftWanderGoal(BubbasBee bee) {
        this.bee = bee;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    private boolean isDawdling() {
        return ((BeeEntityAccessor) bee).getBeePollinateGoal() instanceof ModBeePollinateGoal goal && goal.isDawdling();
    }

    @Override
    public boolean canUse() {
        return isDawdling();
    }

    @Override
    public boolean canContinueToUse() {
        return isDawdling();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        hop();
    }

    @Override
    public void tick() {
        if (bee.getNavigation().isDone()) {
            hop();
        }
    }

    private void hop() {
        BlockPos hive = bee.getHivePos();
        Vec3 direction = hive != null && !hive.closerThan(bee.blockPosition(), WANDER_THRESHOLD)
                ? Vec3.atCenterOf(hive).subtract(bee.position()).normalize()
                : bee.getViewVector(0.0F);
        Vec3 target = HoverRandomPos.getPos(bee, HOP_DISTANCE, HOP_HEIGHT, direction.x, direction.z,
                (float) Math.PI / 2F, 3, 1);
        if (target == null) {
            target = AirAndWaterRandomPos.getPos(bee, HOP_DISTANCE, HOP_HEIGHT, -2, direction.x, direction.z,
                    Math.PI / 2D);
        }
        if (target != null) {
            PathNavigation navigation = bee.getNavigation();
            navigation.moveTo(navigation.createPath(new BlockPos(target), 1), SPEED);
        }
    }

    @Override
    public void stop() {
        bee.getNavigation().stop();
    }
}
