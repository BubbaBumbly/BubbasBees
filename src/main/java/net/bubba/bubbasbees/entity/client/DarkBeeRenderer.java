package net.bubba.bubbasbees.entity.client;

// DEVIATION: GeckoLib 4 registers layers with addRenderLayer instead of the GeckoLib 3 addLayer.
import net.bubba.bubbasbees.BubbasBees;
import net.bubba.bubbasbees.entity.custom.DarkBee;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import org.jetbrains.annotations.Nullable;

/** Smoky see-through bee (PLACEHOLDER texture until its art is made). */
public class DarkBeeRenderer extends GeoEntityRenderer<DarkBee> {
    public DarkBeeRenderer(EntityRendererProvider.Context context) {
        super(context, new GeoModel<>() {
            @Override
            public ResourceLocation getModelResource(DarkBee bee) {
                return ResourceLocation.fromNamespaceAndPath(BubbasBees.MOD_ID, "geo/ore.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(DarkBee bee) {
                return ResourceLocation.fromNamespaceAndPath(BubbasBees.MOD_ID, "textures/entity/dark_bee.png");
            }

            @Override
            public ResourceLocation getAnimationResource(DarkBee bee) {
                return ResourceLocation.fromNamespaceAndPath(BubbasBees.MOD_ID, "animations/bee.animation.json");
            }
        });
        this.addRenderLayer(new GlowLayer<>(this));
        this.shadowRadius = 0.15F;
    }

    @Override
    public RenderType getRenderType(DarkBee animatable, ResourceLocation texture,
                                    @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
}
