package net.bubba.bubbasbees.client.guide;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.bubba.bubbasbees.blocks.custom.StardustBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A structure from a book page: "pattern" = layers top to bottom, each a list of rows (north to south) of columns
 * (west to east); "mapping" = char to a block id or #tag. '_' is "anything", '0' marks the centre (the altar, the
 * Visualize anchor). Optional "loose": char to {"radius": r, "around": "0P", "within": 8.5}: such a cell also counts
 * as done when a matching block sits within r blocks sideways (and 1 up or down) of the nearest cell whose char is in
 * "around", and within "within" blocks of the centre, the same test the night ritual uses for its candles. An empty
 * flower pot never counts (the queen ritual needs flowers in them). Used by the 3D preview and by Visualize.
 */
public final class GuideMultiblock {
    /** One required block, relative to the centre. */
    public record Cell(BlockPos offset, char key) {
    }

    private final Map<Character, Block> blocks = new HashMap<>();
    private final Map<Character, TagKey<Block>> tags = new HashMap<>();
    private record Loose(int radius, String around, double within) {
    }

    private final Map<Character, Loose> loose = new HashMap<>();
    public final List<Cell> cells = new ArrayList<>();
    public final int sizeX, sizeY, sizeZ;
    /** Display state per offset, with stardust connected to its neighbours. */
    private final Map<BlockPos, BlockState> display = new HashMap<>();

    public GuideMultiblock(JsonObject json) {
        JsonObject mapping = json.getAsJsonObject("mapping");
        for (Map.Entry<String, JsonElement> e : mapping.entrySet()) {
            char c = e.getKey().charAt(0);
            String id = e.getValue().getAsString();
            if (id.startsWith("#")) {
                tags.put(c, TagKey.create(Registry.BLOCK_REGISTRY, new ResourceLocation(id.substring(1))));
            } else {
                Block b = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id));
                blocks.put(c, b == null ? Blocks.BARRIER : b);
            }
        }
        if (json.has("loose")) {
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("loose").entrySet()) {
                JsonObject l = e.getValue().getAsJsonObject();
                loose.put(e.getKey().charAt(0), new Loose(l.get("radius").getAsInt(), l.get("around").getAsString(),
                        l.has("within") ? l.get("within").getAsDouble() : 1e9));
            }
        }
        JsonArray layers = json.getAsJsonArray("pattern");
        int ly = layers.size(), lz = 0, lx = 0, cx = 0, cy = 0, cz = 0;
        char[][][] grid = new char[ly][][];
        for (int y = 0; y < ly; y++) {
            JsonArray rows = layers.get(y).getAsJsonArray();
            grid[y] = new char[rows.size()][];
            lz = Math.max(lz, rows.size());
            for (int z = 0; z < rows.size(); z++) {
                grid[y][z] = rows.get(z).getAsString().toCharArray();
                lx = Math.max(lx, grid[y][z].length);
                for (int x = 0; x < grid[y][z].length; x++) {
                    if (grid[y][z][x] == '0') { cx = x; cy = y; cz = z; }
                }
            }
        }
        sizeX = lx; sizeY = ly; sizeZ = lz;
        for (int y = 0; y < ly; y++) {
            for (int z = 0; z < grid[y].length; z++) {
                for (int x = 0; x < grid[y][z].length; x++) {
                    char c = grid[y][z][x];
                    if (c == '_' || c == ' ') continue;
                    cells.add(new Cell(new BlockPos(x - cx, cy - y, z - cz), c));   // layer 0 is the top
                }
            }
        }
        Map<BlockPos, BlockState> plain = new HashMap<>();
        for (Cell cell : cells) plain.put(cell.offset(), baseState(cell.key()));
        for (Map.Entry<BlockPos, BlockState> e : plain.entrySet()) {
            BlockState s = e.getValue();
            if (s.getBlock() instanceof StardustBlock) {
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    BlockState n = plain.getOrDefault(e.getKey().relative(d), Blocks.AIR.defaultBlockState());
                    s = s.setValue(StardustBlock.PROPERTY.get(d), StardustBlock.sideFor(n, d));
                }
            }
            display.put(e.getKey(), s);
        }
    }

    private BlockState baseState(char key) {
        Block b = blocks.get(key);
        if (b != null) return b.defaultBlockState();
        TagKey<Block> tag = tags.get(key);
        if (tag != null) {   // the first block of the tag that would count (a potted flower, not an empty pot)
            Block fallback = null;
            for (Block value : ForgeRegistries.BLOCKS.tags().getTag(tag)) {
                if (fallback == null) fallback = value;
                if (counts(value.defaultBlockState())) return value.defaultBlockState();
            }
            if (fallback != null) return fallback.defaultBlockState();
        }
        return Blocks.BARRIER.defaultBlockState();
    }

    public BlockState displayState(Cell cell) {
        return display.get(cell.offset());
    }

    public boolean matches(Cell cell, BlockState state) {
        if (!counts(state)) return false;
        Block b = blocks.get(cell.key());
        if (b != null) return state.is(b);
        TagKey<Block> tag = tags.get(cell.key());
        return tag != null && state.is(tag);
    }

    private static boolean counts(BlockState state) {
        return !(state.getBlock() instanceof FlowerPotBlock pot) || pot.getContent() != Blocks.AIR;
    }

    /**
     * The cells already built around centre: exact matches first, then each still-open loose cell takes the nearest
     * matching block in its radius that no other cell has taken.
     */
    public Set<Cell> done(Level level, BlockPos centre) {
        Set<Cell> done = new HashSet<>();
        Set<BlockPos> taken = new HashSet<>();
        for (Cell c : cells) {
            BlockPos pos = centre.offset(c.offset());
            if (matches(c, level.getBlockState(pos))) { done.add(c); taken.add(pos); }
        }
        for (Cell c : cells) {
            Loose l = loose.get(c.key());
            if (l == null || done.contains(c)) continue;
            Cell group = null;     // the altar or pedestal this cell belongs to
            for (Cell o : cells) {
                if (l.around().indexOf(o.key()) < 0) continue;
                if (group == null || o.offset().distSqr(c.offset()) < group.offset().distSqr(c.offset())) group = o;
            }
            if (group == null) continue;
            BlockPos at = centre.offset(c.offset()), g = centre.offset(group.offset()), best = null;
            int r = l.radius();
            for (BlockPos p : BlockPos.betweenClosed(g.offset(-r, -1, -r), g.offset(r, 1, r))) {
                int dx = p.getX() - centre.getX(), dz = p.getZ() - centre.getZ();
                if (dx * dx + dz * dz > l.within() * l.within()) continue;
                if (taken.contains(p) || !matches(c, level.getBlockState(p))) continue;
                if (best == null || p.distSqr(at) < best.distSqr(at)) best = p.immutable();
            }
            if (best != null) { done.add(c); taken.add(best); }
        }
        return done;
    }
}
