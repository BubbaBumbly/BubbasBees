package net.bubba.bubbasbees.entity.goals;

import net.bubba.bubbasbees.blockentities.AdvancedBeehiveBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.block.entity.BlockEntity;


public class ModBeeEnterHiveGoal extends Goal {

    private final Bee bee;

    public ModBeeEnterHiveGoal(Bee bee) {
        this.bee = bee;
    }

    @Override
    public boolean canUse() {
        if (this.bee.isAngry()) {
            return false;
        }
        BlockPos pos = this.bee.getHivePos();
        if (pos != null && pos.closerThan(this.bee.blockPosition(), 2.0D)) {
            BlockEntity block = this.bee.level.getBlockEntity(pos);
            if (block instanceof AdvancedBeehiveBlockEntity hive) {
                if (!hive.isFull()) {
                    return true;
                }
            }
        }

        return false;
    }
}