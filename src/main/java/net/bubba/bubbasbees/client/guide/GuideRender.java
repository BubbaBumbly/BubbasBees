// DEVIATION: 1.21.1 returns a Matrix4fStack from RenderSystem and removed Minecraft.getFrameTime, so entity/block
// previews draw through GuiGraphics.pose() like vanilla's inventory preview.
package net.bubba.bubbasbees.client.guide;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Quaternionf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Items, entities and block structures drawn inside the book. */
public final class GuideRender {
    private GuideRender() {
    }

    /** An item at (x, y), size 16 * scale. */
    public static void item(ItemStack stack, int x, int y, float scale, boolean decorations, GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        PoseStack view = graphics.pose();
        view.pushPose();
        view.translate(x, y, 0);
        view.scale(scale, scale, 1);
        graphics.renderItem(stack, 0, 0);
        if (decorations) graphics.renderItemDecorations(mc.font, stack, 0, 0);
        view.popPose();
    }

    /**
     * An entity standing with its feet at (x, y), size px per block, turned by yaw. Same setup as vanilla's inventory
     * preview (including its depth flip), so our bee renderer needs no special case for the book.
     */
    public static void entity(Entity entity, int x, int y, float size, float yaw, GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 50);
        pose.scale(size, size, -size);
        Quaternionf q = new Quaternionf().rotateZ((float) Math.toRadians(180));
        q.mul(new Quaternionf().rotateX((float) Math.toRadians(-15)));
        pose.mulPose(q);
        pose.mulPose(new Quaternionf().rotateY((float) Math.toRadians(yaw)));
        if (entity instanceof LivingEntity living) {
            living.yBodyRot = 0;
            living.yBodyRotO = 0;
            living.yHeadRot = 0;
            living.yHeadRotO = 0;
        }
        entity.setYRot(0);
        entity.setXRot(0);
        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        dispatcher.setRenderShadow(false);
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        float partialTick = mc.getTimer().getGameTimeDeltaPartialTick(true);
        RenderSystem.runAsFancy(() -> dispatcher.render(entity, 0, 0, 0, 0, partialTick, pose, buffers,
                LightTexture.FULL_BRIGHT));
        buffers.endBatch();
        dispatcher.setRenderShadow(true);
        pose.popPose();
        Lighting.setupFor3DItems();
    }

    /** A structure centred in the box (x, y, w, h), tilted and turned by yaw. */
    public static void multiblock(GuideMultiblock mb, int x, int y, int w, int h, float yaw, GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        int minX = 0, maxX = 0, minY = 0, maxY = 0, minZ = 0, maxZ = 0;
        for (GuideMultiblock.Cell c : mb.cells) {
            minX = Math.min(minX, c.offset().getX()); maxX = Math.max(maxX, c.offset().getX());
            minY = Math.min(minY, c.offset().getY()); maxY = Math.max(maxY, c.offset().getY());
            minZ = Math.min(minZ, c.offset().getZ()); maxZ = Math.max(maxZ, c.offset().getZ());
        }
        float span = Math.max(Math.max(maxX - minX, maxZ - minZ) + 1, maxY - minY + 1);
        float scale = Math.min(w, h) / (span * 1.45F);
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x + w / 2F, y + h / 2F, 50);
        pose.scale(scale, -scale, scale);
        pose.mulPose(new Quaternionf().rotateX((float) Math.toRadians(35)));
        pose.mulPose(new Quaternionf().rotateY((float) Math.toRadians(yaw)));
        pose.translate(-(minX + maxX + 1) / 2F, -(minY + maxY + 1) / 2F, -(minZ + maxZ + 1) / 2F);
        Lighting.setupFor3DItems();
        RenderSystem.enableDepthTest();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        for (GuideMultiblock.Cell c : mb.cells) {
            BlockState state = mb.displayState(c);
            pose.pushPose();
            pose.translate(c.offset().getX(), c.offset().getY(), c.offset().getZ());
            mc.getBlockRenderer().renderSingleBlock(state, pose, buffers, LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
            pose.popPose();
        }
        buffers.endBatch();
        pose.popPose();
    }
}
