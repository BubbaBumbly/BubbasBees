package net.bubba.bubbasbees.blockentities;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Client-visible occupant list of any beehive; implemented on BeehiveBlockEntity by mixin. */
public interface HiveOccupantView {
    String OCCUPANTS_TAG = "BubbasBeeOccupants";

    /** Entity type ids of the bees inside, in storage order (valid on the client after sync). */
    List<ResourceLocation> bubbasBees$getClientOccupants();

    /** Server side: sends a block entity update to clients if the occupant list changed since the last sync. */
    void bubbasBees$syncOccupantsIfChanged();
}
