package net.bubba.bubbasbees.entity.custom;

import net.bubba.bubbasbees.bees.BeeSpec;
import net.bubba.bubbasbees.entity.goals.GuardProtectGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import org.jetbrains.annotations.NotNull;

/** Every roster bee: one class, told apart by its BeeSpec (see BeeSpecs). */
public class ModBee extends BubbasBee {
    private final BeeSpec spec;

    public ModBee(EntityType<? extends Bee> type, Level level, BeeSpec spec) {
        super(type, level, spec);
        this.spec = spec;
        // Goals run in Mob's constructor, before the spec is set, so trait goals are fitted here
        if (!spec.has(BeeSpec.Trait.GREEN_THUMB)) {
            // Every bee inherits vanilla's crop growing; only green-thumb bees keep it
            this.goalSelector.getAvailableGoals().removeIf(
                    goal -> goal.getGoal().getClass().getName().endsWith("$BeeGrowCropGoal"));
        }
        if (spec.fireImmune()) {
            // Vanilla bees treat any cell next to fire, lava or magma as impassable (DANGER_FIRE -1): a magma bee on
            // its magma flower, or a Nether bee beside lava, could never path home and dropped its hive (step 6 run).
            this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, 0.0F);
            this.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, 0.0F);
        }
        if (spec.has(BeeSpec.Trait.ROYAL_GUARD)) {
            this.targetSelector.addGoal(1, new GuardProtectGoal(this));
        }
    }

    @Override
    public @NotNull BeeSpec getSpec() {
        return spec;
    }
}
