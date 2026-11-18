package net.bubba.bubbasbees.client.model;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.resources.ResourceLocation;

/**
 * Wraps every advanced-hive block model so the flattened wood and glass quads can use different blend modes.
 */
public class HiveModelLoadingPlugin implements ModelLoadingPlugin {
    private static final String NAMESPACE = "bubbasbees";
    private static final String MODEL_DIRECTORY = "block/";
    private static final String HIVE_MODEL = "advanced_beehive";

    @Override
    public void onInitializeModelLoader(Context pluginContext) {
        // FIX: the flattened hive models cannot declare a render type, so each baked model is wrapped here.
        pluginContext.modifyModelAfterBake().register((model, context) -> {
            if (model != null && isHiveModel(context.resourceId())) {
                return new HiveGlassBakedModel(model);
            }
            return model;
        });
    }

    private static boolean isHiveModel(ResourceLocation id) {
        return id != null && NAMESPACE.equals(id.getNamespace())
                && id.getPath().startsWith(MODEL_DIRECTORY)
                && id.getPath().contains(HIVE_MODEL);
    }
}
