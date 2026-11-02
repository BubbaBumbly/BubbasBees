package net.bubba.bubbasbees.entity.client;

import net.bubba.bubbasbees.BubbasBees;
import net.bubba.bubbasbees.bees.BeeSpec;
import net.bubba.bubbasbees.entity.custom.BubbasBee;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;


public class BeeModel extends AnimatedGeoModel<BubbasBee> {

    @Override
    public ResourceLocation getModelResource(BubbasBee instance) {
        BeeSpec spec = instance.getSpec();
        return new ResourceLocation(BubbasBees.MOD_ID, "geo/" + (spec == null ? "ore" : spec.model()) + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BubbasBee instance) {
        BeeSpec spec = instance.getSpec();
        String name = spec == null ? "diamond" : spec.id();   // the dev-only Bubbas Bee wears the diamond look
        if ((spec == null || spec.deepslateLook()) && instance.isDeepslate()) {
            name += "_deepslate";
        }
        return new ResourceLocation(BubbasBees.MOD_ID,
                "textures/entity/" + name + (instance.isAngry() ? "_bee_angry.png" : "_bee.png"));
    }

    @Override
    public ResourceLocation getAnimationResource(BubbasBee animatable) {
        return new ResourceLocation(BubbasBees.MOD_ID, "animations/bee.animation.json");
    }
}
