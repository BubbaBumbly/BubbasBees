package net.bubba.bubbasbees.mixin;

import net.bubba.bubbasbees.util.PinkPiston;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** The pink sticky piston uses the pink head and counts as a sticky piston (see PinkPiston). */
@Mixin(PistonBaseBlock.class)
public abstract class PinkPistonBaseMixin {

    // 1.20.1 still has moveBlocks; its three PISTON_HEAD reads place and update the head (triggerEvent has none).
    @Redirect(method = "moveBlocks", require = 3,
            at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/Blocks;PISTON_HEAD:Lnet/minecraft/world/level/block/Block;"))
    private Block bubbasBees$head() {
        return PinkPiston.headFor(this);
    }

    @Redirect(method = "triggerEvent", require = 2,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"))
    private boolean bubbasBees$isInTrigger(BlockState state, Block block) {
        return PinkPiston.is(state, block);
    }

    @Redirect(method = "isPushable", require = 2,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"))
    private static boolean bubbasBees$isInPushable(BlockState state, Block block) {
        return PinkPiston.is(state, block);
    }
}
