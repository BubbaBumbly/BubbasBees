package net.bubba.bubbasbees.blockentities;

import net.minecraft.resources.ResourceLocation;

/**
 * One bee inside a hive as seen by the client.
 * dueGameTime is the world game time at which the hive will try to release it; -1 when unknown (item tooltips).
 */
public record HiveOccupant(ResourceLocation id, long dueGameTime) {
    public static final long NO_TIMER = -1L;
    /** The bee is past its stay and only waiting on night or rain to end; fixed so the sync does not churn. */
    public static final long READY = -2L;

    public HiveOccupant(ResourceLocation id) {
        this(id, NO_TIMER);
    }

    public boolean hasTimer() {
        return dueGameTime != NO_TIMER;
    }

    public boolean isReady() {
        return dueGameTime == READY;
    }

    /** Ticks until release at the given game time; 0 when ready (kept inside by night or rain). */
    public long remainingTicks(long gameTime) {
        if (!hasTimer() || isReady()) return 0L;
        return Math.max(0L, dueGameTime - gameTime);
    }
}
