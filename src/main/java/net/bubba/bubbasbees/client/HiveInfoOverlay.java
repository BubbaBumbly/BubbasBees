package net.bubba.bubbasbees.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
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

import java.util.List;

/** Top-of-screen panel showing honey level and stored bees of the hive under the crosshair. */
public class HiveInfoOverlay implements IGuiOverlay {
    public static final String ID = "hive_info";
    private static final int ICON = 14;
    private static final int ROW = 16;
    private static final int PAD = 4;
    private static final int TOP = 6;
    private static final int BACKGROUND = 0x90000000;
    private static final int TEXT = 0xFFFFFF;
    private static final int HONEY_COLOR = 0xFFC83C;

    @Override
    public void render(ForgeGui gui, PoseStack poseStack, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = gui.getMinecraft();
        if (minecraft.options.hideGui || minecraft.level == null || minecraft.screen != null) return;
        HitResult hit = minecraft.hitResult;
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) return;
        BlockState state = minecraft.level.getBlockState(blockHit.getBlockPos());
        if (!HiveInfo.isHive(state)) return;

        Font font = gui.getFont();
        Component title = state.getBlock().getName();
        Component honey = HiveInfo.honeyText(HiveInfo.honeyLevel(state));
        List<ResourceLocation> bees = HiveInfo.occupants(minecraft.level.getBlockEntity(blockHit.getBlockPos()));
        Component beesHeader = bees.isEmpty()
                ? HiveInfo.emptyText()
                : HiveInfo.beesText(bees.size(), HiveInfo.maxOccupants(state.getBlock()));

        int width = Math.max(font.width(title), Math.max(font.width(honey), font.width(beesHeader)));
        for (ResourceLocation bee : bees) {
            width = Math.max(width, ICON + PAD + font.width(HiveInfo.beeName(bee)));
        }
        width += PAD * 2;
        int height = PAD + 3 * (font.lineHeight + 2) + bees.size() * ROW + PAD;
        int left = (screenWidth - width) / 2;
        int y = TOP;

        GuiComponent.fill(poseStack, left, y, left + width, y + height, BACKGROUND);
        y += PAD;
        GuiComponent.drawCenteredString(poseStack, font, title, screenWidth / 2, y, TEXT);
        y += font.lineHeight + 2;
        GuiComponent.drawCenteredString(poseStack, font, honey, screenWidth / 2, y, HONEY_COLOR);
        y += font.lineHeight + 2;
        GuiComponent.drawCenteredString(poseStack, font, beesHeader, screenWidth / 2, y, TEXT);
        y += font.lineHeight + 2;
        for (ResourceLocation bee : bees) {
            drawBeeRow(poseStack, font, bee, left + PAD, y);
            y += ROW;
        }
    }

    static void drawBeeRow(PoseStack poseStack, Font font, ResourceLocation bee, int x, int y) {
        ResourceLocation texture = HiveInfo.iconTexture(bee);
        if (texture != null) {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, texture);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.enableBlend();
            GuiComponent.blit(poseStack, x, y, ICON, ICON, HiveInfo.FACE_U, HiveInfo.FACE_V,
                    HiveInfo.FACE_SIZE, HiveInfo.FACE_SIZE, HiveInfo.TEXTURE_SIZE, HiveInfo.TEXTURE_SIZE);
        }
        font.draw(poseStack, HiveInfo.beeName(bee), x + ICON + PAD, y + (ICON - font.lineHeight) / 2 + 1, TEXT);
    }
}
