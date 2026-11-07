package net.bubba.bubbasbees.client.guide;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

/** Items, entities and block structures drawn inside the book. */
public final class GuideRender {
    private GuideRender() {
    }

    /** An item at (x, y), size 16 * scale. */
    public static void item(ItemStack stack, int x, int y, float scale, boolean decorations) {
        Minecraft mc = Minecraft.getInstance();
        PoseStack view = RenderSystem.getModelViewStack();
        view.pushPose();
        view.translate(x, y, 0);
        view.scale(scale, scale, 1);
        RenderSystem.applyModelViewMatrix();
        mc.getItemRenderer().renderAndDecorateItem(stack, 0, 0);
        if (decorations) mc.getItemRenderer().renderGuiItemDecorations(mc.font, stack, 0, 0);
        view.popPose();
        RenderSystem.applyModelViewMatrix();
    }

    /**
     * An entity standing with its feet at (x, y), size px per block, turned by yaw. Same setup as vanilla's inventory
     * preview (including its depth flip), so our bee renderer needs no special case for the book.
     */
    public static void entity(Entity entity, int x, int y, float size, float yaw) {
        Minecraft mc = Minecraft.getInstance();
        PoseStack view = RenderSystem.getModelViewStack();
        view.pushPose();
        view.translate(x, y, 1050);
        view.scale(1, 1, -1);
        RenderSystem.applyModelViewMatrix();
        PoseStack pose = new PoseStack();
        pose.translate(0, 0, 1000);
        pose.scale(size, size, size);
        Quaternion q = Vector3f.ZP.rotationDegrees(180);
        q.mul(Vector3f.XP.rotationDegrees(-15));
        pose.mulPose(q);
        pose.mulPose(Vector3f.YP.rotationDegrees(yaw));
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
        RenderSystem.runAsFancy(() -> dispatcher.render(entity, 0, 0, 0, 0, mc.getFrameTime(), pose, buffers,
                LightTexture.FULL_BRIGHT));
        buffers.endBatch();
        dispatcher.setRenderShadow(true);
        view.popPose();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
    }

    /** A structure centred in the box (x, y, w, h), tilted and turned by yaw. */
    public static void multiblock(GuideMultiblock mb, int x, int y, int w, int h, float yaw) {
        Minecraft mc = Minecraft.getInstance();
        int minX = 0, maxX = 0, minY = 0, maxY = 0, minZ = 0, maxZ = 0;
        for (GuideMultiblock.Cell c : mb.cells) {
            minX = Math.min(minX, c.offset().getX()); maxX = Math.max(maxX, c.offset().getX());
            minY = Math.min(minY, c.offset().getY()); maxY = Math.max(maxY, c.offset().getY());
            minZ = Math.min(minZ, c.offset().getZ()); maxZ = Math.max(maxZ, c.offset().getZ());
        }
        float span = Math.max(Math.max(maxX - minX, maxZ - minZ) + 1, maxY - minY + 1);
        float scale = Math.min(w, h) / (span * 1.45F);
        PoseStack view = RenderSystem.getModelViewStack();
        view.pushPose();
        view.translate(x + w / 2F, y + h / 2F, 400);
        view.scale(scale, -scale, scale);
        view.mulPose(Vector3f.XP.rotationDegrees(35));
        view.mulPose(Vector3f.YP.rotationDegrees(yaw));
        view.translate(-(minX + maxX + 1) / 2F, -(minY + maxY + 1) / 2F, -(minZ + maxZ + 1) / 2F);
        RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
        RenderSystem.enableDepthTest();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        PoseStack pose = new PoseStack();
        for (GuideMultiblock.Cell c : mb.cells) {
            BlockState state = mb.displayState(c);
            pose.pushPose();
            pose.translate(c.offset().getX(), c.offset().getY(), c.offset().getZ());
            mc.getBlockRenderer().renderSingleBlock(state, pose, buffers, LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
            pose.popPose();
        }
        buffers.endBatch();
        view.popPose();
        RenderSystem.applyModelViewMatrix();
    }
}
