package net.bubba.bubbasbees.registries;

import net.bubba.bubbasbees.bees.BeeSpec;
import net.bubba.bubbasbees.bees.BeeSpecs;
import net.bubba.bubbasbees.entity.custom.BubbasBee;
import net.bubba.bubbasbees.entity.custom.DarkBee;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

/** Entity attributes, registered from the main entrypoint after the entity types exist. */
public final class ModEvents {
    private ModEvents() {
    }

    public static void registerAttributes() {
        FabricDefaultAttributeRegistry.register(ModEntityTypes.BUBBAS_BEE.get(), BubbasBee.setAttributes());
        for (BeeSpec spec : BeeSpecs.ALL) {
            FabricDefaultAttributeRegistry.register(ModEntityTypes.bee(spec.id()).get(), BubbasBee.setAttributes(spec.maxHealth()));
        }
        FabricDefaultAttributeRegistry.register(ModEntityTypes.DARK_BEE.get(), DarkBee.setAttributes());
    }
}
