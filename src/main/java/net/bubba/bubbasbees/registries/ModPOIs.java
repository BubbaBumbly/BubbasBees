package net.bubba.bubbasbees.registries;

import com.google.common.collect.ImmutableSet;
import com.mojang.logging.LogUtils;
import net.bubba.bubbasbees.BubbasBees;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import org.slf4j.Logger;

import java.lang.reflect.InvocationTargetException;

public class ModPOIs {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<PoiType> POI_TYPES =
            DeferredRegister.create(ForgeRegistries.POI_TYPES, BubbasBees.MOD_ID);

    public static final RegistryObject<PoiType> ADVANCED_BEEHIVE_POI = POI_TYPES.register("advanced_beehive_poi",
            () -> new PoiType(ModBlocks.HIVES.stream()
                    .flatMap(hive -> hive.get().getStateDefinition().getPossibleStates().stream())
                    .collect(ImmutableSet.toImmutableSet()), 1, 1));

    public static void registerPOIs() {
        try {
            ObfuscationReflectionHelper.findMethod(PoiType.class,
                    "registerBlockStates", PoiType.class).invoke(null, ADVANCED_BEEHIVE_POI.get());
        } catch (InvocationTargetException | IllegalAccessException exception) {
            LOGGER.error("Could not register the advanced beehive POI block states", exception);
        }
    }
    public static void register(IEventBus eventBus) {
        POI_TYPES.register(eventBus);
    }
}
