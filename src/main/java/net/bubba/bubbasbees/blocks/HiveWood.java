package net.bubba.bubbasbees.blocks;

import net.minecraftforge.fml.ModList;

import java.util.List;

/**
 * The woods advanced beehives come in. Each wood has a plain hive and a reinforced one; a modded
 * wood's hives only exist when that mod is installed. Ids match the generated assets and data
 * (BubbasBeesArt/work/hive/export_mod.py): oak is the default hive, "advanced_beehive".
 */
public record HiveWood(String id, String modid) {

    public static final List<HiveWood> ALL = List.of(
            vanilla("oak"), vanilla("spruce"), vanilla("birch"), vanilla("jungle"), vanilla("acacia"),
            vanilla("dark_oak"), vanilla("mangrove"), vanilla("crimson"), vanilla("warped"),
            modded("biomesoplenty", "cherry"), modded("biomesoplenty", "dead"), modded("biomesoplenty", "fir"),
            modded("biomesoplenty", "hellbark"), modded("biomesoplenty", "jacaranda"), modded("biomesoplenty", "magic"),
            modded("biomesoplenty", "mahogany"), modded("biomesoplenty", "palm"), modded("biomesoplenty", "redwood"),
            modded("biomesoplenty", "umbran"), modded("biomesoplenty", "willow"),
            modded("quark", "ancient"), modded("quark", "azalea"), modded("quark", "bamboo"), modded("quark", "blossom"),
            modded("windswept", "chestnut"), modded("windswept", "holly"),
            modded("phantasm", "ebony"), modded("phantasm", "pream"));

    private static HiveWood vanilla(String wood) {
        return new HiveWood(wood, "minecraft");
    }

    private static HiveWood modded(String modid, String wood) {
        return new HiveWood(modid + "_" + wood, modid);
    }

    public boolean isAvailable() {
        return modid.equals("minecraft") || ModList.get().isLoaded(modid);
    }

    /** "advanced_beehive" for oak, "<wood>_advanced_beehive" otherwise. */
    public String plainName() {
        return id.equals("oak") ? "advanced_beehive" : id + "_advanced_beehive";
    }

    public String reinforcedName() {
        return "reinforced_" + plainName();
    }
}
