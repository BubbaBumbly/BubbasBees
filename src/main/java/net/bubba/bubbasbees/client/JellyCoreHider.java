package net.bubba.bubbasbees.client;

import net.bubba.bubbasbees.registries.ModBlocks;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Royal jelly block: the opaque core is left out of the one block the camera is inside, so it does not clip
 * through the view. The block is redrawn when the camera enters or leaves it.
 */
public final class JellyCoreHider {
    private static final Direction[] FACES = {Direction.DOWN, Direction.UP, Direction.NORTH,
            Direction.SOUTH, Direction.WEST, Direction.EAST, null};

    /** The jelly block the camera is in, or null. Read by the chunk builder threads. */
    public static volatile BlockPos hidden;

    static void update() {
        Minecraft mc = Minecraft.getInstance();
        BlockPos now = null;
        if (mc.level != null && mc.gameRenderer.getMainCamera().isInitialized()) {
            BlockPos pos = BlockPos.containing(mc.gameRenderer.getMainCamera().getPosition());
            if (mc.level.getBlockState(pos).is(ModBlocks.ROYAL_JELLY_BLOCK.get())) now = pos;
        }
        BlockPos old = hidden;
        if (Objects.equals(old, now)) return;
        hidden = now;
        redraw(mc, old);
        redraw(mc, now);
    }

    private static void redraw(Minecraft mc, BlockPos pos) {
        if (pos == null || mc.level == null) return;
        BlockState state = mc.level.getBlockState(pos);
        mc.levelRenderer.blockChanged(mc.level, pos, state, state, 8);   // 8 = rebuild this frame
    }

    static boolean isCore(BakedQuad quad) {
        return quad.getSprite().contents().name().getPath().endsWith("royal_jelly_block_inner");
    }

    /** Called once from the client initializer. */
    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> update());
        ModelLoadingPlugin.register(plugin -> plugin.modifyModelAfterBake().register((model, context) -> {
            ResourceLocation id = context.resourceId();
            return model != null && id != null && "bubbasbees".equals(id.getNamespace())
                    && id.getPath().equals("block/royal_jelly_block") ? new Model(model) : model;
        }));
    }

    private static class Model extends ForwardingBakedModel {
        Model(BakedModel original) {
            this.wrapped = original;
        }

        @Override
        public boolean isVanillaAdapter() {
            return false;
        }

        @Override
        public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos,
                                   Supplier<RandomSource> randomSupplier, RenderContext context) {
            if (!pos.equals(hidden)) {
                wrapped.emitBlockQuads(blockView, state, pos, randomSupplier, context);
                return;
            }
            // default material: the block's own (translucent) layer
            RenderMaterial material = RendererAccess.INSTANCE.getRenderer().materialFinder().find();
            QuadEmitter emitter = context.getEmitter();
            for (Direction face : FACES) {
                for (BakedQuad quad : wrapped.getQuads(state, face, randomSupplier.get())) {
                    if (!isCore(quad)) emitter.fromVanilla(quad, material, face).emit();
                }
            }
        }
    }

    private JellyCoreHider() {
    }
}
