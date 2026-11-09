package net.bubba.bubbasbees.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.bubba.bubbasbees.BubbasBees;
import net.bubba.bubbasbees.blockentities.HiveOccupant;
import net.bubba.bubbasbees.util.HiveInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.fml.ModList;

import java.util.List;

/**
 * Compact top-of-screen panel: hive name, honey, calm/busy mode, smoke countdown, and one row per stored bee with a release countdown.
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
    private static final int SMOKE_COLOR = 0xC8C8C8;
    private static final int SMOKE_ENDING_COLOR = 0xFF8040;
    private static final boolean JADE_LOADED = ModList.get().isLoaded("jade");

    @Override
    public void render(ForgeGui gui, PoseStack poseStack, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = gui.getMinecraft();
        if (JADE_LOADED || minecraft.options.hideGui || minecraft.level == null || minecraft.screen != null) return;
        HitResult hit = minecraft.hitResult;
        List<Component> lookLines = LookInfo.lines(minecraft, hit);
        if (!lookLines.isEmpty()) {
            drawPanel(poseStack, gui.getFont(), lookLines, screenWidth);
            return;
        }
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) return;
        BlockState state = minecraft.level.getBlockState(blockHit.getBlockPos());
        if (!HiveInfo.isHive(state)) return;

        Font font = gui.getFont();
        long gameTime = minecraft.level.getGameTime();
        BlockEntity blockEntity = minecraft.level.getBlockEntity(blockHit.getBlockPos());
        List<HiveOccupant> bees = HiveInfo.occupants(blockEntity);
        long smoke = HiveInfo.smokeRemaining(blockEntity, gameTime);
        Component smokeLine = smoke > 0L ? HiveInfo.smokeText(smoke) : null;
        Component modeLine = HiveInfo.modeText(blockEntity);
        Component header = HiveInfo.headerText(state.getBlock().getName(), HiveInfo.honeyLevel(state),
                bees.size(), HiveInfo.maxOccupants(state.getBlock()));

        int nameWidth = 0;
        int timerWidth = 0;
        for (HiveOccupant bee : bees) {
            nameWidth = Math.max(nameWidth, font.width(HiveInfo.beeName(bee.id())));
            timerWidth = Math.max(timerWidth, font.width(HiveInfo.timerText(bee, gameTime)));
        }
        int rowWidth = bees.isEmpty() ? 0 : ICON + GAP + nameWidth + TIMER_GAP + timerWidth;
        int smokeWidth = smokeLine == null ? 0 : font.width(smokeLine);
        int modeWidth = modeLine == null ? 0 : font.width(modeLine);
        int width = Math.max(Math.max(font.width(header), Math.max(smokeWidth, modeWidth)), rowWidth) + PAD * 2;
        int smokeHeight = smokeLine == null ? 0 : font.lineHeight + 1;
        int modeHeight = modeLine == null ? 0 : font.lineHeight + 1;
        int height = PAD + font.lineHeight + 1 + modeHeight + smokeHeight + bees.size() * ROW + PAD;
        int left = (screenWidth - width) / 2;
        int right = left + width - PAD;
        int y = TOP;

        GuiComponent.fill(poseStack, left, y, left + width, y + height, BACKGROUND);
        y += PAD;
        GuiComponent.drawCenteredString(poseStack, font, header, screenWidth / 2, y, HEADER_COLOR);
        y += font.lineHeight + 1;
        if (modeLine != null) {
            GuiComponent.drawCenteredString(poseStack, font, modeLine, screenWidth / 2, y, TIMER_COLOR);
            y += font.lineHeight + 1;
        }
        if (smokeLine != null) {
            GuiComponent.drawCenteredString(poseStack, font, smokeLine, screenWidth / 2, y,
                    HiveInfo.smokeEnding(smoke) ? SMOKE_ENDING_COLOR : SMOKE_COLOR);
            y += font.lineHeight + 1;
        }
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
        if (bee.getNamespace().equals(BubbasBees.MOD_ID)) {
            GuiComponent.blit(poseStack, x, y, ICON, ICON, HiveInfo.FACE_U, HiveInfo.CLOAK_FACE_V,
                    HiveInfo.FACE_SIZE, HiveInfo.FACE_SIZE, HiveInfo.TEXTURE_SIZE, HiveInfo.TEXTURE_SIZE);
        }
    }

    /** Draws a list of lines in the same centred panel style as the hive overlay. */
    private static void drawPanel(PoseStack poseStack, Font font, List<Component> lines, int screenWidth) {
        int width = 0;
        for (Component line : lines) width = Math.max(width, font.width(line));
        width += PAD * 2;
        int height = PAD + lines.size() * (font.lineHeight + 1) + PAD;
        int left = (screenWidth - width) / 2;
        int y = TOP;
        GuiComponent.fill(poseStack, left, y, left + width, y + height, BACKGROUND);
        y += PAD;
        for (int i = 0; i < lines.size(); i++) {
            GuiComponent.drawCenteredString(poseStack, font, lines.get(i), screenWidth / 2, y,
                    i == 0 ? HEADER_COLOR : TEXT);
            y += font.lineHeight + 1;
        }
    }
}
