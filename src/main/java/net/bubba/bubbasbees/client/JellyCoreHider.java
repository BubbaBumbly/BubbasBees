package net.bubba.bubbasbees.client;

import net.bubba.bubbasbees.BubbasBees;
import net.bubba.bubbasbees.registries.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Royal jelly block: the opaque core is left out of the one block the camera is inside, so it does not clip
 * through the view. The block is redrawn when the camera enters or leaves it.
 */
public final class JellyCoreHider {
    private static final ModelProperty<BlockPos> POS = new ModelProperty<>();

    /** The jelly block the camera is in, or null. Read by the chunk builder threads. */
    public static volatile BlockPos hidden;

    static void update() {
        Minecraft mc = Minecraft.getInstance();
        BlockPos now = null;
        if (mc.level != null && mc.gameRenderer.getMainCamera().isInitialized()) {
            BlockPos pos = new BlockPos(mc.gameRenderer.getMainCamera().getPosition());
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
        return quad.getSprite().getName().getPath().endsWith("royal_jelly_block_inner");
    }

    @Mod.EventBusSubscriber(modid = BubbasBees.MOD_ID, value = Dist.CLIENT)
    public static class Ticker {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) update();
        }
    }

    @Mod.EventBusSubscriber(modid = BubbasBees.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class Models {
        @SubscribeEvent
        public static void onModelsBaked(ModelEvent.BakingCompleted event) {
            event.getModels().computeIfPresent(
                    BlockModelShaper.stateToModelLocation(ModBlocks.ROYAL_JELLY_BLOCK.get().defaultBlockState()),
                    (id, model) -> new Model(model));
        }
    }

    private static class Model extends BakedModelWrapper<BakedModel> {
        Model(BakedModel original) {
            super(original);
        }

        @Override
        public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos,
                                               @NotNull BlockState state, @NotNull ModelData data) {
            return data.derive().with(POS, pos.immutable()).build();
        }

        @Override
        public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand,
                                                 @NotNull ModelData data, @Nullable RenderType renderType) {
            List<BakedQuad> quads = originalModel.getQuads(state, side, rand, data, renderType);
            BlockPos inside = hidden;
            if (inside == null || !inside.equals(data.get(POS))) return quads;
            List<BakedQuad> kept = new ArrayList<>(quads.size());
            for (BakedQuad quad : quads) if (!isCore(quad)) kept.add(quad);
            return kept;
        }
    }

    private JellyCoreHider() {
    }
}
