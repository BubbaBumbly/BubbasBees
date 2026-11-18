package net.bubba.bubbasbees.entity.client;

// DEVIATION: GeckoLib 4.8's GeoRenderLayer has no getRenderType(ResourceLocation) to override, so it is a local helper.
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.bubba.bubbasbees.entity.custom.BubbasBee;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/** Draws the starlight bee's stripes, eyes, legs and wings over its star pass (see StarlightSky). */
public class StarlightOverlayLayer extends GeoRenderLayer<BubbasBee> {
    private final BubbasBeeRenderer renderer;

    public StarlightOverlayLayer(BubbasBeeRenderer renderer) {
        super(renderer);
        this.renderer = renderer;
    }

    @Override
    public void render(PoseStack poseStack, BubbasBee bee, BakedGeoModel bakedModel, RenderType renderType,
                       MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick,
                       int packedLight, int packedOverlay) {
        if (!BubbasBeeRenderer.usesSky(bee)) return;
        renderer.setOverlayPartsHidden(bee, false);
        // FIX: draw into the overlay render type's own buffer, not the base layer's
        RenderType overlayType = getRenderType(bee.isAngry() ? StarlightSky.OVERLAY_ANGRY : StarlightSky.OVERLAY);
        renderer.reRender(bakedModel, poseStack, bufferSource, bee, overlayType, bufferSource.getBuffer(overlayType),
                partialTick, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
        renderer.setOverlayPartsHidden(bee, true);
    }

    /** Translucent, not GeckoLib's cutout: the overlay's half-alpha pixels (the dim mask) darken the stars below. */
    public RenderType getRenderType(ResourceLocation textureLocation) {
        return RenderType.entityTranslucent(textureLocation);
    }
}
