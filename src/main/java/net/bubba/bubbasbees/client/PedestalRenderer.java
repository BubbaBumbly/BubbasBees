package net.bubba.bubbasbees.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.bubba.bubbasbees.blockentities.PedestalBlockEntity;
import net.bubba.bubbasbees.blocks.custom.PedestalBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/** Floats the pedestal's item above it, bobbing gently and turning slowly (faster on an altar during a ritual). */
public class PedestalRenderer implements BlockEntityRenderer<PedestalBlockEntity> {

    public PedestalRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(@NotNull PedestalBlockEntity pedestal, float partialTick, @NotNull PoseStack pose,
                       @NotNull MultiBufferSource buffers, int light, int overlay) {
        ItemStack item = pedestal.getItem();
        if (item.isEmpty() || pedestal.getLevel() == null) return;
        double top = pedestal.getBlockState().getBlock() instanceof PedestalBlock block ? block.topHeight() : 1.0;
        float time = pedestal.getLevel().getGameTime() + partialTick;
        float bob = Mth.sin(time / 12.0F) * 0.06F;
        float degreesPerTick = pedestal.isRitualActive() ? 9.0F : 1.5F;
        pose.pushPose();
        pose.translate(0.5, top + 0.3 + bob, 0.5);
        pose.mulPose(Vector3f.YP.rotationDegrees((time * degreesPerTick) % 360.0F));
        pose.scale(0.6F, 0.6F, 0.6F);
        Minecraft.getInstance().getItemRenderer().renderStatic(item, ItemTransforms.TransformType.FIXED, light,
                OverlayTexture.NO_OVERLAY, pose, buffers, (int) pedestal.getBlockPos().asLong());
        pose.popPose();
    }
}
