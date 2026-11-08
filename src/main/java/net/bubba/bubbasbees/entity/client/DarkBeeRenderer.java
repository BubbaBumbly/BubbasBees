package net.bubba.bubbasbees.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.bubba.bubbasbees.BubbasBees;
import net.bubba.bubbasbees.entity.custom.DarkBee;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib3.model.AnimatedGeoModel;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

/** Smoky see-through bee (PLACEHOLDER texture until its art is made). */
public class DarkBeeRenderer extends GeoEntityRenderer<DarkBee> {
    public DarkBeeRenderer(EntityRendererProvider.Context context) {
        super(context, new AnimatedGeoModel<>() {
            @Override
            public ResourceLocation getModelResource(DarkBee bee) {
                return new ResourceLocation(BubbasBees.MOD_ID, "geo/ore.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(DarkBee bee) {
                return new ResourceLocation(BubbasBees.MOD_ID, "textures/entity/dark_bee.png");
            }

            @Override
            public ResourceLocation getAnimationResource(DarkBee bee) {
                return new ResourceLocation(BubbasBees.MOD_ID, "animations/bee.animation.json");
            }
        });
        this.addLayer(new GlowLayer<>(this));
        this.shadowRadius = 0.15F;
    }

    @Override
    public RenderType getRenderType(DarkBee animatable, float partialTicks, PoseStack stack,
                                    @Nullable MultiBufferSource buffer, @Nullable VertexConsumer vertexBuilder,
                                    int packedLight, ResourceLocation texture) {
        return RenderType.entityTranslucent(texture);
    }
}
