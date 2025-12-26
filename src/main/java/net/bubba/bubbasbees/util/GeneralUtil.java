package net.bubba.bubbasbees.util;


import net.bubba.bubbasbees.registries.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class GeneralUtil {

    public Item getConversionItemFromVariantStr (String variantStr) {
        return switch (variantStr) {
            case "copper" -> ModItems.COPPER_CONVERSION_GEM.get();
            case "sand" -> ModItems.SANDY_CONVERSION_GEM.get();
            case "none" -> Items.AIR;
            default -> throw new IllegalStateException("Unexpected value: " + variantStr);
        };
    }
}
