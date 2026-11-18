package net.bubba.bubbasbees.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.bubba.bubbasbees.bees.BeeSpec;
import net.bubba.bubbasbees.entity.custom.BubbasBee;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;


public class BubbasBeeRenderer extends GeoEntityRenderer<BubbasBee> {
    public BubbasBeeRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new BeeModel());
        this.shadowRadius = 0.2f;
        this.addRenderLayer(new StarlightOverlayLayer(this));
        this.addRenderLayer(new GlowLayer<>(this));
    }

    /** Whether this bee is drawn as a window into the night sky now (the starlight bee, no shader pack); see StarlightSky. */
    static boolean usesSky(BubbasBee bee) {
        return bee.getSpec() != null && "starlight".equals(bee.getSpec().id()) && StarlightSky.available();
    }

    /** Parts drawn only by the overlay layer: in the star pass their texture shapes would double up (two antenna pairs). */
    private static final String[] OVERLAY_PARTS = {"leftWing", "rightWing", "leftAntenna", "rightAntenna",
            "frontLegs", "middleLegs", "backLegs", "stinger"};

    // The star pass leaves the overlay-only parts out; the overlay layer draws them.
    void setOverlayPartsHidden(BubbasBee bee, boolean hidden) {
        BakedGeoModel model = this.getGeoModel().getBakedModel(this.getGeoModel().getModelResource(bee));
        if (model == null) return;
        for (String name : OVERLAY_PARTS) {
            model.getBone(name).ifPresent(bone -> bone.setHidden(hidden));
        }
    }

    // GeckoLib 4 does not scale babies itself; halve the model like vanilla's baby bee, times the bee's own size
    @Override
    public void scaleModelForRender(float widthScale, float heightScale, PoseStack poseStack, BubbasBee animatable,
                                    BakedGeoModel model, boolean isReRender, float partialTick, int packedLight, int packedOverlay) {
        super.scaleModelForRender(widthScale * sizeOf(animatable), heightScale * sizeOf(animatable),
                poseStack, animatable, model, isReRender, partialTick, packedLight, packedOverlay);
    }

    private static float sizeOf(BubbasBee bee) {
        float scale = bee.getSpec() == null ? 1.0F : bee.getSpec().scale();
        return bee.isBaby() ? 0.5F * scale : scale;
    }

    // Glow bees glow: drawn at full brightness whatever the light around them
    @Override
    public void render(BubbasBee bee, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        // The Encyclobeedia's bee pages (and other GUI previews) never tick their bee, and GeckoLib 4 runs the animation
        // clock from tickCount, so a stuck tickCount loops the flap every frame. Drive it from the level's clock.
        if (!bee.isAddedToLevel() && bee.level() != null) bee.tickCount = (int) bee.level().getGameTime();
        boolean glows = bee.getSpec() != null && bee.getSpec().has(BeeSpec.Trait.GLOW);
        boolean sky = usesSky(bee);
        if (sky) setOverlayPartsHidden(bee, true);
        try {
            super.render(bee, entityYaw, partialTick, poseStack, bufferSource, glows ? LightTexture.FULL_BRIGHT : packedLight);
        } finally {
            if (sky) setOverlayPartsHidden(bee, false);
        }
    }

    @Override
    public RenderType getRenderType(BubbasBee animatable, ResourceLocation texture,
                                    MultiBufferSource bufferSource, float partialTick) {
        if (usesSky(animatable)) return StarlightSky.SKY;
        // Cutout drops partial alpha; see-through bees (the soul bee) blend
        if (animatable.getSpec() != null && animatable.getSpec().translucent()) {
            return RenderType.entityTranslucent(texture);
        }
        // GeckoLib 4 defaults to entityCutoutNoCull, which draws both faces of the flat wings in one plane so they flicker
        return RenderType.entityCutout(texture);
    }
}
