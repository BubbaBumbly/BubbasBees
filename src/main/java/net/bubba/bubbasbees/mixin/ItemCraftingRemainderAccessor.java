package net.bubba.bubbasbees.mixin;

// DEVIATION: vanilla Item.getCraftingRemainingItem() is final and fixed, so this accessor points a star's remainder at itself.
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Vanilla's crafting remainder is a final field; this lets a star item return itself after crafting. */
@Mixin(Item.class)
public interface ItemCraftingRemainderAccessor {

    @Mutable
    @Accessor("craftingRemainingItem")
    void bubbasBees$setCraftingRemainder(Item item);
}
