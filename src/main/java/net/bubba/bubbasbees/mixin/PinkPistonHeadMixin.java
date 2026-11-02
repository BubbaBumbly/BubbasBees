package net.bubba.bubbasbees.mixin;

import net.bubba.bubbasbees.util.PinkPiston;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** A head fits a pink sticky piston base like a sticky one (see PinkPiston). */
@Mixin(PistonHeadBlock.class)
public abstract class PinkPistonHeadMixin {
    @Redirect(method = "isFittingBase",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"))
    private boolean bubbasBees$isBase(BlockState state, Block block) {
        return PinkPiston.is(state, block);
    }
}
