package net.bubba.bubbasbees.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.bubba.bubbasbees.BubbasBees;
import net.bubba.bubbasbees.entity.custom.BubbasBee;
import net.bubba.bubbasbees.entity.custom.CopperBee;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib3.model.AnimatedGeoModel;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

import java.util.Objects;

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
    public ResourceLocation getTextureLocation(BubbasBee instance) {
        switch (instance.getVariant()) {
            case "copper":
                if (instance.isAngry()){
                    return new ResourceLocation(BubbasBees.MOD_ID, "textures/entity/copper_bee_angry.png");
                } else {
                    return new ResourceLocation(BubbasBees.MOD_ID, "textures/entity/copper_bee.png");
                }
            case "sand":
                if (instance.isAngry()){
                    return new ResourceLocation(BubbasBees.MOD_ID, "textures/entity/sandy_bee_angry.png");
                } else {
                    return new ResourceLocation(BubbasBees.MOD_ID, "textures/entity/sandy_bee.png");
                }
            case "gold":
                if (instance.isAngry()){
                    return new ResourceLocation(BubbasBees.MOD_ID, "textures/entity/gold_bee_angry.png");
                } else {
                    return new ResourceLocation(BubbasBees.MOD_ID, "textures/entity/gold_bee.png");
                }
            case "iron":
                if (instance.isAngry()){
                    return new ResourceLocation(BubbasBees.MOD_ID, "textures/entity/iron_bee_angry.png");
                } else {
                    return new ResourceLocation(BubbasBees.MOD_ID, "textures/entity/iron_bee.png");
                }
            case "nature":
                if (instance.isAngry()){
                    return new ResourceLocation(BubbasBees.MOD_ID, "textures/entity/nature_bee_angry.png");
                } else {
                    return new ResourceLocation(BubbasBees.MOD_ID, "textures/entity/nature_bee.png");
                }
            case "redstone":
                if (instance.isAngry()){
                    return new ResourceLocation(BubbasBees.MOD_ID, "textures/entity/redstone_bee_angry.png");
                } else {
                    return new ResourceLocation(BubbasBees.MOD_ID, "textures/entity/redstone_bee.png");
                }
            case "ender":
                if (instance.isAngry()){
                    return new ResourceLocation(BubbasBees.MOD_ID, "textures/entity/ender_bee_angry.png");
                } else {
                    return new ResourceLocation(BubbasBees.MOD_ID, "textures/entity/ender_bee.png");
                }
            case "stone":
                if (instance.isAngry()){
                    return new ResourceLocation(BubbasBees.MOD_ID, "textures/entity/stone_bee_angry.png");
                } else {
                    return new ResourceLocation(BubbasBees.MOD_ID, "textures/entity/stone_bee.png");
                }
            case "none":
                if (instance.isAngry()){
                    return new ResourceLocation(BubbasBees.MOD_ID, "textures/entity/diamond_bee_angry.png");
                } else {
                    return new ResourceLocation(BubbasBees.MOD_ID, "textures/entity/diamond_bee.png");
                }
            default:
                return new ResourceLocation(BubbasBees.MOD_ID, "textures/entity/diamond_bee_angry.png");
        }
    }

    @Override
    public RenderType getRenderType(BubbasBee animatable, float partialTicks, PoseStack stack,
                                    @Nullable MultiBufferSource renderTypeBuffer,
                                    @Nullable VertexConsumer vertexBuilder, int packedLightIn,
                                    ResourceLocation textureLocation) {
        return super.getRenderType(animatable, partialTicks, stack,
                renderTypeBuffer, vertexBuilder, packedLightIn, textureLocation);
    }
}
