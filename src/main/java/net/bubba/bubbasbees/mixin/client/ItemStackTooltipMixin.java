package net.bubba.bubbasbees.mixin.client;

import net.bubba.bubbasbees.blockentities.HiveOccupant;
import net.bubba.bubbasbees.client.BeeListTooltip;
import net.bubba.bubbasbees.client.HiveTooltipHandler;
import net.bubba.bubbasbees.util.HiveInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

/** Adds the hive summary text line and the bee-list tooltip component to hive items. */
@Mixin(ItemStack.class)
public abstract class ItemStackTooltipMixin {

    @Inject(method = "getTooltipLines", at = @At("RETURN"))
    private void bubbasBees$hiveLine(Item.TooltipContext context, Player player, TooltipFlag flag,
                                     CallbackInfoReturnable<List<Component>> cir) {
        HiveTooltipHandler.addHiveLine((ItemStack) (Object) this, cir.getReturnValue());
    }

    @Inject(method = "getTooltipImage", at = @At("RETURN"), cancellable = true)
    private void bubbasBees$hiveBees(CallbackInfoReturnable<Optional<TooltipComponent>> cir) {
        ItemStack self = (ItemStack) (Object) this;
        if (cir.getReturnValue().isPresent() || !HiveInfo.isHiveItem(self)) return;
        List<HiveOccupant> bees = HiveInfo.occupants(self);
        if (!bees.isEmpty()) cir.setReturnValue(Optional.of(new BeeListTooltip(bees)));
    }
}
