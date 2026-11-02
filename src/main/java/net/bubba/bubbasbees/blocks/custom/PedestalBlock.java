package net.bubba.bubbasbees.blocks.custom;

import net.bubba.bubbasbees.blockentities.PedestalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Display pedestal: holds one item that floats above it, bobbing and turning slowly. Right-click with an item to set
 * it down, right-click again to take it back. The Celestial Altar is the fancy one: the centre of both rituals.
 */
public class PedestalBlock extends BaseEntityBlock {
    // Final shapes (BubbasBeesArt/work/pedestals/final): PB column and AE altar
    private static final VoxelShape PEDESTAL = Shapes.or(Block.box(2, 0, 2, 14, 2, 14), Block.box(3, 2, 3, 13, 3, 13),
            Block.box(5, 3, 5, 11, 11, 11), Block.box(3, 11, 3, 13, 12, 13), Block.box(2, 12, 2, 14, 14, 14));
    private static final VoxelShape ALTAR = Shapes.or(Block.box(0, 0, 0, 16, 4, 16), Block.box(2, 4, 2, 14, 13, 14),
            Block.box(1, 13, 1, 15, 16, 15));

    private final boolean altar;

    public PedestalBlock(Properties properties, boolean altar) {
        super(properties);
        this.altar = altar;
    }

    public boolean isAltar() {
        return altar;
    }

    /** Height of the top surface in blocks; the item floats a little above it. */
    public double topHeight() {
        return altar ? 16.5 / 16.0 : 14.0 / 16.0;   // top of the model (the altar's diamond inlay)
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos,
                                        @NotNull CollisionContext context) {
        return altar ? ALTAR : PEDESTAL;
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new PedestalBlockEntity(pos, state);
    }

    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                                          @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof PedestalBlockEntity pedestal)) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (pedestal.getItem().isEmpty()) {
            if (held.isEmpty()) return InteractionResult.PASS;
            if (!level.isClientSide) {
                ItemStack one = held.copy();
                one.setCount(1);
                pedestal.setItem(one);
                if (!player.getAbilities().instabuild) held.shrink(1);
                level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!level.isClientSide) {
            ItemStack taken = pedestal.getItem();
            pedestal.setItem(ItemStack.EMPTY);
            if (!player.getInventory().add(taken)) player.drop(taken, false);
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof PedestalBlockEntity pedestal) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + topHeight(), pos.getZ() + 0.5, pedestal.getItem());
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    // Comparators read whether something is on it (rituals and redstone builds can check)
    @Override
    public boolean hasAnalogOutputSignal(@NotNull BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos) {
        return level.getBlockEntity(pos) instanceof PedestalBlockEntity pedestal && !pedestal.getItem().isEmpty() ? 15 : 0;
    }
}
