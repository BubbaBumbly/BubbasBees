package net.bubba.bubbasbees;

// DEVIATION: Forge 1.20.1 removed TextureStitchEvent.Pre; the stardust sprites are added to the block atlas by the data-driven
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
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(BubbasBees.MOD_ID)
public class BubbasBees {
    public static final String MOD_ID = "bubbasbees";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BubbasBees()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModEntityTypes.register(modEventBus);
        ModPOIs.register(modEventBus);
        ModCreativeModeTab.register(modEventBus);
        ModRecipes.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Dispenser behavior map is not thread-safe; register on the main thread
        event.enqueueWork(() -> {
            DispenserBlock.registerBehavior(Items.SHEARS, new ShearsDispenseItemBehavior());
            DispenserBlock.registerBehavior(Items.GLASS_BOTTLE, new GlassBottleDispenseItemBehavior());
        });
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> ItemProperties.register(
                    ModItems.BEE_JAR.get(),
                    new ResourceLocation(BubbasBees.MOD_ID, "filled"),
                    (stack, level, entity, seed) -> BeeJarItem.getFillState(stack)
            ));
        }

        @SubscribeEvent
        public static void onRegisterShaders(net.minecraftforge.client.event.RegisterShadersEvent event) throws java.io.IOException {
            net.bubba.bubbasbees.entity.client.StarlightSky.registerShaders(event);
        }

        // The stardust pile's textures are drawn by a renderer; the atlases/blocks.json source adds them to the block atlas

        @SubscribeEvent
        public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAboveAll(HiveInfoOverlay.ID, new HiveInfoOverlay());
            event.registerAboveAll(net.bubba.bubbasbees.client.guide.GuideVisualizer.OVERLAY_ID,
                    new net.bubba.bubbasbees.client.guide.GuideVisualizer.Overlay());
        }

        // The Encyclobeedia's pages are client resources (assets/bubbasbees/encyclobeedia), reloaded with F3+T
        @SubscribeEvent
        public static void onRegisterReloadListeners(net.minecraftforge.client.event.RegisterClientReloadListenersEvent event) {
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
