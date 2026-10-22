package com.evohints.mixin;

import com.cobblemon.mod.common.client.gui.pc.PCGUI;
import com.evohints.client.EvolutionHintsRenderer;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/**
 * Compatibility for deferred widget tooltips in Cobblemon's PC, such as the
 * one added by Chiselmon.
 *
 * <p>Those tooltips are queued while the storage slots render and are drawn by
 * {@link Screen#renderWithTooltip} after {@link PCGUI#render} returns. We wrap
 * that draw call so the evolution summary can be appended to the tooltip and so
 * the tooltip sits above our slot badges. The fixed z offsets match the ones
 * used in {@link EvolutionHintsRenderer}.</p>
 */
@Mixin(Screen.class)
public abstract class ScreenMixin {
    @WrapOperation(
            method = "renderWithTooltip",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;renderTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;II)V"
            )
    )
    private void evohints$renderDeferredTooltip(
            GuiGraphics context,
            Font font,
            List<FormattedCharSequence> lines,
            ClientTooltipPositioner positioner,
            int mouseX,
            int mouseY,
            Operation<Void> original
    ) {
        if (!((Object) this instanceof PCGUI gui)) {
            original.call(context, font, lines, positioner, mouseX, mouseY);
            return;
        }

        List<FormattedCharSequence> augmented =
                EvolutionHintsRenderer.augmentDeferredTooltip(gui, font, lines, mouseX, mouseY);

        PoseStack pose = context.pose();
        pose.pushPose();
        try {
            // Deferred tooltips render at z 400, while Cobblemon slot overlays
            // use z 500 and our badges use z 550. Lift the tooltip above both.
            pose.translate(0.0D, 0.0D, 300.0D);
            original.call(context, font, augmented, positioner, mouseX, mouseY);
        } finally {
            pose.popPose();
        }
    }
}
