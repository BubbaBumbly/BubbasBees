package net.bubba.bubbasbees.blocks;

import java.util.List;

/**
 * The woods advanced beehives come in. Each wood has a plain hive and a reinforced one; a modded
 * wood's hives only exist when that mod is installed. Ids match the generated assets and data:
 * oak is the default hive, "advanced_beehive".
 */
public record HiveWood(String id, String modid) {

    // Only the vanilla woods; the 1.19.2 modded-wood hives are dropped in this port.
    public static final List<HiveWood> ALL = List.of(
            vanilla("oak"), vanilla("spruce"), vanilla("birch"), vanilla("jungle"), vanilla("acacia"),
            vanilla("dark_oak"), vanilla("mangrove"), vanilla("crimson"), vanilla("warped"));

    private static HiveWood vanilla(String wood) {
        return new HiveWood(wood, "minecraft");
    }

    public boolean isAvailable() {
        return true;
    }

    /** "advanced_beehive" for oak, "<wood>_advanced_beehive" otherwise. */
    public String plainName() {
        return id.equals("oak") ? "advanced_beehive" : id + "_advanced_beehive";
    }

    public String reinforcedName() {
        return "reinforced_" + plainName();
    }
}
