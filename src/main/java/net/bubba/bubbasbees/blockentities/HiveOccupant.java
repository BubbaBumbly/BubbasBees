package net.bubba.bubbasbees.blockentities;

import net.minecraft.resources.ResourceLocation;

/**
 * One bee inside a hive as seen by the client.
 * dueGameTime is the world game time at which the hive will try to release it; -1 when unknown (item tooltips).
 */
public record HiveOccupant(ResourceLocation id, long dueGameTime) {
    public static final long NO_TIMER = -1L;

    public HiveOccupant(ResourceLocation id) {
        this(id, NO_TIMER);
    }

    public boolean hasTimer() {
        return dueGameTime != NO_TIMER;
    }

    /** Ticks until release at the given game time; 0 when overdue (kept inside by night or rain). */
    public long remainingTicks(long gameTime) {
        return hasTimer() ? Math.max(0L, dueGameTime - gameTime) : 0L;
    }
}
