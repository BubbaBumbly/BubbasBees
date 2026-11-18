package net.bubba.bubbasbees.client;

// DEVIATION: vanilla EntityRenderers.register is private; Fabric's EntityRendererRegistry is used instead.
import net.bubba.bubbasbees.BubbasBees;
import net.bubba.bubbasbees.client.guide.GuideBook;
import net.bubba.bubbasbees.client.guide.GuideVisualizer;
import net.bubba.bubbasbees.client.model.HiveModelLoadingPlugin;
import net.bubba.bubbasbees.entity.client.BubbasBeeRenderer;
import net.bubba.bubbasbees.entity.client.DarkBeeRenderer;
import net.bubba.bubbasbees.entity.client.GlowLayer;
import net.bubba.bubbasbees.entity.client.StarlightSky;
import net.bubba.bubbasbees.item.custom.BeeJarItem;
import net.bubba.bubbasbees.registries.ModBlockEntities;
import net.bubba.bubbasbees.registries.ModBlocks;
import net.bubba.bubbasbees.registries.ModEntityTypes;
import net.bubba.bubbasbees.registries.ModItems;
import net.bubba.bubbasbees.util.Reg;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.level.block.Block;

public class BubbasBeesClient implements ClientModInitializer {
    private static final HiveInfoOverlay HIVE_INFO_OVERLAY = new HiveInfoOverlay();
    private static final GuideVisualizer.Overlay GUIDE_OVERLAY = new GuideVisualizer.Overlay();

    @Override
    public void onInitializeClient() {
        // FIX: hives keep the default solid layer; the model plugin splits glass panes into translucent quads.
        ModelLoadingPlugin.register(new HiveModelLoadingPlugin());
        JellyCoreHider.register();

        ItemProperties.register(ModItems.BEE_JAR.get(),
                ResourceLocation.fromNamespaceAndPath(BubbasBees.MOD_ID, "filled"),
                (stack, level, entity, seed) -> BeeJarItem.getFillState(stack));

        EntityRendererRegistry.register(ModEntityTypes.BUBBAS_BEE.get(), BubbasBeeRenderer::new);
        ModEntityTypes.bees().values().forEach(bee -> EntityRendererRegistry.register(bee.get(), BubbasBeeRenderer::new));
        EntityRendererRegistry.register(ModEntityTypes.DARK_BEE.get(), DarkBeeRenderer::new);

        BlockEntityRenderers.register(ModBlockEntities.PEDESTAL.get(), PedestalRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.STARDUST.get(), StardustPileRenderer::new);

        HudRenderCallback.EVENT.register((graphics, deltaTracker) -> HIVE_INFO_OVERLAY.render(graphics, deltaTracker));
        HudRenderCallback.EVENT.register((graphics, deltaTracker) -> GUIDE_OVERLAY.render(graphics, deltaTracker));

        CoreShaderRegistrationCallback.EVENT.register(StarlightSky::registerShaders);

        TooltipComponentCallback.EVENT.register(data -> data instanceof BeeListTooltip bees ? new ClientBeeListTooltip(bees) : null);

        // The Encyclobeedia's pages are client resources (assets/bubbasbees/encyclobeedia), reloaded with F3+T
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new GuideBook.Loader());
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new GlowCacheReloader());

        GuideVisualizer.register();

        // FIX: Fabric ignores the NeoForge "render_type" model key, so the cutout/translucent layers are set here.
        // FIX: the four pedestals share the cutout pedestal model; the celestial altar's model is cutout too.
        for (Reg<Block> pedestal : ModBlocks.PEDESTALS) BlockRenderLayerMap.INSTANCE.putBlock(pedestal.get(), RenderType.cutout());
        // FIX: every stardust model variant (dot, arm, hill) is a blockstate variant of the single stardust block.
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.STARDUST.get(), RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.ROYAL_JELLY_BLOCK.get(), RenderType.translucent());
    }

    private static final class GlowCacheReloader implements IdentifiableResourceReloadListener, ResourceManagerReloadListener {
        @Override
        public ResourceLocation getFabricId() {
            return ResourceLocation.fromNamespaceAndPath(BubbasBees.MOD_ID, "glow_layer_cache");
        }

        @Override
        public void onResourceManagerReload(ResourceManager manager) {
            GlowLayer.clearCache();
        }
    }
}
