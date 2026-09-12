package net.bubba.bubbasbees.client;

import com.mojang.datafixers.util.Either;
import net.bubba.bubbasbees.BubbasBees;
import net.bubba.bubbasbees.blockentities.HiveOccupant;
import net.bubba.bubbasbees.util.HiveInfo;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/** Adds honey level and stored bees to the tooltip of any hive item hovered in an inventory. */
@Mod.EventBusSubscriber(modid = BubbasBees.MOD_ID, value = Dist.CLIENT)
public class HiveTooltipHandler {

    @SubscribeEvent
    public static void onGatherComponents(RenderTooltipEvent.GatherComponents event) {
        ItemStack stack = event.getItemStack();
        if (!HiveInfo.isHiveItem(stack)) return;

        List<HiveOccupant> bees = HiveInfo.occupants(stack);
        int max = HiveInfo.maxOccupants(((BlockItem) stack.getItem()).getBlock());
        List<Either<FormattedText, TooltipComponent>> elements = event.getTooltipElements();
        int insertAt = Math.min(1, elements.size());

        elements.add(insertAt++, Either.left(HiveInfo.honeyText(HiveInfo.honeyLevel(stack))));
        if (bees.isEmpty()) {
            elements.add(insertAt, Either.left(HiveInfo.emptyText()));
        } else {
            elements.add(insertAt++, Either.left(HiveInfo.beesText(bees.size(), max)));
            elements.add(insertAt, Either.right(new BeeListTooltip(bees)));
        }
    }
}
