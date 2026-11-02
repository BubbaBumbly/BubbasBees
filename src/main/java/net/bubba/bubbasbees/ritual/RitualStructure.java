package net.bubba.bubbasbees.ritual;

import net.bubba.bubbasbees.blockentities.PedestalBlockEntity;
import net.bubba.bubbasbees.blocks.custom.PedestalBlock;
import net.bubba.bubbasbees.blocks.custom.StardustBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.List;

/**
 * The two ritual structures around the Celestial Altar (finish-spec.md "Rituals"), all on the altar's own layer.
 * Positions are offsets from the altar; x = east, z = south.
 *
 * Night: end rods 7 blocks N/E/S/W; a closed stardust circle (radius 6, touching each rod) through 4 pedestals with
 * echo shards on the diagonals at (+-4, +-4); 4 candles within 2 blocks of the altar and 2 beside each corner pedestal.
 * Queen: the altar on a 3x3 of honeycomb blocks with gold block corners; 8 potted flowers on the radius-6 edge, at
 * N/E/S/W (6 out) and on the diagonals (+-4, +-4), the diagonal ones on honey blocks.
 */
public final class RitualStructure {
    private RitualStructure() {}

    public static final int RADIUS = 6;
    /** "Inside the circle": horizontal distance up to 6.5 (the dust ring's middle), 4 below to 8 above the altar. */
    private static final double AREA_RADIUS_SQ = 6.5 * 6.5;
    private static final int AREA_BELOW = 4, AREA_ABOVE = 8;
    private static final int ROD_DISTANCE = 7, CORNER = 4;
    /** The flood fill that proves the circle is closed gives up (leak) once it reaches this distance squared. */
    private static final int LEAK_DISTANCE_SQ = 7 * 7;

    static final int[][] COMPASS = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
    static final String[] COMPASS_NAMES = {"north", "east", "south", "west"};
    static final int[][] DIAGONALS = {{1, -1}, {1, 1}, {-1, 1}, {-1, -1}};
    static final String[] DIAGONAL_NAMES = {"north_east", "south_east", "south_west", "north_west"};

    public static boolean inArea(BlockPos altar, BlockPos pos) {
        int dx = pos.getX() - altar.getX(), dy = pos.getY() - altar.getY(), dz = pos.getZ() - altar.getZ();
        return dy >= -AREA_BELOW && dy <= AREA_ABOVE && dx * dx + dz * dz <= AREA_RADIUS_SQ;
    }

    /**
     * Where a finished flower visit counts: inside the circle, or (night) on one of the circle's own end rods, which
     * sit 7 out and are a starlight flower (starlight bees fly to them first).
     */
    public static boolean countsFlower(BlockPos altar, BlockPos flower, boolean night) {
        if (inArea(altar, flower)) return true;
        if (!night || flower.getY() != altar.getY()) return false;
        int dx = Math.abs(flower.getX() - altar.getX()), dz = Math.abs(flower.getZ() - altar.getZ());
        return (dx == 0 && dz == ROD_DISTANCE) || (dz == 0 && dx == ROD_DISTANCE);
    }

    private static Component dir(String name) {
        return Component.translatable("ritual.bubbasbees.dir." + name);
    }

    private static Component problem(String key, Object... args) {
        return Component.translatable("ritual.bubbasbees.problem." + key, args);
    }

    /** What's wrong with the night structure, or null when it is complete; fills candles with every candle it counted. */
    public static @Nullable Component checkNight(Level level, BlockPos altar, List<BlockPos> candles) {
        candles.clear();
        for (int i = 0; i < 4; i++) {
            int[] d = COMPASS[i];
            if (!level.getBlockState(altar.offset(d[0] * ROD_DISTANCE, 0, d[1] * ROD_DISTANCE)).is(Blocks.END_ROD)) {
                return problem("end_rod", dir(COMPASS_NAMES[i]));
            }
            if (!(level.getBlockState(altar.offset(d[0] * RADIUS, 0, d[1] * RADIUS)).getBlock() instanceof StardustBlock)) {
                return problem("rod_dust", dir(COMPASS_NAMES[i]));
            }
        }
        for (int i = 0; i < 4; i++) {
            int[] d = DIAGONALS[i];
            BlockPos pos = altar.offset(d[0] * CORNER, 0, d[1] * CORNER);
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof PedestalBlock pedestal) || pedestal.isAltar()) {
                return problem("corner_pedestal", dir(DIAGONAL_NAMES[i]));
            }
            if (!(level.getBlockEntity(pos) instanceof PedestalBlockEntity be) || !be.getItem().is(Items.ECHO_SHARD)) {
                return problem("echo_shard", dir(DIAGONAL_NAMES[i]));
            }
        }
        if (!circleClosed(level, altar)) return problem("circle");
        int centre = countCandles(level, altar, 2, null, candles);
        if (centre < 4) return problem("centre_candles", centre);
        for (int i = 0; i < 4; i++) {
            int[] d = DIAGONALS[i];
            int beside = countCandles(level, altar.offset(d[0] * CORNER, 0, d[1] * CORNER), 1, altar, candles);
            if (beside < 2) return problem("corner_candles", dir(DIAGONAL_NAMES[i]), beside);
        }
        return null;
    }

    /** What's wrong with the queen structure, or null when it is complete. */
    public static @Nullable Component checkQueen(Level level, BlockPos altar) {
        BlockPos base = altar.below();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Block want = x != 0 && z != 0 ? Blocks.GOLD_BLOCK : Blocks.HONEYCOMB_BLOCK;
                if (!level.getBlockState(base.offset(x, 0, z)).is(want)) return problem("queen_base");
            }
        }
        for (int i = 0; i < 4; i++) {
            int[] d = COMPASS[i];
            if (!isPottedFlower(level.getBlockState(altar.offset(d[0] * RADIUS, 0, d[1] * RADIUS)))) {
                return problem("flower_pot", dir(COMPASS_NAMES[i]));
            }
        }
        for (int i = 0; i < 4; i++) {
            int[] d = DIAGONALS[i];
            BlockPos pot = altar.offset(d[0] * CORNER, 0, d[1] * CORNER);
            if (!isPottedFlower(level.getBlockState(pot))) return problem("flower_pot", dir(DIAGONAL_NAMES[i]));
            if (!level.getBlockState(pot.below()).is(Blocks.HONEY_BLOCK)) return problem("honey_block", dir(DIAGONAL_NAMES[i]));
        }
        return null;
    }

    private static boolean isPottedFlower(BlockState state) {
        return state.getBlock() instanceof FlowerPotBlock pot && pot.getContent() != Blocks.AIR
                && pot.getContent().defaultBlockState().is(BlockTags.FLOWERS);
    }

    static boolean isLitCandle(BlockState state) {
        return state.getBlock() instanceof AbstractCandleBlock && state.getValue(AbstractCandleBlock.LIT);
    }

    /** Candle blocks within reach (horizontal square, one layer up or down) of centre; inside the circle when altar is given. */
    private static int countCandles(Level level, BlockPos centre, int reach, @Nullable BlockPos altar, List<BlockPos> out) {
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-reach, -1, -reach), centre.offset(reach, 1, reach))) {
            if (!(level.getBlockState(pos).getBlock() instanceof AbstractCandleBlock)) continue;
            if (altar != null && !inArea(altar, pos)) continue;
            BlockPos found = pos.immutable();
            if (!out.contains(found)) out.add(found);
            count++;
        }
        return count;
    }

    private static boolean isWall(Level level, BlockPos pos) {
        Block block = level.getBlockState(pos).getBlock();
        return block instanceof StardustBlock || block instanceof PedestalBlock;
    }

    /**
     * The circle is closed when a flood fill from the altar, moving in all 8 directions on the altar's layer and
     * stopped by stardust and pedestals, never gets 7 blocks out. Diagonal-only dust links leak, so the dust must
     * join edge to edge, like it looks.
     */
    private static boolean circleClosed(Level level, BlockPos altar) {
        int size = 2 * ROD_DISTANCE + 1;
        boolean[] seen = new boolean[size * size];
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{0, 0});
        seen[(ROD_DISTANCE) * size + ROD_DISTANCE] = true;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        while (!queue.isEmpty()) {
            int[] cell = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int x = cell[0] + dx, z = cell[1] + dz;
                    if (x * x + z * z >= LEAK_DISTANCE_SQ) return false;
                    int index = (x + ROD_DISTANCE) * size + z + ROD_DISTANCE;
                    if (seen[index]) continue;
                    seen[index] = true;
                    if (isWall(level, pos.set(altar.getX() + x, altar.getY(), altar.getZ() + z))) continue;
                    queue.add(new int[]{x, z});
                }
            }
        }
        return true;
    }
}
