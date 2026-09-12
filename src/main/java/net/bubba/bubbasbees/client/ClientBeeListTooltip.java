package net.bubba.bubbasbees.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.bubba.bubbasbees.blockentities.HiveOccupant;
import net.bubba.bubbasbees.util.HiveInfo;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.entity.ItemRenderer;

/** Renders each stored bee as a face icon followed by its name inside an item tooltip. */
public class ClientBeeListTooltip implements ClientTooltipComponent {
    private final BeeListTooltip data;

    public ClientBeeListTooltip(BeeListTooltip data) {
        this.data = data;
    }

    @Override
    public int getHeight() {
        return data.bees().size() * HiveInfoOverlay.ROW;
    }

    @Override
    public int getWidth(Font font) {
        int width = 0;
        for (HiveOccupant bee : data.bees()) {
            width = Math.max(width, HiveInfoOverlay.ICON + HiveInfoOverlay.GAP + font.width(HiveInfo.beeName(bee.id())));
        }
        return width;
    }

    @Override
    public void renderImage(Font font, int x, int y, PoseStack poseStack, ItemRenderer itemRenderer, int blitOffset) {
        int rowY = y;
        for (HiveOccupant bee : data.bees()) {
            HiveInfoOverlay.drawBeeRow(poseStack, font, bee.id(), x, rowY);
            rowY += HiveInfoOverlay.ROW;
        }
    }
}
