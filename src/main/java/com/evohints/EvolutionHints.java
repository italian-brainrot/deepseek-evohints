package com.evohints;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Client-side Cobblemon addon that adds evolution hints to the PC storage UI.
 */
@Mod(EvolutionHints.MODID)
public final class EvolutionHints {
    public static final String MODID = "evohints";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EvolutionHints() {
        // All behaviour lives in client-side mixins.
    }
}
