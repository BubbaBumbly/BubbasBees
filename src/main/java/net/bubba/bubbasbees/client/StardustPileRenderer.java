package net.bubba.bubbasbees.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import net.bubba.bubbasbees.BubbasBees;
import net.bubba.bubbasbees.blockentities.StardustBlockEntity;
import net.bubba.bubbasbees.blocks.custom.PedestalBlock;
import net.bubba.bubbasbees.blocks.custom.StardustBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The stardust pile-ups. Against a pedestal or the Celestial Altar, the pile rises in
 * 0.5 px steps up to 3 px at the pedestal's base (the altar's comes 2 px further out, it fills its block); each
 * dust block draws its own. Columns over flat dust start on top of it, so no faces are shared with the block model.
 *
 * The stardust pile-up against an end rod (9 px tall, from generated height
 * maps). Dust can arrive at a rod from 1-4 sides; the per-side height maps merge with max, and the
 * first arriving dust (order W, N, E, S) draws the whole merged pile so no cube is drawn twice. Nothing goes inside
 * the rod (px 7-8); columns over its base (px 6-9) start on the ground (some resource packs drop the base) and are
 * padded out a hair so they don't z-fight the vanilla base.
 */
public class StardustPileRenderer implements BlockEntityRenderer<StardustBlockEntity> {
    /** 4 twinkle versions: version 0 has no suffix, then _1.._3. A dust block picks one by position. */
    public static final int VARIANTS = 4;
    public static final ResourceLocation[] TOP = new ResourceLocation[VARIANTS];
    public static final ResourceLocation[] SIDE = new ResourceLocation[VARIANTS];
    static {
        for (int i = 0; i < VARIANTS; i++) {
            String sfx = i == 0 ? "" : "_" + i;
            TOP[i] = ResourceLocation.fromNamespaceAndPath(BubbasBees.MOD_ID, "block/stardust_pile_top" + sfx);
            SIDE[i] = ResourceLocation.fromNamespaceAndPath(BubbasBees.MOD_ID, "block/stardust_pile_side" + sfx);
        }
    }
    private static final Direction[] ORDER = {Direction.WEST, Direction.NORTH, Direction.EAST, Direction.SOUTH};
    private static final double H = 9.0;
    /** Merged pile per set of arriving sides (bit i = ORDER[i]): boxes {x, y0, z, height, padded} in rod-block pixels. */
    private static final Map<Integer, List<int[]>> PILES = new HashMap<>();
    /** Outward pad (block units) for columns over the rod's base, so their sides sit just outside the base's sides. */
    private static final float BASE_PAD = 0.002F;

    public StardustPileRenderer(BlockEntityRendererProvider.Context context) {
    }

    // FIX: the pile reaches into the rod's or pedestal's block, so it must draw beyond this block's own box.
    @Override
    public boolean shouldRenderOffScreen(@NotNull StardustBlockEntity dust) {
        return true;
    }

    @Override
    public void render(@NotNull StardustBlockEntity dust, float partialTick, @NotNull PoseStack pose,
                       @NotNull MultiBufferSource buffers, int light, int overlay) {
        Level level = dust.getLevel();
        if (level == null) return;
        BlockState state = dust.getBlockState();
        if (!(state.getBlock() instanceof StardustBlock)) return;
        VertexConsumer out = buffers.getBuffer(RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS));
        for (Direction towards : ORDER) {
            if (state.getValue(StardustBlock.PROPERTY.get(towards)) != StardustBlock.Side.SIDE) continue;
            BlockPos ped = dust.getBlockPos().relative(towards);
            if (!(level.getBlockState(ped).getBlock() instanceof PedestalBlock block)) continue;
            int k = indexOf(towards.getOpposite());   // the pedestal side the dust is on: W 0, N 1, E 2, S 3
            List<float[]> boxes = new ArrayList<>();
            for (float[] c : pedestalMap(block.isAltar())) {
                int x = (int) c[0], z = (int) c[2];
                for (int r = 0; r < k; r++) { int t = x; x = 15 - z; z = t; }
                float y0 = c[1], top = c[3];
                if (Math.floorDiv(x, 16) != 0 || Math.floorDiv(z, 16) != 0) {   // inside the dust block: sit on the flat dust
                    y0 = Math.max(y0, flatHeight(state, Math.floorMod(x, 16), Math.floorMod(z, 16)));
                }
                if (top > y0 + 1e-3F) boxes.add(new float[]{x, y0, z, top - y0});
            }
            pose.pushPose();
            pose.translate(ped.getX() - dust.getBlockPos().getX(), 0, ped.getZ() - dust.getBlockPos().getZ());
            drawBoxes(boxes, pose, out, light, variant(dust.getBlockPos()));
            pose.popPose();
        }
        for (Map.Entry<Direction, ?> e : StardustBlock.PROPERTY.entrySet()) {
            Direction towardsRod = e.getKey();
            if (state.getValue(StardustBlock.PROPERTY.get(towardsRod)) != StardustBlock.Side.ROD) continue;
            BlockPos rod = dust.getBlockPos().relative(towardsRod);
            int mask = 0;
            BlockPos owner = null;
            for (int i = 0; i < ORDER.length; i++) {
                BlockPos side = rod.relative(ORDER[i]);
                BlockState s = level.getBlockState(side);
                if (s.getBlock() instanceof StardustBlock
                        && s.getValue(StardustBlock.PROPERTY.get(ORDER[i].getOpposite())) == StardustBlock.Side.ROD) {
                    mask |= 1 << i;
                    if (owner == null) owner = side;
                }
            }
            if (!dust.getBlockPos().equals(owner)) continue;
            pose.pushPose();
            pose.translate(rod.getX() - dust.getBlockPos().getX(), 0, rod.getZ() - dust.getBlockPos().getZ());
            List<float[]> boxes = new ArrayList<>();
            for (int[] c : pile(mask)) {
                float y0 = c[1], top = c[1] + c[3];
                int bx = Math.floorDiv(c[0], 16), bz = Math.floorDiv(c[2], 16);
                if (bx != 0 || bz != 0) {   // over a neighbouring dust block: sit on its flat dust
                    BlockState under = level.getBlockState(rod.offset(bx, 0, bz));
                    if (under.getBlock() instanceof StardustBlock) y0 = Math.max(y0, flatHeight(under, Math.floorMod(c[0], 16), Math.floorMod(c[2], 16)));
                }
                if (top > y0 + 1e-3F) boxes.add(new float[]{c[0], y0, c[2], top - y0, c[4] * BASE_PAD});
            }
            drawBoxes(boxes, pose, out, light, variant(dust.getBlockPos()));
            pose.popPose();
        }
    }

    /** Which twinkle version a pile uses: any fixed spread by position, so neighbours rarely blink alike. */
    private static int variant(BlockPos pos) {
        return Math.floorMod((int) Mth.getSeed(pos.getX(), 0, pos.getZ()) >>> 7, VARIANTS);
    }

    /** Boxes {x, y0, z, h[, pad]} in block px (fractions allowed), pad in blocks; top uv = the world pixel, side uv = its height band. */
    private static void drawBoxes(List<float[]> boxes, PoseStack pose, VertexConsumer out, int light, int variant) {
        var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        TextureAtlasSprite top = atlas.apply(TOP[variant]);
        TextureAtlasSprite side = atlas.apply(SIDE[variant]);
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        for (float[] b : boxes) {
            float pad = b.length > 4 ? b[4] : 0F;
            float x0 = b[0] / 16f - pad, y0 = b[1] / 16f, z0 = b[2] / 16f - pad;
            float x1 = (b[0] + 1) / 16f + pad, y1 = (b[1] + b[3]) / 16f, z1 = (b[2] + 1) / 16f + pad;
            int tu = Math.floorMod((int) b[0], 16), tv = Math.floorMod((int) b[2], 16);
            // 1.21 sprite getU/getV take a 0..1 fraction, not pixels
            float u0 = top.getU(tu / 16f), u1 = top.getU((tu + 1) / 16f), v0 = top.getV(tv / 16f), v1 = top.getV((tv + 1) / 16f);
            quad(out, m, n, light, 0, 1, 0, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, u0, v0, u1, v1);
            int su = Math.floorMod((int) b[0] + (int) b[2], 16);
            float sv0 = side.getV(Math.max(0, 16 - b[1] - b[3]) / 16f), sv1 = side.getV((16 - b[1]) / 16f);
            float su0 = side.getU(su / 16f), su1 = side.getU((su + 1) / 16f);
            quad(out, m, n, light, 0, 0, -1, x1, y1, z0, x1, y0, z0, x0, y0, z0, x0, y1, z0, su0, sv0, su1, sv1);
            quad(out, m, n, light, 0, 0, 1, x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1, su0, sv0, su1, sv1);
            quad(out, m, n, light, -1, 0, 0, x0, y1, z0, x0, y0, z0, x0, y0, z1, x0, y1, z1, su0, sv0, su1, sv1);
            quad(out, m, n, light, 1, 0, 0, x1, y1, z1, x1, y0, z1, x1, y0, z0, x1, y1, z0, su0, sv0, su1, sv1);
        }
    }

    /** Height of the block model's flat dust at a pixel of a stardust block (0 = none): models stardust_dot/arm/hill. */
    static float flatHeight(BlockState state, int x, int z) {
        boolean core = x >= 6 && x <= 9 && z >= 6 && z <= 9;
        if (core && isTurn(state)) return 0.75F;
        boolean dot = (x >= 5 && x <= 10 && z >= 6 && z <= 9) || (x >= 6 && x <= 9 && (z == 5 || z == 10));
        if (dot) return 0.5F;
        boolean across = z >= 6 && z <= 9, along = x >= 6 && x <= 9;
        if (across && x >= 11 && on(state, Direction.EAST)) return 0.5F;
        if (across && x <= 4 && on(state, Direction.WEST)) return 0.5F;
        if (along && z >= 11 && on(state, Direction.SOUTH)) return 0.5F;
        if (along && z <= 4 && on(state, Direction.NORTH)) return 0.5F;
        return 0F;
    }

    private static boolean on(BlockState state, Direction d) {
        return state.getValue(StardustBlock.PROPERTY.get(d)) != StardustBlock.Side.NONE;
    }

    /** Two perpendicular connections (a turn, or a 3-4 way junction): the blockstate's stardust_hill case. */
    private static boolean isTurn(BlockState state) {
        boolean n = on(state, Direction.NORTH), e = on(state, Direction.EAST), s = on(state, Direction.SOUTH), w = on(state, Direction.WEST);
        return (n && e) || (e && s) || (s && w) || (w && n);
    }

    private static int indexOf(Direction d) {
        for (int i = 0; i < ORDER.length; i++) if (ORDER[i] == d) return i;
        return 0;
    }

    private static final Map<Boolean, List<float[]>> PEDESTAL = new HashMap<>();

    /** flat3d_pass1.ped_side_map(3), dust arriving from the west, pedestal-block px: {x, y0, z, top}. The altar fills its
     *  block, so its ramp ends 2 px further out (x <= -1) and has no ledge. Python's round() = Math.rint. */
    private static List<float[]> pedestalMap(boolean altar) {
        return PEDESTAL.computeIfAbsent(altar, a -> {
            List<float[]> out = new ArrayList<>();
            double H = 3;
            for (int x = -6; x < 2; x++) {
                double t = (x + 7) / 8.0;
                double h = H * (Math.exp(2.5 * t) - 1) / (Math.exp(2.5) - 1);
                for (int z = 4; z < 12; z++) {
                    double d = Math.abs(z - 7.5);
                    double hz = d < 2 ? h : (d < 3 ? h * 0.6 : h * 0.3);
                    hz = Math.max(0.5, Math.rint(hz * 2) / 2);
                    out.add(new float[]{a ? x - 2 : x, 0, z, (float) hz});
                }
            }
            if (!a) {
                for (int z = 5; z < 11; z++) out.add(new float[]{2, 2, z, (float) (H - (z >= 6 && z <= 9 ? 0 : 0.5))});
            }
            return out;
        });
    }

    /** Four corners in order; uv: (u0,v0) at the first corner's top edge, (u1,v1) at the opposite corner. */
    private static void quad(VertexConsumer out, Matrix4f m, Matrix3f n, int light, float nx, float ny, float nz,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float u0, float v0, float u1, float v1) {
        out.addVertex(m, ax, ay, az).setColor(255, 255, 255, 255).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(nx, ny, nz);
        out.addVertex(m, bx, by, bz).setColor(255, 255, 255, 255).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(nx, ny, nz);
        out.addVertex(m, cx, cy, cz).setColor(255, 255, 255, 255).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(nx, ny, nz);
        out.addVertex(m, dx, dy, dz).setColor(255, 255, 255, 255).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(nx, ny, nz);
    }

    private static List<int[]> pile(int mask) {
        return PILES.computeIfAbsent(mask, k -> {
            Map<Long, Integer> merged = new HashMap<>();
            for (int i = 0; i < 4; i++) {
                if ((k & (1 << i)) == 0) continue;
                for (Map.Entry<Long, Integer> e : sideMap().entrySet()) {
                    int x = (int) (e.getKey() >> 32), z = (int) (long) e.getKey().intValue();
                    for (int r = 0; r < i; r++) { int t = x; x = 15 - z; z = t; }
                    merged.merge(key(x, z), e.getValue(), Math::max);
                }
            }
            List<int[]> boxes = new ArrayList<>();
            for (Map.Entry<Long, Integer> e : merged.entrySet()) {
                int x = (int) (e.getKey() >> 32), z = e.getKey().intValue();
                boolean onBase = x >= 6 && x <= 9 && z >= 6 && z <= 9;
                int h = e.getValue();
                // over the rod's base: 1 px only would be the base's own top, so skip it; taller columns go down
                // to the ground (resource packs may draw the rod without a base, which left a hollow gap under
                // them), padded out a hair so their sides don't flicker against a vanilla base's sides
                if (onBase && h <= 1) continue;
                if (h > 0) boxes.add(new int[]{x, 0, z, h, onBase ? 1 : 0});
            }
            return boxes;
        });
    }

    private static long key(int x, int z) {
        return ((long) x << 32) | (z & 0xffffffffL);
    }

    /** pile_pass4.side_map(): dust arriving from the west, rod at px 7-8; Python's round() = Math.rint. */
    private static Map<Long, Integer> sideMap() {
        Map<Long, Integer> m = new HashMap<>();
        for (int x = -8; x < 7; x++) {
            double t = (x + 9) / 16.0;
            double h = H * (Math.exp(3 * t) - 1) / (Math.exp(3) - 1);
            for (int z = 4; z < 12; z++) {
                double d = Math.abs(z - 7.5), hz;
                if (d < 1) hz = h;
                else if (d < 2) hz = Math.min(h, 1 + h * 0.45);
                else {
                    double spread = (x + 2) / 8.0;
                    hz = spread <= 0 ? 0 : (d < 3 ? 2 : 1) * Math.min(1, spread * 1.6);
                }
                int r = (int) Math.rint(hz);
                if (r > 0) m.merge(key(x, z), r, Math::max);
            }
        }
        for (int x = 5; x < 12; x++) {
            for (int z = 4; z < 12; z++) {
                if (x >= 7 && x <= 8 && z >= 7 && z <= 8) continue;
                double dr = Math.max(Math.abs(x - 7.5), Math.abs(z - 7.5)) - 1;
                if (dr > 2.5) continue;
                double side = (7.5 - x) / 4;
                int r = (int) Math.rint((3 - dr) * (0.55 + 0.45 * side));
                if (r > 0) m.merge(key(x, z), r, Math::max);
            }
        }
        return m;
    }
}
