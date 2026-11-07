package net.bubba.bubbasbees.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.bubba.bubbasbees.client.RitualBarTitle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Ritual boss bars: a title wider than the screen drops the missing bee names (RitualBarTitle). */
@Mixin(BossHealthOverlay.class)
public abstract class BossHealthOverlayMixin {
    @ModifyExpressionValue(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/LerpingBossEvent;getName()Lnet/minecraft/network/chat/Component;"))
    private Component bubbasBees$fitRitualTitle(Component name) {
        Minecraft mc = Minecraft.getInstance();
        return RitualBarTitle.fit(name, mc.font, mc.getWindow().getGuiScaledWidth());
    }
}
