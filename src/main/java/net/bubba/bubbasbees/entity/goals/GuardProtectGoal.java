package net.bubba.bubbasbees.entity.goals;

import net.bubba.bubbasbees.entity.custom.BubbasBee;
import net.bubba.bubbasbees.entity.custom.DarkBee;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

import java.util.Comparator;

/**
 * Royal guard: goes after dark bees on sight and after hostile mobs that are going for a player or a bee, within
 * 8 blocks. Setting the target makes the bee angry (NeutralMob), which runs its sting attack.
 */
public class GuardProtectGoal extends TargetGoal {
    private static final double RANGE = 8.0D;
    private final BubbasBee guard;
    private LivingEntity threat;

    public GuardProtectGoal(BubbasBee guard) {
        super(guard, true);
        this.guard = guard;
    }

    @Override
    public boolean canUse() {
        if ((guard.tickCount + guard.getId()) % 10 != 0) return false;
        threat = guard.level.getEntitiesOfClass(Mob.class, guard.getBoundingBox().inflate(RANGE), GuardProtectGoal::isThreat)
                .stream().min(Comparator.comparingDouble(guard::distanceToSqr)).orElse(null);
        return threat != null;
    }

    private static boolean isThreat(Mob mob) {
        if (!mob.isAlive()) return false;
        if (mob instanceof DarkBee) return true;
        return mob instanceof Enemy && (mob.getTarget() instanceof Player || mob.getTarget() instanceof Bee);
    }

    @Override
    public void start() {
        guard.setTarget(threat);
        super.start();
    }
}
