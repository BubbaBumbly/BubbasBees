package net.bubba.bubbasbees.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Bee;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Bee.class)
public interface BeeEntityAccessor {

    @Accessor
    void setHivePos(BlockPos pos);

    @Accessor
    int getStayOutOfHiveCountdown();

}