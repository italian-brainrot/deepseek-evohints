package com.evohints.mixin;

import com.cobblemon.mod.common.client.gui.pc.PCGUI;
import com.evohints.client.EvolutionHintsRenderer;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws the evolution tooltip after every other PC widget, so nothing can paint
 * over it. {@code PCGUI.render} has several early returns, hence RETURN rather
 * than TAIL.
 */
@Mixin(PCGUI.class)
public abstract class PCGUIMixin {
    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("RETURN"))
    private void evohints$renderTooltip(GuiGraphics context, int mouseX, int mouseY,
                                        float delta, CallbackInfo callbackInfo) {
        EvolutionHintsRenderer.renderTooltip((PCGUI) (Object) this, context, mouseX, mouseY);
    }
}
