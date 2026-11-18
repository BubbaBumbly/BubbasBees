package net.bubba.bubbasbees.item;

import net.bubba.bubbasbees.BubbasBees;
import net.bubba.bubbasbees.registries.ModBlocks;
import net.bubba.bubbasbees.registries.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeModeTab {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BubbasBees.MOD_ID);

    public static final RegistryObject<CreativeModeTab> BUBBASBEES_TAB = CREATIVE_MODE_TABS.register("bubbasbeestab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.bubbasbeestab"))
                    .icon(() -> new ItemStack(ModBlocks.ADVANCED_BEEHIVE.get()))
                    .displayItems((parameters, output) -> {
                        // Getting started, then hives (plain and reinforced paired per wood), comb chute, stars,
                        // gems by tier, ritual blocks, royal jelly things, combs, spawn eggs.
                        output.accept(ModItems.ENCYCLOBEEDIA.get());
                        output.accept(ModItems.COPPER_SMOKER.get());
                        output.accept(ModItems.SMOKER.get());
                        output.accept(ModItems.BEE_JAR.get());
                        for (RegistryObject<Block> hive : ModBlocks.HIVES) output.accept(hive.get().asItem());
                        output.accept(ModItems.COMB_CHUTE.get());
                        output.accept(ModItems.LESSER_BUBBA_STAR.get());
                        output.accept(ModItems.BUBBA_STAR.get());
                        output.accept(ModItems.CELESTIAL_BUBBA_STAR.get());
                        for (Item gem : ModItems.allGems()) output.accept(gem);
                        output.accept(ModBlocks.CELESTIAL_ALTAR.get());
                        output.accept(ModBlocks.STONE_PEDESTAL.get());
                        output.accept(ModBlocks.DEEPSLATE_PEDESTAL.get());
                        output.accept(ModBlocks.BLACKSTONE_PEDESTAL.get());
                        output.accept(ModBlocks.QUARTZ_PEDESTAL.get());
                        output.accept(ModItems.STARDUST.get());
                        output.accept(ModItems.ROYAL_JELLY.get());
                        output.accept(ModBlocks.ROYAL_JELLY_BLOCK.get());
                        output.accept(ModBlocks.PINK_STICKY_PISTON.get());
                        for (Item comb : ModItems.allCombs()) output.accept(comb);
                        for (Item egg : ModItems.allSpawnEggs()) output.accept(egg);
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
