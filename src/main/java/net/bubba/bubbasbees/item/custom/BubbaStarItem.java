package net.bubba.bubbasbees.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BubbaStarItem extends Item {
    public BubbaStarItem(Properties properties) {
        super(properties.craftRemainder(Items.BEDROCK));
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return new ItemStack(this);  // Should return self
    }

    // The hive side of the mode switch is in AdvancedBeehiveBlock.use
    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("item.bubbasbees.bubba_star.hive_mode").withStyle(ChatFormatting.GRAY));
    }
}