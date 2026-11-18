package net.bubba.bubbasbees.client.model;

import java.util.List;
import java.util.function.Supplier;

import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Re-emits a flattened hive model with a material per quad: glass panes translucent, wooden parts solid.
 */
public class HiveGlassBakedModel extends ForwardingBakedModel {
    private static final Direction[] FACES = {Direction.DOWN, Direction.UP, Direction.NORTH,
            Direction.SOUTH, Direction.WEST, Direction.EAST};
    private static final String GLASS_SPRITE = "_glass";
    private static final RenderMaterial SOLID = material(BlendMode.SOLID);
    private static final RenderMaterial TRANSLUCENT = material(BlendMode.TRANSLUCENT);

    public HiveGlassBakedModel(BakedModel wrapped) {
        super(wrapped);
    }

    private static RenderMaterial material(BlendMode blendMode) {
        Renderer renderer = RendererAccess.INSTANCE.getRenderer();
        return renderer.materialFinder().blendMode(blendMode).find();
    }

    // The renderer API emits the quads itself so each quad can pick its own blend mode.
    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos,
                               Supplier<RandomSource> randomSupplier, RenderContext context) {
        QuadEmitter emitter = context.getEmitter();
        for (Direction face : FACES) {
            emit(emitter, wrapped.getQuads(state, face, randomSupplier.get()), face);
        }
        emit(emitter, wrapped.getQuads(state, null, randomSupplier.get()), null);
    }

    private static void emit(QuadEmitter emitter, List<BakedQuad> quads, Direction face) {
        for (BakedQuad quad : quads) {
            boolean glass = quad.getSprite().contents().name().getPath().contains(GLASS_SPRITE);
            emitter.fromVanilla(quad, glass ? TRANSLUCENT : SOLID, face).emit();
        }
    }
}
