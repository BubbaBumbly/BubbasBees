package net.bubba.bubbasbees.util;

import net.bubba.bubbasbees.BubbasBees;
import net.bubba.bubbasbees.blocks.HiveWood;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.forgespi.locating.IModFile;
import net.minecraftforge.resource.PathPackResources;

import java.nio.file.Path;

/**
 * Adds the jar's compat/&lt;modid&gt;/ folder as an always-on data pack while that wood mod is loaded. It holds the
 * mod's hive recipes, loot tables and tag entries: 1.19.2 loot tables can't carry a mod_loaded condition, so they
 * failed to parse (with an error per table) whenever their hive wasn't registered.
 */
@Mod.EventBusSubscriber(modid = BubbasBees.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CompatPacks {

    @SubscribeEvent
    public static void addPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) return;
        IModFile modFile = ModList.get().getModFileById(BubbasBees.MOD_ID).getFile();
        HiveWood.ALL.stream()
                .filter(wood -> !wood.modid().equals("minecraft") && wood.isAvailable())
                .map(HiveWood::modid)
                .distinct()
                .forEach(modid -> {
                    Path path = modFile.findResource("compat", modid);
                    event.addRepositorySource((consumer, constructor) -> {
                        Pack pack = Pack.create(BubbasBees.MOD_ID + "_" + modid, true,
                                () -> new PathPackResources(modFile.getFileName() + ":compat/" + modid, path),
                                constructor, Pack.Position.TOP, PackSource.BUILT_IN);
                        if (pack != null) consumer.accept(pack);
                    });
                });
    }
}
