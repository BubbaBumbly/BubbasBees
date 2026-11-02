package net.bubba.bubbasbees.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import net.bubba.bubbasbees.BubbasBees;
import net.bubba.bubbasbees.blockentities.StardustBlockEntity;
import net.bubba.bubbasbees.blocks.custom.StardustBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
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
 * The stardust pile-up against an end rod (art pick T1 at 9 px, BubbasBeesArt/work/stardust/final/README.md, height
 * maps from pile_pass4.py). Dust can arrive at a rod from 1-4 sides; the per-side height maps merge with max, and the
 * first arriving dust (order W, N, E, S) draws the whole merged pile so no cube is drawn twice. Nothing goes inside
 * the rod (px 7-8) and columns over the rod's base (px 6-9) start on top of it (y 1).
 */
public class StardustPileRenderer implements BlockEntityRenderer<StardustBlockEntity> {
    public static final ResourceLocation TOP = new ResourceLocation(BubbasBees.MOD_ID, "block/stardust_pile_top");
    public static final ResourceLocation SIDE = new ResourceLocation(BubbasBees.MOD_ID, "block/stardust_pile_side");
    private static final Direction[] ORDER = {Direction.WEST, Direction.NORTH, Direction.EAST, Direction.SOUTH};
    private static final double H = 9.0;
    /** Merged pile per set of arriving sides (bit i = ORDER[i]): boxes {x, y0, z, height} in rod-block pixels. */
    private static final Map<Integer, List<int[]>> PILES = new HashMap<>();

    public StardustPileRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(@NotNull StardustBlockEntity dust, float partialTick, @NotNull PoseStack pose,
                       @NotNull MultiBufferSource buffers, int light, int overlay) {
        Level level = dust.getLevel();
        if (level == null) return;
        BlockState state = dust.getBlockState();
        if (!(state.getBlock() instanceof StardustBlock)) return;
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
            drawPile(pile(mask), pose, buffers.getBuffer(RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS)), light);
            pose.popPose();
        }
    }

    private static void drawPile(List<int[]> boxes, PoseStack pose, VertexConsumer out, int light) {
        var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        TextureAtlasSprite top = atlas.apply(TOP);
        TextureAtlasSprite side = atlas.apply(SIDE);
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        for (int[] b : boxes) {
            float x0 = b[0] / 16f, y0 = b[1] / 16f, z0 = b[2] / 16f;
            float x1 = x0 + 1 / 16f, y1 = (b[1] + b[3]) / 16f, z1 = z0 + 1 / 16f;
            int tu = Math.floorMod(b[0], 16), tv = Math.floorMod(b[2], 16);
            float u0 = top.getU(tu), u1 = top.getU(tu + 1), v0 = top.getV(tv), v1 = top.getV(tv + 1);
            quad(out, m, n, light, 0, 1, 0, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, u0, v0, u1, v1);
            int su = Math.floorMod(b[0] + b[2], 16);
            float sv0 = side.getV(Math.max(0, 16 - b[3])), sv1 = side.getV(16);
            float su0 = side.getU(su), su1 = side.getU(su + 1);
            quad(out, m, n, light, 0, 0, -1, x1, y1, z0, x1, y0, z0, x0, y0, z0, x0, y1, z0, su0, sv0, su1, sv1);
            quad(out, m, n, light, 0, 0, 1, x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1, su0, sv0, su1, sv1);
            quad(out, m, n, light, -1, 0, 0, x0, y1, z0, x0, y0, z0, x0, y0, z1, x0, y1, z1, su0, sv0, su1, sv1);
            quad(out, m, n, light, 1, 0, 0, x1, y1, z1, x1, y0, z1, x1, y0, z0, x1, y1, z0, su0, sv0, su1, sv1);
        }
    }

    /** Four corners in order; uv: (u0,v0) at the first corner's top edge, (u1,v1) at the opposite corner. */
    private static void quad(VertexConsumer out, Matrix4f m, Matrix3f n, int light, float nx, float ny, float nz,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float u0, float v0, float u1, float v1) {
        out.vertex(m, ax, ay, az).color(255, 255, 255, 255).uv(u0, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, nx, ny, nz).endVertex();
        out.vertex(m, bx, by, bz).color(255, 255, 255, 255).uv(u0, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, nx, ny, nz).endVertex();
        out.vertex(m, cx, cy, cz).color(255, 255, 255, 255).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, nx, ny, nz).endVertex();
        out.vertex(m, dx, dy, dz).color(255, 255, 255, 255).uv(u1, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, nx, ny, nz).endVertex();
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
                int y0 = onBase ? 1 : 0;
                int h = e.getValue() - y0;
                if (h > 0) boxes.add(new int[]{x, y0, z, h});
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
