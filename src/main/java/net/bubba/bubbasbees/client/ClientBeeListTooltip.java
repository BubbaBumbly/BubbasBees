package net.bubba.bubbasbees.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import net.bubba.bubbasbees.blockentities.HiveOccupant;
import net.bubba.bubbasbees.util.HiveInfo;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

/** Renders the bees stored in a hive item inside its tooltip: one row per bee type with face icon, name and count. */
public class ClientBeeListTooltip implements ClientTooltipComponent {
    private static final int COUNT_GAP = 6;
    private static final int NAME_COLOR = 0xFFFFFF;
    private static final int COUNT_COLOR = 0xAAAAAA;
    private static final int FULL_BRIGHT = 0xF000F0;
    private final Map<ResourceLocation, Integer> counts = new LinkedHashMap<>();

    public ClientBeeListTooltip(BeeListTooltip data) {
        for (HiveOccupant bee : data.bees()) {
            counts.merge(bee.id(), 1, Integer::sum);
        }
    }

    @Override
    public int getHeight() {
        return counts.size() * HiveInfoOverlay.ROW;
    }

    @Override
    public int getWidth(Font font) {
        int countWidth = 0;
        for (int count : counts.values()) {
            if (count > 1) countWidth = Math.max(countWidth, COUNT_GAP + font.width(HiveInfo.countText(count)));
        }
        return nameColumnWidth(font) + countWidth;
    }

    /** Names go through the text pass: drawn from renderImage they end up behind the tooltip background. */
    @Override
    public void renderText(Font font, int x, int y, Matrix4f matrix, MultiBufferSource.BufferSource bufferSource) {
        int countX = x + nameColumnWidth(font) + COUNT_GAP;
        int rowY = y;
        for (Map.Entry<ResourceLocation, Integer> entry : counts.entrySet()) {
            font.drawInBatch(HiveInfo.beeName(entry.getKey()), x + HiveInfoOverlay.ICON + HiveInfoOverlay.GAP, rowY + 1,
                    NAME_COLOR, true, matrix, bufferSource, false, 0, FULL_BRIGHT);
            if (entry.getValue() > 1) {
                Component count = HiveInfo.countText(entry.getValue());
                font.drawInBatch(count, countX, rowY + 1, COUNT_COLOR, true, matrix, bufferSource, false, 0, FULL_BRIGHT);
            }
            rowY += HiveInfoOverlay.ROW;
        }
    }

    @Override
    public void renderImage(Font font, int x, int y, PoseStack poseStack, ItemRenderer itemRenderer, int blitOffset) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.0D, blitOffset);
        int rowY = y;
        for (ResourceLocation bee : counts.keySet()) {
            HiveInfoOverlay.drawBeeIcon(poseStack, bee, x, rowY);
            rowY += HiveInfoOverlay.ROW;
        }
        poseStack.popPose();
    }

    private int nameColumnWidth(Font font) {
        int width = 0;
        for (ResourceLocation bee : counts.keySet()) {
            width = Math.max(width, HiveInfoOverlay.ICON + HiveInfoOverlay.GAP + font.width(HiveInfo.beeName(bee)));
        }
        return width;
    }
}
