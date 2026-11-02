package net.bubba.bubbasbees.blocks.custom;

import net.bubba.bubbasbees.blockentities.CombChuteBlockEntity;
import net.bubba.bubbasbees.registries.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Comb chute (user design, 2026-11-02): a funnel block placed under a hive. When the hive above is full (honey
 * level 5) it takes everything in one go, as if sheared but without angering the bees, and pushes the items down
 * into whatever is below, like a hopper. Comb mode takes the combs; honey mode fills a glass bottle from its bottle
 * slot instead (the combs are lost, as when bottling by hand). Empty-hand right-click switches mode; right-click with
 * glass bottles loads them (hoppers and pipes can feed bottles in from the sides too).
 */
public class CombChuteBlock extends BaseEntityBlock {
    public static final BooleanProperty HONEY = BooleanProperty.create("honey");
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 10, 0, 16, 16, 16), Block.box(4, 4, 4, 12, 10, 12),
            Block.box(6, 0, 6, 10, 4, 10));

    public CombChuteBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HONEY, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HONEY);
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos,
                                        @NotNull CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                                          @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (!(level.getBlockEntity(pos) instanceof CombChuteBlockEntity chute)) return InteractionResult.PASS;
        if (held.is(Items.GLASS_BOTTLE)) {
            if (!level.isClientSide) {
                ItemStack left = chute.insertBottles(player.getAbilities().instabuild ? held.copy() : held);
                if (!player.getAbilities().instabuild) player.setItemInHand(hand, left);
                level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.6F, 1.2F);
                player.displayClientMessage(Component.translatable("message.bubbasbees.comb_chute.bottles",
                        chute.bottleCount()), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (held.isEmpty() && hand == InteractionHand.MAIN_HAND) {
            if (!level.isClientSide) {
                boolean honey = !state.getValue(HONEY);
                level.setBlock(pos, state.setValue(HONEY, honey), 3);
                level.playSound(null, pos, honey ? SoundEvents.HONEY_BLOCK_PLACE : SoundEvents.BEEHIVE_SHEAR,
                        SoundSource.BLOCKS, 0.8F, 1.0F);
                player.displayClientMessage(Component.translatable(honey
                        ? "message.bubbasbees.comb_chute.honey_mode" : "message.bubbasbees.comb_chute.comb_mode"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                         @NotNull BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CombChuteBlockEntity chute) {
            for (ItemStack stack : chute.contents()) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new CombChuteBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state,
                                                                           @NotNull BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.COMB_CHUTE.get(), CombChuteBlockEntity::serverTick);
    }
}
