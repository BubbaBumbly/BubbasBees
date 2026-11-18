package net.bubba.bubbasbees.mixin;

import net.bubba.bubbasbees.registries.ModBlocks;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Royal jelly sticks to pistons like slime, but not to slime or honey blocks. */
@Mixin(PistonStructureResolver.class)
public abstract class PistonStructureResolverMixin {

    // FIX: the NeoForge isStickyBlock override is applied here; royal jelly counts as a sticky block.
    @Inject(method = "isSticky", at = @At("HEAD"), cancellable = true)
    private static void bubbasBees$isSticky(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (state.is(ModBlocks.ROYAL_JELLY_BLOCK.get())) cir.setReturnValue(true);
    }

    // FIX: the NeoForge canStickTo override is applied here; jelly does not stick to slime or honey.
    @Inject(method = "canStickToEachOther", at = @At("HEAD"), cancellable = true)
    private static void bubbasBees$canStickToEachOther(BlockState state, BlockState other, CallbackInfoReturnable<Boolean> cir) {
        boolean jelly = isJelly(state);
        boolean otherJelly = isJelly(other);
        if ((jelly && (other.is(Blocks.SLIME_BLOCK) || other.is(Blocks.HONEY_BLOCK)))
                || (otherJelly && (state.is(Blocks.SLIME_BLOCK) || state.is(Blocks.HONEY_BLOCK)))) {
            cir.setReturnValue(false);
        }
    }

    private static boolean isJelly(BlockState state) {
        return state.is(ModBlocks.ROYAL_JELLY_BLOCK.get());
    }
}
