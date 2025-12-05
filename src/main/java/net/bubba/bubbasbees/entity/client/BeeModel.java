package net.bubba.bubbasbees.entity.client;

import net.bubba.bubbasbees.BubbasBees;
import net.bubba.bubbasbees.entity.custom.BubbasBee;
import net.bubba.bubbasbees.entity.custom.CopperBee;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

import java.util.Objects;

public class BeeModel extends AnimatedGeoModel<BubbasBee> {
    @Override
    public ResourceLocation getModelResource(BubbasBee object) {
        return new ResourceLocation(BubbasBees.MOD_ID, "geo/ore.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BubbasBee instance) {
        if (Objects.equals(instance.getVariant(), "copper"))  {
            return new ResourceLocation(BubbasBees.MOD_ID, "textures/entity/copper_bee.png");
        } else {
            return new ResourceLocation(BubbasBees.MOD_ID, "textures/entity/diamond_bee.png");
        }
    }

    @Override
    public ResourceLocation getAnimationResource(BubbasBee animatable) {
        return new ResourceLocation(BubbasBees.MOD_ID, "animations/bee.animation.json");
    }
}
