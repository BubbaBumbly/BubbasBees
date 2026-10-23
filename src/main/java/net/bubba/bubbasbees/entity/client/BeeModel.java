package net.bubba.bubbasbees.entity.client;

import net.bubba.bubbasbees.BubbasBees;
import net.bubba.bubbasbees.entity.custom.BubbasBee;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

import java.util.Set;

public class BeeModel extends AnimatedGeoModel<BubbasBee> {
    // Variants with a deepslate look (textures/entity/<name>_deepslate_bee.png)
    private static final Set<String> DEEPSLATE_VARIANTS = Set.of("copper", "gold", "iron", "redstone", "stone", "none");

    @Override
    public ResourceLocation getModelResource(BubbasBee instance) {
        return switch (instance.getVariant()) {
            case "nature" -> new ResourceLocation(BubbasBees.MOD_ID, "geo/nature.geo.json");   // grass on top
            case "sand" -> new ResourceLocation(BubbasBees.MOD_ID, "geo/sandy.geo.json");      // cacti on top
            default -> new ResourceLocation(BubbasBees.MOD_ID, "geo/ore.geo.json");
        };
    }

    @Override
    public ResourceLocation getTextureResource(BubbasBee instance) {
        String variant = instance.getVariant();
        String name = switch (variant) {
            case "sand" -> "sandy";
            case "none" -> "diamond";    // the dev-only Bubbas Bee
            default -> variant;
        };
        if (DEEPSLATE_VARIANTS.contains(variant) && instance.isDeepslate()) {
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
