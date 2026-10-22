package com.evohints.client;

import com.cobblemon.mod.common.pokemon.RenderablePokemon;
import com.cobblemon.mod.common.pokemon.Species;

import java.util.List;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * One evolution path shown in the tooltip.
 *
 * @param targetSpecies the resolved target species, or {@code null} when the result could not be resolved
 * @param renderable    a renderable version of the target, or {@code null} when the target could not be resolved
 * @param requirements  human-readable explanations of what the evolution requires
 */
public record EvolutionHint(
        @Nullable Species targetSpecies,
        @Nullable RenderablePokemon renderable,
        List<Component> requirements
) {
}
