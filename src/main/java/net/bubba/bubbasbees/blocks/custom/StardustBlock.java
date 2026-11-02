package net.bubba.bubbasbees.blocks.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndRodBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.bubba.bubbasbees.blockentities.StardustBlockEntity;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Stardust laid on the ground like redstone dust: glows (light 7), carries no signal. Connects to other stardust,
 * pedestals and the Celestial Altar, and to end rods, where it piles up the rod's side on an exponential curve within
 * the last block (the "rod" arm's model reaches into the rod's block). The night ritual's circle is made of it.
 */
public class StardustBlock extends Block implements EntityBlock {
    public enum Side implements StringRepresentable {
        NONE("none"), SIDE("side"), ROD("rod");

        private final String name;

        Side(String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }

    public static final EnumProperty<Side> NORTH = EnumProperty.create("north", Side.class);
    public static final EnumProperty<Side> EAST = EnumProperty.create("east", Side.class);
    public static final EnumProperty<Side> SOUTH = EnumProperty.create("south", Side.class);
    public static final EnumProperty<Side> WEST = EnumProperty.create("west", Side.class);
    public static final Map<Direction, EnumProperty<Side>> PROPERTY = Map.of(
            Direction.NORTH, NORTH, Direction.EAST, EAST, Direction.SOUTH, SOUTH, Direction.WEST, WEST);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1, 16);

    public StardustBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(NORTH, Side.NONE).setValue(EAST, Side.NONE)
                .setValue(SOUTH, Side.NONE).setValue(WEST, Side.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST);
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos,
                                        @NotNull CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return connect(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    public @NotNull BlockState updateShape(@NotNull BlockState state, @NotNull Direction direction, @NotNull BlockState neighbour,
                                           @NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull BlockPos neighbourPos) {
        if (direction == Direction.DOWN && !canSurvive(state, level, pos)) return Blocks.AIR.defaultBlockState();
        if (direction.getAxis().isHorizontal()) {
            return state.setValue(PROPERTY.get(direction), sideFor(neighbour, direction));
        }
        return state;
    }

    private BlockState connect(BlockState state, BlockGetter level, BlockPos pos) {
        for (Map.Entry<Direction, EnumProperty<Side>> e : PROPERTY.entrySet()) {
            state = state.setValue(e.getValue(), sideFor(level.getBlockState(pos.relative(e.getKey())), e.getKey()));
        }
        return state;
    }

    private static Side sideFor(BlockState neighbour, Direction towards) {
        if (neighbour.getBlock() instanceof StardustBlock || neighbour.getBlock() instanceof PedestalBlock) return Side.SIDE;
        // a standing end rod (the ritual's compass-point rods); the dust climbs its side
        if (neighbour.is(Blocks.END_ROD) && neighbour.getValue(EndRodBlock.FACING).getAxis() == Direction.Axis.Y) return Side.ROD;
        return Side.NONE;
    }

    // The block entity only draws the end-rod pile (StardustPileRenderer)
    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new StardustBlockEntity(pos, state);
    }

    @Override
    public boolean canSurvive(@NotNull BlockState state, @NotNull LevelReader level, @NotNull BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }
}
