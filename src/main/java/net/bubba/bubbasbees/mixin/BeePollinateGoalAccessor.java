package net.bubba.bubbasbees.mixin;

import net.minecraft.world.entity.animal.Bee;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes the private pollination counters so ModBeePollinateGoal can drive them from its own tick. */
@Mixin(Bee.BeePollinateGoal.class)
public interface BeePollinateGoalAccessor {
    @Accessor("successfulPollinatingTicks")
    int getSuccessfulPollinatingTicks();

    @Accessor("successfulPollinatingTicks")
    void setSuccessfulPollinatingTicks(int ticks);

    @Accessor("pollinatingTicks")
    int getPollinatingTicks();

    @Accessor("pollinatingTicks")
    void setPollinatingTicks(int ticks);

    @Accessor("lastSoundPlayedTick")
    int getLastSoundPlayedTick();

    @Accessor("lastSoundPlayedTick")
    void setLastSoundPlayedTick(int tick);
}
