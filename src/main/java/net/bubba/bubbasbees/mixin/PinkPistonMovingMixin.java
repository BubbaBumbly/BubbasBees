package net.bubba.bubbasbees.mixin;

import net.bubba.bubbasbees.util.PinkPiston;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** A retracting pink piston's head collides as the pink sticky head (see PinkPiston). */
@Mixin(PistonMovingBlockEntity.class)
public abstract class PinkPistonMovingMixin {
    @Shadow private BlockState movedState;

    @Redirect(method = "getCollisionRelatedBlockState",
            at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/Blocks;PISTON_HEAD:Lnet/minecraft/world/level/block/Block;"))
    private Block bubbasBees$head() {
        return PinkPiston.headFor(this.movedState);
    }

    @Redirect(method = "getCollisionRelatedBlockState",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"))
    private boolean bubbasBees$isSticky(BlockState state, Block block) {
        return PinkPiston.is(state, block);
    }
}
