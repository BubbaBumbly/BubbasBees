package net.bubba.bubbasbees.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.bubba.bubbasbees.blockentities.HiveOccupant;
import net.bubba.bubbasbees.util.HiveInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.fml.ModList;

import java.util.List;

/**
 * Compact top-of-screen panel: hive name, honey, and one row per stored bee with a release countdown.
 * Hidden while Jade is installed: Jade draws its own panel in the same spot and gets the bee rows from jade.HiveBeesProvider.
 */
public class HiveInfoOverlay implements IGuiOverlay {
    public static final String ID = "hive_info";
    public static final int ICON = 9;
    static final int ROW = 10;
    public static final int GAP = 3;
    private static final int PAD = 3;
    private static final int TOP = 4;
    public static final int TIMER_GAP = 8;
    private static final int BACKGROUND = 0x90000000;
    private static final int TEXT = 0xFFFFFF;
    private static final int HEADER_COLOR = 0xFFC83C;
    private static final int TIMER_COLOR = 0xAAAAAA;
    private static final boolean JADE_LOADED = ModList.get().isLoaded("jade");

    @Override
    public void render(ForgeGui gui, PoseStack poseStack, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = gui.getMinecraft();
        if (JADE_LOADED || minecraft.options.hideGui || minecraft.level == null || minecraft.screen != null) return;
        HitResult hit = minecraft.hitResult;
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) return;
        BlockState state = minecraft.level.getBlockState(blockHit.getBlockPos());
        if (!HiveInfo.isHive(state)) return;

        Font font = gui.getFont();
        long gameTime = minecraft.level.getGameTime();
        List<HiveOccupant> bees = HiveInfo.occupants(minecraft.level.getBlockEntity(blockHit.getBlockPos()));
        Component header = HiveInfo.headerText(state.getBlock().getName(), HiveInfo.honeyLevel(state),
                bees.size(), HiveInfo.maxOccupants(state.getBlock()));

        int nameWidth = 0;
        int timerWidth = 0;
        for (HiveOccupant bee : bees) {
            nameWidth = Math.max(nameWidth, font.width(HiveInfo.beeName(bee.id())));
            timerWidth = Math.max(timerWidth, font.width(HiveInfo.timerText(bee, gameTime)));
        }
        int rowWidth = bees.isEmpty() ? 0 : ICON + GAP + nameWidth + TIMER_GAP + timerWidth;
        int width = Math.max(font.width(header), rowWidth) + PAD * 2;
        int height = PAD + font.lineHeight + 1 + bees.size() * ROW + PAD;
        int left = (screenWidth - width) / 2;
        int right = left + width - PAD;
        int y = TOP;

        GuiComponent.fill(poseStack, left, y, left + width, y + height, BACKGROUND);
        y += PAD;
        GuiComponent.drawCenteredString(poseStack, font, header, screenWidth / 2, y, HEADER_COLOR);
        y += font.lineHeight + 1;
        for (HiveOccupant bee : bees) {
            drawBeeRow(poseStack, font, bee.id(), left + PAD, y);
            Component timer = HiveInfo.timerText(bee, gameTime);
            font.draw(poseStack, timer, right - font.width(timer), y + 1, TIMER_COLOR);
            y += ROW;
        }
    }

    static void drawBeeRow(PoseStack poseStack, Font font, ResourceLocation bee, int x, int y) {
        drawBeeIcon(poseStack, bee, x, y);
        font.draw(poseStack, HiveInfo.beeName(bee), x + ICON + GAP, y + 1, TEXT);
    }

    public static void drawBeeIcon(PoseStack poseStack, ResourceLocation bee, int x, int y) {
        ResourceLocation texture = HiveInfo.iconTexture(bee);
        if (texture == null) return;
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        GuiComponent.blit(poseStack, x, y, ICON, ICON, HiveInfo.FACE_U, HiveInfo.FACE_V,
                HiveInfo.FACE_SIZE, HiveInfo.FACE_SIZE, HiveInfo.TEXTURE_SIZE, HiveInfo.TEXTURE_SIZE);
    }
}
