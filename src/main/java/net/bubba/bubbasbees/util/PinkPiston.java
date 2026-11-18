package net.bubba.bubbasbees.util;

import net.bubba.bubbasbees.registries.ModBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Vanilla piston code checks for Blocks.STICKY_PISTON / PISTON_HEAD by identity; the pink pair counts as those. */
public final class PinkPiston {
    private PinkPiston() {}

    public static boolean is(BlockState state, Block block) {
        if (state.is(block)) return true;
        if (block == Blocks.STICKY_PISTON) return state.is(ModBlocks.PINK_STICKY_PISTON.get());
        if (block == Blocks.PISTON_HEAD) return state.is(ModBlocks.PINK_PISTON_HEAD.get());
        return false;
    }

    /** The head block a piston places: the pink head for the pink piston. */
    public static Block headFor(Object piston) {
        return piston == ModBlocks.PINK_STICKY_PISTON.get() ? ModBlocks.PINK_PISTON_HEAD.get() : Blocks.PISTON_HEAD;
    }

    public static Block headFor(BlockState pistonState) {
        return pistonState != null && pistonState.is(ModBlocks.PINK_STICKY_PISTON.get())
                ? ModBlocks.PINK_PISTON_HEAD.get() : Blocks.PISTON_HEAD;
    }
}
