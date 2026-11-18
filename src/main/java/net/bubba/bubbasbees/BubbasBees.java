package net.bubba.bubbasbees;

// DEVIATION: NeoForge 1.21 has no TextureStitchEvent.Pre; the stardust sprites are added to the block atlas by the data-driven
// atlases/blocks.json source file instead of onTextureStitch.
import com.mojang.logging.LogUtils;
import net.bubba.bubbasbees.dispenser.GlassBottleDispenseItemBehavior;
import net.bubba.bubbasbees.dispenser.ShearsDispenseItemBehavior;
import net.bubba.bubbasbees.client.BeeListTooltip;
import net.bubba.bubbasbees.client.ClientBeeListTooltip;
import net.bubba.bubbasbees.client.HiveInfoOverlay;
import net.bubba.bubbasbees.entity.client.BubbasBeeRenderer;
import net.bubba.bubbasbees.item.ModCreativeModeTab;
import net.bubba.bubbasbees.item.custom.BeeJarItem;
import net.bubba.bubbasbees.registries.*;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.DispenserBlock;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

@Mod(BubbasBees.MOD_ID)
public class BubbasBees {
    public static final String MOD_ID = "bubbasbees";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BubbasBees(IEventBus modEventBus)
    {
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModEntityTypes.register(modEventBus);
        ModPOIs.register(modEventBus);
        ModCreativeModeTab.register(modEventBus);
        ModRecipes.register(modEventBus);
        ModDataComponents.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        // NeoForge capabilities: the comb chute's item handler and the partial lava cauldron's fluid handler
        modEventBus.addListener(BubbasBees::registerCapabilities);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Dispenser behavior map is not thread-safe; register on the main thread
        event.enqueueWork(() -> {
            DispenserBlock.registerBehavior(Items.SHEARS, new ShearsDispenseItemBehavior());
            DispenserBlock.registerBehavior(Items.GLASS_BOTTLE, new GlassBottleDispenseItemBehavior());
        });
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.COMB_CHUTE.get(),
                (chute, side) -> chute.getExternalHandler());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.PARTIAL_LAVA_CAULDRON.get(),
                (cauldron, side) -> cauldron.getFluidHandler());
    }

    @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = net.neoforged.api.distmarker.Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> ItemProperties.register(
                    ModItems.BEE_JAR.get(),
                    ResourceLocation.fromNamespaceAndPath(BubbasBees.MOD_ID, "filled"),
                    (stack, level, entity, seed) -> BeeJarItem.getFillState(stack)
            ));
        }

        @SubscribeEvent
        public static void onRegisterShaders(RegisterShadersEvent event) throws java.io.IOException {
            net.bubba.bubbasbees.entity.client.StarlightSky.registerShaders(event);
        }

        // The stardust pile's textures are drawn by a renderer; the atlases/blocks.json source adds them to the block atlas

        @SubscribeEvent
        public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
            event.registerAboveAll(HiveInfoOverlay.ID, new HiveInfoOverlay());
            event.registerAboveAll(net.bubba.bubbasbees.client.guide.GuideVisualizer.OVERLAY_ID,
                    new net.bubba.bubbasbees.client.guide.GuideVisualizer.Overlay());
        }

        // The Encyclobeedia's pages are client resources (assets/bubbasbees/encyclobeedia), reloaded with F3+T
        @SubscribeEvent
        public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener(new net.bubba.bubbasbees.client.guide.GuideBook.Loader());
            event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)
                    manager -> net.bubba.bubbasbees.entity.client.GlowLayer.clearCache());
        }

        @SubscribeEvent
        public static void onRegisterTooltipFactories(RegisterClientTooltipComponentFactoriesEvent event) {
            event.register(BeeListTooltip.class, ClientBeeListTooltip::new);
        }

        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntityTypes.BUBBAS_BEE.get(), BubbasBeeRenderer::new);
            ModEntityTypes.bees().values().forEach(bee -> event.registerEntityRenderer(bee.get(), BubbasBeeRenderer::new));
            event.registerEntityRenderer(ModEntityTypes.DARK_BEE.get(), net.bubba.bubbasbees.entity.client.DarkBeeRenderer::new);

            event.registerBlockEntityRenderer(ModBlockEntities.PEDESTAL.get(), net.bubba.bubbasbees.client.PedestalRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.STARDUST.get(), net.bubba.bubbasbees.client.StardustPileRenderer::new);
        }
    }
}
