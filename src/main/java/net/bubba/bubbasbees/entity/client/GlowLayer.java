// DEVIATION: GeckoLib 4.9 moved its classes out of the .core package and reRender gained a trailing colour int.
package net.bubba.bubbasbees.entity.client;

// DEVIATION: GeckoLib 4.8's GeoRenderLayer has no getRenderType(ResourceLocation) to override, so it is a local helper
// and the layer is registered with addRenderLayer instead of addLayer.
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.HashMap;
import java.util.Map;

/**
 * Faint glow on part of a bee's texture (ore spots, eyes, magma cracks...): if "<texture>_glow.png" exists next to
 * the texture in use (normal, angry or deepslate), the model is drawn again with it at full brightness. The masks keep
 * only the glowing pixels, at 35% alpha (the "faint" strength), so the bee glows a little in the dark.
 */
public class GlowLayer<T extends LivingEntity & GeoAnimatable> extends GeoRenderLayer<T> {
    private static final Map<ResourceLocation, ResourceLocation> GLOW = new HashMap<>();
    private static final ResourceLocation NONE = ResourceLocation.fromNamespaceAndPath("bubbasbees", "none");
    private final GeoRenderer<T> renderer;

    public GlowLayer(GeoRenderer<T> renderer) {
        super(renderer);
        this.renderer = renderer;
    }

    @Override
    public void render(PoseStack poseStack, T entity, BakedGeoModel bakedModel, RenderType renderType,
                       MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick,
                       int packedLight, int packedOverlay) {
        if (entity.isInvisible()) return;
        ResourceLocation glow = glowFor(renderer.getTextureLocation(entity));
        if (glow == null) return;
        // FIX: draw into the glow render type's own buffer, not the base layer's
        RenderType glowType = getRenderType(glow);
        renderer.reRender(bakedModel, poseStack, bufferSource, entity, glowType, bufferSource.getBuffer(glowType),
                partialTick, LightTexture.FULL_BRIGHT, packedOverlay, -1);
    }

    /** Translucent and full-bright: the mask's 35% alpha blends the lit pixels over the bee. */
    public RenderType getRenderType(ResourceLocation textureLocation) {
        return RenderType.entityTranslucentEmissive(textureLocation);
    }

    /** "<path>_glow.png" if that texture exists, else null; looked up once per texture. */
    private static ResourceLocation glowFor(ResourceLocation texture) {
        ResourceLocation glow = GLOW.computeIfAbsent(texture, t -> {
            ResourceLocation g = ResourceLocation.fromNamespaceAndPath(t.getNamespace(), t.getPath().replace(".png", "_glow.png"));
            return Minecraft.getInstance().getResourceManager().getResource(g).isPresent() ? g : NONE;
        });
        return glow == NONE ? null : glow;
    }

    /** Resource packs can add or remove masks: forget the lookups on reload. */
    public static void clearCache() {
        GLOW.clear();
    }
}
