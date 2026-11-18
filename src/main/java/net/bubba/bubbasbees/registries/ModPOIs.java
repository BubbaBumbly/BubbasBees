package net.bubba.bubbasbees.registries;

import com.google.common.collect.ImmutableSet;
import net.bubba.bubbasbees.BubbasBees;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModPOIs {

    public static final DeferredRegister<PoiType> POI_TYPES =
            DeferredRegister.create(BuiltInRegistries.POINT_OF_INTEREST_TYPE, BubbasBees.MOD_ID);

    // NeoForge registers the matching block states for POI types on registry add, as Forge did
    public static final DeferredHolder<PoiType, PoiType> ADVANCED_BEEHIVE_POI = POI_TYPES.register("advanced_beehive_poi",
            () -> new PoiType(ModBlocks.HIVES.stream()
                    .flatMap(hive -> hive.get().getStateDefinition().getPossibleStates().stream())
                    .collect(ImmutableSet.toImmutableSet()), 1, 1));

    public static void register(IEventBus eventBus) {
        POI_TYPES.register(eventBus);
    }
}
