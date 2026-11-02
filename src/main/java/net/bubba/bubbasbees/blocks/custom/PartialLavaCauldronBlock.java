package net.bubba.bubbasbees.blocks.custom;

import net.bubba.bubbasbees.blockentities.PartialLavaCauldronBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A cauldron 1/4, 2/4 or 3/4 full of lava (the magma bee fills it a quarter at a time); at 4/4 it becomes the vanilla
 * lava cauldron, at 0 the empty cauldron. Other mods see the lava as 250/500/750 mB through the block entity's fluid
 * handler. Hurts like lava when an entity is in the lava.
 */
public class PartialLavaCauldronBlock extends AbstractCauldronBlock implements EntityBlock {
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 1, 3);
    public static final int MB_PER_LEVEL = 250;

    public PartialLavaCauldronBlock(Properties properties) {
        super(properties, CauldronInteraction.newInteractionMap());
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    @Override
    protected double getContentHeight(BlockState state) {
        return (4.0 + 11.0 * state.getValue(LEVEL) / 4.0) / 16.0;   // quarters of the inner depth (px 4-15)
    }

    @Override
    public boolean isFull(@NotNull BlockState state) {
        return false;
    }

    @Override
    public void entityInside(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Entity entity) {
        if (isEntityInsideContent(state, pos, entity)) entity.lavaHurt();
    }

    @Override
    public boolean hasAnalogOutputSignal(@NotNull BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos) {
        return state.getValue(LEVEL);
    }

    @Override
    public @NotNull ItemStack getCloneItemStack(@NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull BlockState state) {
        return new ItemStack(Items.CAULDRON);
    }

    /** The block for a lava level 0-4 (0 = empty cauldron, 4 = vanilla lava cauldron). */
    public static BlockState stateFor(BlockState partial, int level) {
        if (level <= 0) return Blocks.CAULDRON.defaultBlockState();
        if (level >= 4) return Blocks.LAVA_CAULDRON.defaultBlockState();
        return partial.setValue(LEVEL, level);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new PartialLavaCauldronBlockEntity(pos, state);
    }
}
