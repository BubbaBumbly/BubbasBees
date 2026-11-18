package net.bubba.bubbasbees.registries;

import com.google.common.collect.ImmutableSet;
import net.bubba.bubbasbees.BubbasBees;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModPOIs {

    public static final DeferredRegister<PoiType> POI_TYPES =
            DeferredRegister.create(ForgeRegistries.POI_TYPES, BubbasBees.MOD_ID);

    // Forge registers the matching block states for POI types on registry add (GameData.PointOfInterestTypeCallbacks)
    public static final RegistryObject<PoiType> ADVANCED_BEEHIVE_POI = POI_TYPES.register("advanced_beehive_poi",
            () -> new PoiType(ModBlocks.HIVES.stream()
                    .flatMap(hive -> hive.get().getStateDefinition().getPossibleStates().stream())
                    .collect(ImmutableSet.toImmutableSet()), 1, 1));

    public static void register(IEventBus eventBus) {
        POI_TYPES.register(eventBus);
    }
}
