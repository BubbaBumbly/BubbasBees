package net.bubba.bubbasbees.client;

// DEVIATION: the hive tooltip is injected by ItemStackTooltipMixin instead of ItemTooltipCallback + a component callback.
import net.bubba.bubbasbees.blockentities.HiveOccupant;
import net.bubba.bubbasbees.util.HiveInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Adds honey level and stored bees to the tooltip of any hive item hovered in an inventory (see ItemStackTooltipMixin). */
public final class HiveTooltipHandler {
    private HiveTooltipHandler() {
    }

    /** Inserts the honey-level line under an item's name. */
    public static void addHiveLine(ItemStack stack, List<Component> lines) {
        if (!HiveInfo.isHiveItem(stack)) return;
        List<HiveOccupant> bees = HiveInfo.occupants(stack);
        int max = HiveInfo.maxOccupants(((BlockItem) stack.getItem()).getBlock());
        int insertAt = Math.min(1, lines.size());
        lines.add(insertAt, HiveInfo.summaryText(HiveInfo.honeyLevel(stack),
                HiveInfo.maxHoney(((BlockItem) stack.getItem()).getBlock()), bees.size(), max));
    }
}
