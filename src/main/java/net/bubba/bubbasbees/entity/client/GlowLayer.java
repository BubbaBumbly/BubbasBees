package net.bubba.bubbasbees.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.renderers.geo.GeoLayerRenderer;
import software.bernie.geckolib3.renderers.geo.IGeoRenderer;

import java.util.HashMap;
import java.util.Map;

/**
 * Faint glow on part of a bee's texture (ore spots, eyes, magma cracks...): if "<texture>_glow.png" exists next to
 * the texture in use (normal, angry or deepslate), the model is drawn again with it at full brightness. The masks keep
 * only the glowing pixels, at 35% alpha (the "faint" strength, user 2026-11-08), so the bee glows a little in the dark.
 * Masks: BubbasBeesArt/work/glow/final.
 */
public class GlowLayer<T extends LivingEntity & IAnimatable> extends GeoLayerRenderer<T> {
    private static final Map<ResourceLocation, ResourceLocation> GLOW = new HashMap<>();
    private static final ResourceLocation NONE = new ResourceLocation("bubbasbees", "none");
    private final IGeoRenderer<T> renderer;

    public GlowLayer(IGeoRenderer<T> renderer) {
        super(renderer);
        this.renderer = renderer;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity, float limbSwing,
                       float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) return;
        ResourceLocation glow = glowFor(renderer.getTextureLocation(entity));
        if (glow == null) return;
        renderModel(getEntityModel(), glow, poseStack, buffer, LightTexture.FULL_BRIGHT, entity, partialTicks, 1.0F, 1.0F, 1.0F);
    }

    /** Translucent and full-bright: the mask's 35% alpha blends the lit pixels over the bee. */
    @Override
    public RenderType getRenderType(ResourceLocation textureLocation) {
        return RenderType.entityTranslucentEmissive(textureLocation);
    }

    /** "<path>_glow.png" if that texture exists, else null; looked up once per texture. */
    private static ResourceLocation glowFor(ResourceLocation texture) {
        ResourceLocation glow = GLOW.computeIfAbsent(texture, t -> {
            ResourceLocation g = new ResourceLocation(t.getNamespace(), t.getPath().replace(".png", "_glow.png"));
            return Minecraft.getInstance().getResourceManager().getResource(g).isPresent() ? g : NONE;
        });
        return glow == NONE ? null : glow;
    }

    /** Resource packs can add or remove masks: forget the lookups on reload. */
    public static void clearCache() {
        GLOW.clear();
    }
}
