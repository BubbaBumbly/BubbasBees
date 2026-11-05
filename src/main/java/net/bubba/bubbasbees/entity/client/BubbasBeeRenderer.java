package net.bubba.bubbasbees.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector4f;
import net.bubba.bubbasbees.bees.BeeSpec;
import net.bubba.bubbasbees.entity.custom.BubbasBee;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;


public class BubbasBeeRenderer extends GeoEntityRenderer<BubbasBee> {
    public BubbasBeeRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new BeeModel());
        this.shadowRadius = 0.2f;
        this.addLayer(new StarlightOverlayLayer(this));
    }

    /** Whether this bee is drawn as a window into the night sky now (the starlight bee, no shader pack); see StarlightSky. */
    static boolean usesSky(BubbasBee bee) {
        return bee.getSpec() != null && "starlight".equals(bee.getSpec().id()) && StarlightSky.available();
    }

    /** Parts drawn only by the overlay layer: in the star pass their texture shapes would double up (two antenna pairs). */
    private static final String[] OVERLAY_PARTS = {"leftWing", "rightWing", "leftAntenna", "rightAntenna",
            "frontLegs", "middleLegs", "backLegs", "stinger"};

    /**
     * The star pass leaves the overlay-only parts out; the overlay layer draws them. Bones come from the bee's baked
     * model: GeckoLib 3's modelProvider.getBone throws until that model has been rendered once.
     */
    void setOverlayPartsHidden(BubbasBee bee, boolean hidden) {
        var model = this.modelProvider.getModel(this.modelProvider.getModelResource(bee));
        for (String name : OVERLAY_PARTS) {
            model.getBone(name).ifPresent(bone -> bone.setHidden(hidden));
        }
    }

    // GeckoLib 3 does not scale babies itself; halve the model like vanilla's baby bee, times the bee's own size
    @Override
    public float getWidthScale(BubbasBee bee) {
        return sizeOf(bee) * super.getWidthScale(bee);
    }

    @Override
    public float getHeightScale(BubbasBee bee) {
        return sizeOf(bee) * super.getHeightScale(bee);
    }

    private static float sizeOf(BubbasBee bee) {
        float scale = bee.getSpec() == null ? 1.0F : bee.getSpec().scale();
        return bee.isBaby() ? 0.5F * scale : scale;
    }

    // Glow bees glow: drawn at full brightness whatever the light around them
    @Override
    public void render(BubbasBee bee, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        // Patchouli's entity pages (and other GUI previews) never tick their bee, and GeckoLib 3 runs the animation
        // clock from tickCount, so a stuck tickCount loops the flap every frame. Drive it from the level's clock.
        if (!bee.isAddedToWorld() && bee.level != null) bee.tickCount = (int) bee.level.getGameTime();
        boolean glows = bee.getSpec() != null && bee.getSpec().has(BeeSpec.Trait.GLOW);
        boolean sky = usesSky(bee);
        boolean book = inPatchouliBook(bee);
        if (book) {
            poseStack.pushPose();
            flipDepth(poseStack);
        }
        if (sky) setOverlayPartsHidden(bee, true);
        try {
            super.render(bee, entityYaw, partialTick, poseStack, bufferSource, glows ? LightTexture.FULL_BRIGHT : packedLight);
        } finally {
            if (sky) setOverlayPartsHidden(bee, false);
            if (book) poseStack.popPose();
        }
    }

    /**
     * Patchouli's entity pages draw with screen depth reversed (vanilla's inventory preview flips z, Patchouli 1.19.2
     * doesn't), so a bee seen from behind shows its antennae and stinger through its body. Jade/JEI/inventory
     * previews go through vanilla's flip and are left alone.
     */
    private static boolean inPatchouliBook(BubbasBee bee) {
        var screen = Minecraft.getInstance().screen;
        return !bee.isAddedToWorld() && screen != null && screen.getClass().getName().startsWith("vazkii.patchouli");
    }

    /**
     * Mirrors screen depth around the bee's origin (z' = 2c - z): screen x/y, and so face winding and culling, stay
     * the same; only which part is in front flips. Normals are left alone, so the lighting doesn't change.
     */
    private static void flipDepth(PoseStack poseStack) {
        Matrix4f pose = poseStack.last().pose();
        Vector4f origin = new Vector4f(0, 0, 0, 1);
        origin.transform(pose);
        Matrix4f flip = Matrix4f.createTranslateMatrix(0, 0, 2 * origin.z());
        flip.multiply(Matrix4f.createScaleMatrix(1, 1, -1));
        flip.multiply(pose);
        pose.load(flip);
    }

    @Override
    public RenderType getRenderType(BubbasBee animatable, float partialTicks, PoseStack stack,
                                    @Nullable MultiBufferSource renderTypeBuffer,
                                    @Nullable VertexConsumer vertexBuilder, int packedLightIn,
                                    ResourceLocation textureLocation) {
        if (usesSky(animatable)) return StarlightSky.SKY;
        // GeckoLib 3 defaults to entityCutout, which drops partial alpha; see-through bees (the soul bee) blend
        if (animatable.getSpec() != null && animatable.getSpec().translucent()) {
            return RenderType.entityTranslucent(textureLocation);
        }
        return super.getRenderType(animatable, partialTicks, stack,
                renderTypeBuffer, vertexBuilder, packedLightIn, textureLocation);
    }
}
