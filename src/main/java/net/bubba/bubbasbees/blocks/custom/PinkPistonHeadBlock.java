package net.bubba.bubbasbees.blocks.custom;

import net.bubba.bubbasbees.registries.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * The pink sticky piston's head (pink face). Pistons hard-code their head and base blocks; PinkPistonMixins make
 * vanilla's piston code treat the pink piston as a sticky piston and this as its head.
 */
public class PinkPistonHeadBlock extends PistonHeadBlock {
    public PinkPistonHeadBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull ItemStack getCloneItemStack(@NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull BlockState state) {
        return new ItemStack(ModBlocks.PINK_STICKY_PISTON.get());
    }
}
