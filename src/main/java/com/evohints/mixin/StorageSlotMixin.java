package com.evohints.mixin;

import com.cobblemon.mod.common.client.gui.pc.StorageSlot;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.evohints.client.EvolutionHintsRenderer;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws a small evolution marker in the bottom-left corner of populated slots.
 */
@Mixin(StorageSlot.class)
public abstract class StorageSlotMixin {
    @Inject(method = "renderSlot(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("RETURN"))
    private void evohints$renderIndicator(GuiGraphics context, int posX, int posY,
                                          float partialTicks, CallbackInfo callbackInfo) {
        StorageSlot slot = (StorageSlot) (Object) this;
        Pokemon pokemon = slot.getPokemon();
        if (pokemon != null) {
            EvolutionHintsRenderer.renderIndicator(context, pokemon, posX, posY);
        }
    }
}
