package net.bubba.bubbasbees.client.guide;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.bubba.bubbasbees.BubbasBees;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.jetbrains.annotations.Nullable;

/**
 * "Visualize" on a ritual page: a see-through ghost of the blocks still missing, in the world. Until it's pinned the
 * ghost follows what you look at (snapping onto an altar you look at); right-click pins it. Wrong blocks in the way
 * get a red outline. A counter shows placed / total. Clear it from the book's eye button or with sneak + right-click.
 */
@EventBusSubscriber(modid = BubbasBees.MOD_ID, value = Dist.CLIENT)
public final class GuideVisualizer {
    public static final ResourceLocation OVERLAY_ID = ResourceLocation.fromNamespaceAndPath(BubbasBees.MOD_ID, "guide_visualize");
    private static final float GHOST_ALPHA = 0.45F;
    private static @Nullable String name;
    private static @Nullable GuideMultiblock multiblock;
    private static @Nullable BlockPos anchor;
    private static @Nullable ResourceKey<Level> dimension;

    private GuideVisualizer() {
    }

    public static boolean isShowing(String pageName) {
        return multiblock != null && pageName.equals(name);
    }

    public static void show(String pageName, GuideMultiblock mb) {
        name = pageName;
        multiblock = mb;
        anchor = null;
        dimension = null;
        var player = Minecraft.getInstance().player;
        if (player != null) player.displayClientMessage(Component.translatable("gui.bubbasbees.guide.visualize_hint"), true);
    }

    public static void clear() {
        multiblock = null;
        anchor = null;
        name = null;
    }

    /** Where the centre ('0', the altar) goes: the pin, an altar you look at, or the spot in front of the block you look at. */
    private static @Nullable BlockPos centre() {
        if (anchor != null) return anchor;
        Minecraft mc = Minecraft.getInstance();
        if (multiblock == null || mc.level == null || !(mc.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) return null;
        BlockState looked = mc.level.getBlockState(hit.getBlockPos());
        for (GuideMultiblock.Cell c : multiblock.cells) {
            if (c.offset().equals(BlockPos.ZERO) && multiblock.matches(c, looked)) return hit.getBlockPos();
        }
        return hit.getBlockPos().relative(hit.getDirection());
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || multiblock == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (anchor != null && (mc.level.dimension() != dimension || mc.player.blockPosition().distSqr(anchor) > 128 * 128)) {
            clear();
            return;
        }
        BlockPos centre = centre();
        if (centre == null) return;
        PoseStack pose = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        MultiBufferSource ghost = type -> new Faded(buffers.getBuffer(RenderType.translucent()));
        var done = multiblock.done(mc.level, centre);
        for (GuideMultiblock.Cell c : multiblock.cells) {
            if (done.contains(c)) continue;
            BlockPos pos = centre.offset(c.offset());
            // Off-screen cells are skipped: a line box behind the camera projects as stray red dots on screen.
            if (!event.getFrustum().isVisible(new AABB(pos))) continue;
            BlockState there = mc.level.getBlockState(pos);
            pose.pushPose();
            pose.translate(pos.getX() - cam.x, pos.getY() - cam.y, pos.getZ() - cam.z);
            if (!there.isAir() && !there.canBeReplaced()) {
                LevelRenderer.renderLineBox(pose, buffers.getBuffer(RenderType.lines()), new AABB(0, 0, 0, 1, 1, 1).inflate(0.002),
                        1F, 0.2F, 0.2F, 1F);
            }
            pose.translate(0.5, 0.5, 0.5);
            pose.scale(0.85F, 0.85F, 0.85F);
            pose.translate(-0.5, -0.5, -0.5);
            mc.getBlockRenderer().renderSingleBlock(multiblock.displayState(c), pose, ghost, LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, ModelData.EMPTY, RenderType.translucent());
            pose.popPose();
        }
        buffers.endBatch(RenderType.translucent());
        buffers.endBatch(RenderType.lines());
    }

    @SubscribeEvent
    public static void onUse(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (multiblock == null || !event.isUseItem() || mc.player == null || mc.level == null) return;
        if (anchor == null) {
            BlockPos centre = centre();
            if (centre == null) return;
            anchor = centre;
            dimension = mc.level.dimension();
        } else if (mc.player.isShiftKeyDown() && mc.player.getMainHandItem().isEmpty()) {
            clear();
            mc.player.displayClientMessage(Component.translatable("gui.bubbasbees.guide.visualize_cleared"), true);
        } else {
            return;
        }
        event.setCanceled(true);
        event.setSwingHand(false);
    }

    /** "Celestial Ritual: 65 / 77" above the hotbar while a ghost is showing. */
    public static final class Overlay implements LayeredDraw.Layer {
        @Override
        public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
            Minecraft mc = Minecraft.getInstance();
            if (multiblock == null || mc.level == null || mc.options.hideGui) return;
            BlockPos centre = centre();
            Component line;
            if (centre == null) {
                line = Component.literal(name + ": ").append(Component.translatable("gui.bubbasbees.guide.visualize_aim"));
            } else {
                int done = multiblock.done(mc.level, centre).size();
                int total = multiblock.cells.size();
                line = Component.literal(name + ": " + done + " / " + total)
                        .withStyle(done == total ? ChatFormatting.GREEN : ChatFormatting.WHITE);
                if (anchor == null) line = ((net.minecraft.network.chat.MutableComponent) line).append(Component.literal("  "))
                        .append(Component.translatable("gui.bubbasbees.guide.visualize_pin").withStyle(ChatFormatting.GRAY));
            }
            int w = mc.font.width(line);
            graphics.drawString(mc.font, line, (graphics.guiWidth() - w) / 2, graphics.guiHeight() - 88, 0xFFFFFF);
        }
    }

    /** Passes vertices through with their alpha scaled, for the see-through ghost blocks. */
    private record Faded(VertexConsumer inner) implements VertexConsumer {
        @Override public VertexConsumer addVertex(float x, float y, float z) { inner.addVertex(x, y, z); return this; }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { inner.setColor(r, g, b, (int) (a * GHOST_ALPHA)); return this; }
        @Override public VertexConsumer setUv(float u, float v) { inner.setUv(u, v); return this; }
        @Override public VertexConsumer setUv1(int u, int v) { inner.setUv1(u, v); return this; }
        @Override public VertexConsumer setUv2(int u, int v) { inner.setUv2(u, v); return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { inner.setNormal(x, y, z); return this; }
    }
}
