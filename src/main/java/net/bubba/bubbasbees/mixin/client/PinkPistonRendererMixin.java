package net.bubba.bubbasbees.mixin.client;

import net.bubba.bubbasbees.util.PinkPiston;
import net.minecraft.client.renderer.blockentity.PistonHeadRenderer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Draws the pink head while a pink sticky piston extends or retracts (vanilla draws its own head there). */
@Mixin(PistonHeadRenderer.class)
public abstract class PinkPistonRendererMixin {
    // The moving block checked against STICKY_PISTON just before the retracting head is built
    @Unique private BlockState bubbasBees$lastChecked;

    @Redirect(method = "render(Lnet/minecraft/world/level/block/piston/PistonMovingBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"))
    private boolean bubbasBees$is(BlockState state, Block block) {
        bubbasBees$lastChecked = state;
        return PinkPiston.is(state, block);
    }

    @Redirect(method = "render(Lnet/minecraft/world/level/block/piston/PistonMovingBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/Blocks;PISTON_HEAD:Lnet/minecraft/world/level/block/Block;", ordinal = 1))
    private Block bubbasBees$head() {
        return PinkPiston.headFor(bubbasBees$lastChecked);
    }
}
