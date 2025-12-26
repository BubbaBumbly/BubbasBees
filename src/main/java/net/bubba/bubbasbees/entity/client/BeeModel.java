package net.bubba.bubbasbees.entity.client;

import net.bubba.bubbasbees.BubbasBees;
import net.bubba.bubbasbees.entity.custom.BubbasBee;
import net.bubba.bubbasbees.entity.custom.CopperBee;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

import java.util.Objects;

public class BeeModel extends AnimatedGeoModel<BubbasBee> {
    @Override
    public ResourceLocation getModelResource(BubbasBee instance) {
        if (Objects.equals(instance.getVariant(), "sand")){
            return new ResourceLocation(BubbasBees.MOD_ID, "geo/base.geo.json");
        } else {
            return new ResourceLocation(BubbasBees.MOD_ID, "geo/ore.geo.json");
        }
    }

    @Override
    public ResourceLocation getTextureResource(BubbasBee instance) {
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
    public ResourceLocation getAnimationResource(BubbasBee animatable) {
        return new ResourceLocation(BubbasBees.MOD_ID, "animations/bee.animation.json");
    }
}
