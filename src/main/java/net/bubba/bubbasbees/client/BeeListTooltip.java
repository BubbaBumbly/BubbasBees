package net.bubba.bubbasbees.client;

import net.bubba.bubbasbees.blockentities.HiveOccupant;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.List;

/** Tooltip data: the bees stored in a hive item, one icon row per bee. */
public record BeeListTooltip(List<HiveOccupant> bees) implements TooltipComponent {
}
