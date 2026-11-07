package net.bubba.bubbasbees.client;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.List;

/**
 * Fits a ritual boss bar's title to the screen. The server builds the full title (Ritual.refreshBar: the name, then
 * " · " + part pairs) without knowing anyone's screen; when it is wider than this player's screen, the list of missing
 * bee names is left out and the "Types n/8" count stays. Other boss bars are untouched.
 */
public final class RitualBarTitle {
    private static final String RITUAL_PREFIX = "ritual.bubbasbees.";
    private static final String MISSING_TYPES = "ritual.bubbasbees.missing_types";
    private static final int MARGIN = 4;

    private RitualBarTitle() {}

    public static Component fit(Component title, Font font, int screenWidth) {
        if (!isKey(title, null) || font.width(title) <= screenWidth - MARGIN) return title;
        List<Component> parts = title.getSiblings();
        MutableComponent shortTitle = title.plainCopy().setStyle(title.getStyle());
        boolean dropped = false;
        for (int i = 0; i < parts.size(); i++) {
            // separator at i, part at i + 1: skip both for the missing-names part
            if (i + 1 < parts.size() && isKey(parts.get(i + 1), MISSING_TYPES)) {
                i++;
                dropped = true;
                continue;
            }
            shortTitle.append(parts.get(i));
        }
        return dropped ? shortTitle : title;
    }

    /** Whether the component is a translation with this key (null: any ritual.bubbasbees.* key). */
    private static boolean isKey(Component component, String key) {
        if (!(component.getContents() instanceof TranslatableContents contents)) return false;
        return key == null ? contents.getKey().startsWith(RITUAL_PREFIX) : contents.getKey().equals(key);
    }
}
