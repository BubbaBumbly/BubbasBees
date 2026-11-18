package net.bubba.bubbasbees.blocks.custom;

import com.mojang.serialization.MapCodec;
import net.bubba.bubbasbees.blockentities.CombChuteBlockEntity;
import net.bubba.bubbasbees.registries.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Comb chute: a funnel block on a hive. When the hive is full (honey level 5) it takes
 * everything in one go, as if sheared but without angering the bees, and pushes the items out of its spout into
 * whatever is there, like a hopper. It goes on any face of the hive:
 * FACING is the way the spout points, the hive is on the opposite side. Comb mode takes the combs; honey mode fills a glass bottle from its bottle
 * slot instead (the combs are lost, as when bottling by hand). Empty-hand right-click switches mode; right-click with
 * glass bottles loads them (hoppers and pipes can feed bottles in from the sides too).
 */
public class CombChuteBlock extends BaseEntityBlock {
    public static final BooleanProperty HONEY = BooleanProperty.create("honey");
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    /** Model boxes, spout down: {depth from the hive side, from, to; square cross-section, from, to} (all centred). */
    private static final int[][] BOXES = {{0, 5, 0, 16}, {5, 8, 2, 14}, {8, 10, 3, 13}, {10, 12, 4, 12}, {12, 16, 6, 10}};
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);
    static {
        for (Direction dir : Direction.values()) {
            VoxelShape shape = Shapes.empty();
            for (int[] b : BOXES) {
                // along the spout axis: the hive side is at 0 for a spout pointing the positive way, at 16 otherwise
                double a0 = dir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? b[0] : 16 - b[1];
                double a1 = dir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? b[1] : 16 - b[0];
                double c0 = b[2], c1 = b[3];
                shape = Shapes.or(shape, switch (dir.getAxis()) {
                    case X -> Block.box(a0, c0, c0, a1, c1, c1);
                    case Y -> Block.box(c0, a0, c0, c1, a1, c1);
                    case Z -> Block.box(c0, c0, a0, c1, c1, a1);
                });
            }
            SHAPES.put(dir, shape);
        }
    }

    public CombChuteBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HONEY, false).setValue(FACING, Direction.DOWN));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HONEY, FACING);
    }

    /** The spout points away from the hive: the clicked hive face, else a hive next to it (the one most in view),
     *  else down (a chute placed first, for a hive on top, as before). */
    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction face = context.getClickedFace();
        if (level.getBlockState(pos.relative(face.getOpposite())).getBlock() instanceof BeehiveBlock) {
            return defaultBlockState().setValue(FACING, face);
        }
        for (Direction look : context.getNearestLookingDirections()) {
            if (level.getBlockState(pos.relative(look)).getBlock() instanceof BeehiveBlock) {
                return defaultBlockState().setValue(FACING, look.getOpposite());
            }
        }
        return defaultBlockState();
    }

    @Override
    public @NotNull BlockState rotate(@NotNull BlockState state, @NotNull Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public @NotNull BlockState mirror(@NotNull BlockState state, @NotNull Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return MapCodec.unit(this);
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos,
                                        @NotNull CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack held, @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                                                    @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CombChuteBlockEntity chute)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (held.is(Items.GLASS_BOTTLE)) {
            if (!level.isClientSide) {
                ItemStack left = chute.insertBottles(player.getAbilities().instabuild ? held.copy() : held);
                if (!player.getAbilities().instabuild) player.setItemInHand(hand, left);
                level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.6F, 1.2F);
                player.displayClientMessage(Component.translatable("message.bubbasbees.comb_chute.bottles",
                        chute.bottleCount()), true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
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
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
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
