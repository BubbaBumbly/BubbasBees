package net.bubba.bubbasbees.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.bubba.bubbasbees.entity.custom.BubbasBee;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.renderers.geo.GeoLayerRenderer;
import software.bernie.geckolib3.renderers.geo.IGeoRenderer;

/** Draws the starlight bee's stripes, eyes, legs and wings over its star pass (see StarlightSky). */
public class StarlightOverlayLayer extends GeoLayerRenderer<BubbasBee> {
    private final BubbasBeeRenderer renderer;

    public StarlightOverlayLayer(BubbasBeeRenderer renderer) {
        super((IGeoRenderer<BubbasBee>) renderer);
        this.renderer = renderer;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, BubbasBee bee, float limbSwing,
                       float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!BubbasBeeRenderer.usesSky(bee)) return;
        renderer.setOverlayPartsHidden(bee, false);
        renderModel(getEntityModel(), StarlightSky.OVERLAY, poseStack, buffer, packedLight, bee, partialTicks, 1.0F, 1.0F, 1.0F);
        renderer.setOverlayPartsHidden(bee, true);
    }

    /** Translucent, not GeckoLib's cutout: the overlay's half-alpha pixels (the dim mask) darken the stars below. */
    @Override
    public RenderType getRenderType(ResourceLocation textureLocation) {
        return RenderType.entityTranslucent(textureLocation);
    }
}
