package net.bubba.bubbasbees.mixin;

import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** BeeData wraps an immutable Occupant (entity NBT, minimum stay) plus the live ticksInHive counter. */
@Mixin(BeehiveBlockEntity.BeeData.class)
public interface BeehiveBeeDataAccessor {
    @Accessor("occupant")
    BeehiveBlockEntity.Occupant getOccupant();

    @Accessor("ticksInHive")
    int getTicksInHive();

    @Accessor("ticksInHive")
    void setTicksInHive(int ticksInHive);
}
