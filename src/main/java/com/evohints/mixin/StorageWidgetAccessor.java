package com.evohints.mixin;

import com.cobblemon.mod.common.client.gui.pc.BoxStorageSlot;
import com.cobblemon.mod.common.client.gui.pc.PartyStorageSlot;
import com.cobblemon.mod.common.client.gui.pc.StorageWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.ArrayList;

/**
 * Exposes Cobblemon's private storage slot lists so the hover tooltip can find
 * the slot under the mouse without relying on the preview Pokémon.
 */
@Mixin(StorageWidget.class)
public interface StorageWidgetAccessor {
    @Accessor("partySlots")
    ArrayList<PartyStorageSlot> evohints$getPartySlots();

    @Accessor("boxSlots")
    ArrayList<BoxStorageSlot> evohints$getBoxSlots();
}
