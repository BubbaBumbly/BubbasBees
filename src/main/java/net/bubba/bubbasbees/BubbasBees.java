package net.bubba.bubbasbees;

// DEVIATION: NeoForge 1.21 has no TextureStitchEvent.Pre; the stardust sprites are added to the block atlas by the data-driven
// atlases/blocks.json source file instead of onTextureStitch.
import com.mojang.logging.LogUtils;
import net.bubba.bubbasbees.dispenser.GlassBottleDispenseItemBehavior;
import net.bubba.bubbasbees.dispenser.ShearsDispenseItemBehavior;
import net.bubba.bubbasbees.item.ModCreativeModeTab;
import net.bubba.bubbasbees.registries.BeeEvents;
import net.bubba.bubbasbees.registries.ModBlockEntities;
import net.bubba.bubbasbees.registries.ModBlocks;
import net.bubba.bubbasbees.registries.ModDataComponents;
import net.bubba.bubbasbees.registries.ModEntityTypes;
import net.bubba.bubbasbees.registries.ModEvents;
import net.bubba.bubbasbees.registries.ModItems;
import net.bubba.bubbasbees.registries.ModPOIs;
import net.bubba.bubbasbees.registries.ModRecipes;
import net.bubba.bubbasbees.ritual.RitualManager;
import net.bubba.bubbasbees.util.BubbasAttachments;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.DispenserBlock;
import org.slf4j.Logger;

public class BubbasBees implements ModInitializer {
    public static final String MOD_ID = "bubbasbees";
    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        // DEVIATION: entity types register before items so the spawn eggs can resolve their entity type eagerly.
        ModBlocks.register();
        ModBlockEntities.register();
        ModEntityTypes.register();
        ModPOIs.register();
        ModItems.register();
        ModCreativeModeTab.register();
        ModRecipes.register();
        ModDataComponents.register();

        BubbasAttachments.init();
        ModEvents.registerAttributes();
        BeeEvents.register();
        RitualManager.register();

        // Dispenser behavior map is not thread-safe; Fabric runs this on the main thread
        DispenserBlock.registerBehavior(Items.SHEARS, new ShearsDispenseItemBehavior());
        DispenserBlock.registerBehavior(Items.GLASS_BOTTLE, new GlassBottleDispenseItemBehavior());

        // Fabric Transfer API: the comb chute's item storage and the partial lava cauldron's fluid storage
        ItemStorage.SIDED.registerForBlockEntity((chute, side) -> chute.getExternalStorage(), ModBlockEntities.COMB_CHUTE.get());
        FluidStorage.SIDED.registerForBlockEntity((cauldron, side) -> cauldron.getFluidStorage(), ModBlockEntities.PARTIAL_LAVA_CAULDRON.get());
    }
}
