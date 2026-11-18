package net.bubba.bubbasbees.registries;

import com.google.common.collect.ImmutableSet;
import net.bubba.bubbasbees.BubbasBees;
import net.bubba.bubbasbees.util.Reg;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PointOfInterestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.village.poi.PoiType;

public class ModPOIs {
    private static ResourceLocation rl(String name) {
        return ResourceLocation.fromNamespaceAndPath(BubbasBees.MOD_ID, name);
    }

    public static final Reg<PoiType> ADVANCED_BEEHIVE_POI = Reg.of(rl("advanced_beehive_poi"));

    public static void register() {
        // Plain Registry.register doesn't add the block states to vanilla's POI state map; this helper does
        ADVANCED_BEEHIVE_POI.set(PointOfInterestHelper.register(ADVANCED_BEEHIVE_POI.id(), 1, 1,
                ModBlocks.HIVES.stream()
                        .flatMap(hive -> hive.get().getStateDefinition().getPossibleStates().stream())
                        .collect(ImmutableSet.toImmutableSet())));
    }
}
