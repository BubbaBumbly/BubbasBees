package net.bubba.bubbasbees.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.bubba.bubbasbees.entity.custom.BubbasBee;
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
    }

    // GeckoLib 3 does not scale babies itself; halve the model like vanilla's baby bee
    @Override
    public float getWidthScale(BubbasBee bee) {
        return bee.isBaby() ? 0.5F * super.getWidthScale(bee) : super.getWidthScale(bee);
    }

    @Override
    public float getHeightScale(BubbasBee bee) {
        return bee.isBaby() ? 0.5F * super.getHeightScale(bee) : super.getHeightScale(bee);
    }

    @Override
    public RenderType getRenderType(BubbasBee animatable, float partialTicks, PoseStack stack,
                                    @Nullable MultiBufferSource renderTypeBuffer,
                                    @Nullable VertexConsumer vertexBuilder, int packedLightIn,
                                    ResourceLocation textureLocation) {
        // GeckoLib 3 defaults to entityCutout, which drops partial alpha; see-through bees (the soul bee) blend
        if (animatable.getSpec() != null && animatable.getSpec().translucent()) {
            return RenderType.entityTranslucent(textureLocation);
        }
        return super.getRenderType(animatable, partialTicks, stack,
                renderTypeBuffer, vertexBuilder, packedLightIn, textureLocation);
    }
}
