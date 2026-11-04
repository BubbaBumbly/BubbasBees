package net.bubba.bubbasbees;

import com.mojang.logging.LogUtils;
import net.bubba.bubbasbees.client.BeeListTooltip;
import net.bubba.bubbasbees.client.ClientBeeListTooltip;
import net.bubba.bubbasbees.client.HiveInfoOverlay;
import net.bubba.bubbasbees.dispenser.GlassBottleDispenseItemBehavior;
import net.bubba.bubbasbees.dispenser.ShearsDispenseItemBehavior;
import net.bubba.bubbasbees.item.custom.BeeJarItem;
import net.bubba.bubbasbees.registries.*;
import net.bubba.bubbasbees.entity.client.BubbasBeeRenderer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.DispenserBlock;
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
import software.bernie.geckolib3.GeckoLib;

@Mod(BubbasBees.MOD_ID)
public class BubbasBees {
    public static final String MOD_ID = "bubbasbees";
    private static final Logger LOGGER = LogUtils.getLogger();

    public BubbasBees()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModEntityTypes.register(modEventBus);
        ModPOIs.register(modEventBus);
        ModRecipes.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        GeckoLib.initialize();
        MinecraftForge.EVENT_BUS.register(this);

    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModPOIs.registerPOIs();
        });
        DispenserBlock.registerBehavior(Items.SHEARS.asItem(), new ShearsDispenseItemBehavior());
        DispenserBlock.registerBehavior(Items.GLASS_BOTTLE.asItem(), new GlassBottleDispenseItemBehavior());
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                net.minecraft.client.renderer.item.ItemProperties.register(
                        ModItems.BEE_JAR.get(),
                        new ResourceLocation(BubbasBees.MOD_ID, "filled"),
                        (stack, level, entity, seed) -> BeeJarItem.getFillState(stack)
                );
            });
            EntityRenderers.register(ModEntityTypes.BUBBAS_BEE.get(), BubbasBeeRenderer::new);
            ModEntityTypes.bees().values().forEach(bee -> EntityRenderers.register(bee.get(), BubbasBeeRenderer::new));
            EntityRenderers.register(ModEntityTypes.DARK_BEE.get(), net.bubba.bubbasbees.entity.client.DarkBeeRenderer::new);
        }

        @SubscribeEvent
        public static void onRegisterShaders(net.minecraftforge.client.event.RegisterShadersEvent event) throws java.io.IOException {
            net.bubba.bubbasbees.entity.client.StarlightSky.registerShaders(event);
        }

        @SubscribeEvent
        public static void onRegisterRenderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(ModBlockEntities.PEDESTAL.get(), net.bubba.bubbasbees.client.PedestalRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.STARDUST.get(), net.bubba.bubbasbees.client.StardustPileRenderer::new);
        }

        // The stardust pile's textures are drawn by a renderer, not a block model, so add them to the block atlas
        @SubscribeEvent
        public static void onTextureStitch(net.minecraftforge.client.event.TextureStitchEvent.Pre event) {
            if (event.getAtlas().location().equals(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS)) {
                event.addSprite(net.bubba.bubbasbees.client.StardustPileRenderer.TOP);
                event.addSprite(net.bubba.bubbasbees.client.StardustPileRenderer.SIDE);
            }
        }

        @SubscribeEvent
        public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAboveAll(HiveInfoOverlay.ID, new HiveInfoOverlay());
        }

        @SubscribeEvent
        public static void onRegisterTooltipFactories(RegisterClientTooltipComponentFactoriesEvent event) {
            event.register(BeeListTooltip.class, ClientBeeListTooltip::new);
        }
    }
}
