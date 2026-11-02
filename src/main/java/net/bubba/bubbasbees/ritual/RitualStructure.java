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

import java.util.List;

/**
 * The two ritual structures around the Celestial Altar (finish-spec.md "Rituals"), all on the altar's own layer.
 * Positions are offsets from the altar; x = east, z = south.
 *
 * Night (radius 8, user 2026-11-02): one exact ring, the 8-neighbour outer edge of a radius-8.5 disc (64 cells). Its
 * N/E/S/W cells (8 out) are end rods, its 4 diagonal cells (+-6, +-6) are pedestals holding echo shards, the other 56
 * are stardust. Candles: 4 within 2 of the altar, 2 within 2 of each corner pedestal (inside the ring).
 * Queen (radius 6): the altar on a 3x3 of honeycomb blocks with gold block corners; 8 potted flowers on the radius-6
 * edge, at N/E/S/W (6 out) and on the diagonals (+-4, +-4), the diagonal ones on honey blocks.
 */
public final class RitualStructure {
    private RitualStructure() {}

    public static final int NIGHT_RADIUS = 8, QUEEN_RADIUS = 6;
    /** "Inside the circle": horizontal distance up to the radius + 0.5 (the ring itself), 4 below to 8 above the altar. */
    private static final int AREA_BELOW = 4, AREA_ABOVE = 8;
    private static final int NIGHT_CORNER = 6, QUEEN_CORNER = 4;

    static final int[][] COMPASS = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
    static final String[] COMPASS_NAMES = {"north", "east", "south", "west"};
    static final int[][] DIAGONALS = {{1, -1}, {1, 1}, {-1, 1}, {-1, -1}};
    static final String[] DIAGONAL_NAMES = {"north_east", "south_east", "south_west", "north_west"};

    /** The night ring's stardust cells (rods and corner pedestals left out), in walking order around the circle. */
    public static final List<int[]> NIGHT_DUST = nightDust();

    private static List<int[]> nightDust() {
        double r2 = (NIGHT_RADIUS + 0.5) * (NIGHT_RADIUS + 0.5);
        List<int[]> cells = new java.util.ArrayList<>();
        for (int x = -NIGHT_RADIUS - 1; x <= NIGHT_RADIUS + 1; x++) {
            for (int z = -NIGHT_RADIUS - 1; z <= NIGHT_RADIUS + 1; z++) {
                if (x * x + z * z > r2) continue;
                boolean edge = false;
                for (int dx = -1; dx <= 1 && !edge; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        int nx = x + dx, nz = z + dz;
                        if (nx * nx + nz * nz > r2) { edge = true; break; }
                    }
                }
                boolean rod = (x == 0 || z == 0);
                boolean corner = Math.abs(x) == NIGHT_CORNER && Math.abs(z) == NIGHT_CORNER;
                if (edge && !rod && !corner) cells.add(new int[]{x, z});
            }
        }
        cells.sort(java.util.Comparator.comparingDouble(c -> Math.atan2(c[1], c[0])));
        return List.copyOf(cells);
    }

    public static boolean inArea(BlockPos altar, BlockPos pos, boolean night) {
        int dx = pos.getX() - altar.getX(), dy = pos.getY() - altar.getY(), dz = pos.getZ() - altar.getZ();
        double r = (night ? NIGHT_RADIUS : QUEEN_RADIUS) + 0.5;
        return dy >= -AREA_BELOW && dy <= AREA_ABOVE && dx * dx + dz * dz <= r * r;
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
            if (!level.getBlockState(altar.offset(d[0] * NIGHT_RADIUS, 0, d[1] * NIGHT_RADIUS)).is(Blocks.END_ROD)) {
                return problem("end_rod", dir(COMPASS_NAMES[i]));
            }
        }
        for (int i = 0; i < 4; i++) {
            int[] d = DIAGONALS[i];
            BlockPos pos = altar.offset(d[0] * NIGHT_CORNER, 0, d[1] * NIGHT_CORNER);
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof PedestalBlock pedestal) || pedestal.isAltar()) {
                return problem("corner_pedestal", dir(DIAGONAL_NAMES[i]));
            }
            if (!(level.getBlockEntity(pos) instanceof PedestalBlockEntity be) || !be.getItem().is(Items.ECHO_SHARD)) {
                return problem("echo_shard", dir(DIAGONAL_NAMES[i]));
            }
        }
        BlockPos.MutableBlockPos cell = new BlockPos.MutableBlockPos();
        for (int[] c : NIGHT_DUST) {
            cell.set(altar.getX() + c[0], altar.getY(), altar.getZ() + c[1]);
            if (!(level.getBlockState(cell).getBlock() instanceof StardustBlock)) {
                return problem("ring_dust", cell.getX(), cell.getY(), cell.getZ());
            }
        }
        int centre = countCandles(level, altar, altar, candles);
        if (centre < 4) return problem("centre_candles", centre);
        for (int i = 0; i < 4; i++) {
            int[] d = DIAGONALS[i];
            int beside = countCandles(level, altar.offset(d[0] * NIGHT_CORNER, 0, d[1] * NIGHT_CORNER), altar, candles);
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
            if (!isPottedFlower(level.getBlockState(altar.offset(d[0] * QUEEN_RADIUS, 0, d[1] * QUEEN_RADIUS)))) {
                return problem("flower_pot", dir(COMPASS_NAMES[i]));
            }
        }
        for (int i = 0; i < 4; i++) {
            int[] d = DIAGONALS[i];
            BlockPos pot = altar.offset(d[0] * QUEEN_CORNER, 0, d[1] * QUEEN_CORNER);
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

    /** Candle blocks within 2 (horizontal square, one layer up or down) of centre and inside the night ring. */
    private static int countCandles(Level level, BlockPos centre, BlockPos altar, List<BlockPos> out) {
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-2, -1, -2), centre.offset(2, 1, 2))) {
            if (!(level.getBlockState(pos).getBlock() instanceof AbstractCandleBlock)) continue;
            if (!inArea(altar, pos, true)) continue;
            BlockPos found = pos.immutable();
            if (!out.contains(found)) out.add(found);
            count++;
        }
        return count;
    }
}
