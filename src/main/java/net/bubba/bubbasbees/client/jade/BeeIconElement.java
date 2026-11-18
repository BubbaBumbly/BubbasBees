package net.bubba.bubbasbees.client.jade;

import net.bubba.bubbasbees.client.HiveInfoOverlay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec2;
import snownee.jade.api.ui.Element;

/** A bee's face icon as a Jade tooltip element, drawn the same way as in HiveInfoOverlay. */
public class BeeIconElement extends Element {
    private static final Vec2 SIZE = new Vec2(HiveInfoOverlay.ICON + HiveInfoOverlay.GAP, HiveInfoOverlay.ICON);
    private final ResourceLocation bee;

    public BeeIconElement(ResourceLocation bee) {
        this.bee = bee;
    }

    @Override
    public Vec2 getSize() {
        return SIZE;
    }

    @Override
    public void render(GuiGraphics graphics, float x, float y, float maxX, float maxY) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0D);
        HiveInfoOverlay.drawBeeIcon(graphics, bee, 0, 0);
        graphics.pose().popPose();
    }
}
