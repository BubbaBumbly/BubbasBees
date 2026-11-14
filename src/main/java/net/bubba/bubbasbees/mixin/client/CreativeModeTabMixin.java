package net.bubba.bubbasbees.mixin.client;

import net.bubba.bubbasbees.item.ModCreativeModeTab;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * JEI builds its item list from the search tab before it reaches the mod's own tab, and the search tab lists items
 * by registry id. Ids differ per world (a world keeps the ids it first gave each item), so JEI's order drifted from
 * the creative tab's. This puts the mod's items in the search tab's list into the tab's order.
 */
@Mixin(CreativeModeTab.class)
public abstract class CreativeModeTabMixin {
    @Inject(method = "fillItemList", at = @At("TAIL"))
    private void bubbasBees$tabOrderInSearch(NonNullList<ItemStack> items, CallbackInfo ci) {
        if ((Object) this == CreativeModeTab.TAB_SEARCH) ModCreativeModeTab.sortLikeTab(items);
    }
}
