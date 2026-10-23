package net.bubba.bubbasbees.blockentities;

import java.util.List;

/** Client-visible occupant list and smoke timer of any beehive; implemented on BeehiveBlockEntity by mixin. */
public interface HiveOccupantView {
    String OCCUPANTS_TAG = "BubbasBeeOccupants";
    String ID_TAG = "Id";
    String DUE_TAG = "Due";
    String SMOKE_END_TAG = "SmokeEnd";
    long NOT_SMOKED = -1L;

    /** Bees inside, in storage order (valid on the client after sync). */
    List<HiveOccupant> bubbasBees$getClientOccupants();

    /** Game time at which the hive's smoke wears off, or NOT_SMOKED (valid on the client after sync). */
    long bubbasBees$getClientSmokeEnd();

    /** Server side: sends a block entity update to clients if the occupant list changed since the last sync. */
    void bubbasBees$syncOccupantsIfChanged();
}
