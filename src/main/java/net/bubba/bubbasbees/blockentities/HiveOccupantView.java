package net.bubba.bubbasbees.blockentities;

import java.util.List;

/** Client-visible occupant list of any beehive; implemented on BeehiveBlockEntity by mixin. */
public interface HiveOccupantView {
    String OCCUPANTS_TAG = "BubbasBeeOccupants";
    String ID_TAG = "Id";
    String DUE_TAG = "Due";

    /** Bees inside, in storage order (valid on the client after sync). */
    List<HiveOccupant> bubbasBees$getClientOccupants();

    /** Server side: sends a block entity update to clients if the occupant list changed since the last sync. */
    void bubbasBees$syncOccupantsIfChanged();
}
