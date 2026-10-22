package com.evohints.client;

import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress;
import com.cobblemon.mod.common.api.pokedex.entry.DexEvolutionDisplayKt;
import com.cobblemon.mod.common.api.pokedex.entry.DexEvolutionRequirementDisplay;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.pokemon.evolution.Evolution;
import com.cobblemon.mod.common.api.pokemon.requirement.Requirement;
import com.cobblemon.mod.common.api.storage.player.client.ClientPokedexManager;
import com.cobblemon.mod.common.client.CobblemonClient;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.RenderablePokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.cobblemon.mod.common.pokemon.evolution.variants.BlockClickEvolution;
import com.cobblemon.mod.common.pokemon.evolution.variants.ItemInteractionEvolution;
import com.cobblemon.mod.common.pokemon.evolution.variants.TradeEvolution;
import com.cobblemon.mod.common.pokemon.requirements.FriendshipRequirement;
import com.cobblemon.mod.common.pokemon.requirements.LevelRequirement;
import com.evohints.EvolutionHints;
import com.cobblemon.mod.common.api.conditional.RegistryLikeCondition;
import com.cobblemon.mod.common.api.conditional.RegistryLikeIdentifierCondition;
import com.cobblemon.mod.common.api.conditional.RegistryLikeTagCondition;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

/**
 * Resolves Cobblemon evolution paths into a small, UI-friendly summary.
 *
 * <p>The summary deliberately hides level and friendship thresholds. Cobblemon's
 * own Pokédex evolution displays reveal the exact numbers, which is not wanted
 * here. Everything else is delegated to Cobblemon's requirement display API so
 * modded requirements that provide an icon/tooltip keep working.</p>
 */
public final class EvolutionHintsResolver {
    private static Pokemon cachedPokemon;
    private static String cachedSpecies;
    private static String cachedForm;
    private static List<EvolutionHint> cachedHints = List.of();

    private EvolutionHintsResolver() {
    }

    /**
     * Cheap check used while drawing every storage slot. Cobblemon's
     * {@link Pokemon#getEvolutions()} sorts its result on every call, so use the
     * form's raw evolution set for the per-frame indicator check instead.
     */
    public static boolean hasEvolution(Pokemon pokemon) {
        return !pokemon.getForm().getEvolutions().isEmpty();
    }

    public static List<EvolutionHint> resolve(Pokemon pokemon) {
        String species = pokemon.getSpecies().getResourceIdentifier().toString();
        String form = pokemon.getForm().getName();
        if (cachedPokemon == pokemon && species.equals(cachedSpecies) && form.equals(cachedForm)) {
            return cachedHints;
        }

        List<EvolutionHint> hints;
        try {
            hints = build(pokemon);
        } catch (Exception exception) {
            // Never let malformed addon evolution data break the PC screen.
            EvolutionHints.LOGGER.warn("Could not resolve evolutions for {}", species, exception);
            hints = List.of();
        }
        cachedPokemon = pokemon;
        cachedSpecies = species;
        cachedForm = form;
        cachedHints = hints;
        return hints;
    }

    private static List<EvolutionHint> build(Pokemon pokemon) {
        List<EvolutionHint> hints = new ArrayList<>();
        for (Evolution evolution : pokemon.getEvolutions()) {
            try {
                PokemonProperties result = evolution.getResult();
                String speciesId = result.getSpecies();
                Species target = speciesId == null || speciesId.isBlank()
                        // Form-only results keep the current species.
                        ? pokemon.getSpecies()
                        : findSpecies(speciesId);
                RenderablePokemon renderable = target == null ? null : createRenderable(target, result);
                hints.add(new EvolutionHint(target, renderable, buildRequirements(evolution)));
            } catch (Exception exception) {
                EvolutionHints.LOGGER.debug("Skipping an evolution that could not be resolved", exception);
            }
        }

        if (hints.isEmpty() && hasEvolution(pokemon)) {
            hints.add(new EvolutionHint(
                    null,
                    null,
                    List.of(Component.translatable("evohints.requirement.unknown"))
            ));
        }
        return List.copyOf(hints);
    }

    @Nullable
    private static Species findSpecies(String speciesId) {
        String trimmed = speciesId.trim();
        boolean hasNamespace = trimmed.indexOf(':') >= 0;

        ResourceLocation identifier = hasNamespace
                ? ResourceLocation.tryParse(trimmed)
                : ResourceLocation.tryParse("cobblemon:" + trimmed);
        Species species = identifier == null ? null : PokemonSpecies.getByIdentifier(identifier);

        if (species == null && !hasNamespace) {
            // Some addons define their species without a namespace in evolution data.
            for (Species candidate : PokemonSpecies.getSpecies()) {
                if (candidate.getResourceIdentifier().getPath().equals(trimmed)) {
                    return candidate;
                }
            }
        }
        return species;
    }

    private static RenderablePokemon createRenderable(Species species, PokemonProperties result) {
        Set<String> aspects = new HashSet<>(result.getAspects());
        String formName = result.getForm();
        if (formName != null && !formName.isBlank()) {
            FormData form = species.getFormByName(formName);
            if (form == null) {
                form = species.getFormByShowdownId(formName);
            }
            if (form != null) {
                aspects.addAll(form.getAspects());
            } else {
                // Best effort for forms that only expose an aspect name.
                aspects.add(formName);
            }
        }
        return new RenderablePokemon(species, aspects, ItemStack.EMPTY);
    }

    private static List<Component> buildRequirements(Evolution evolution) {
        List<Component> lines = new ArrayList<>();

        if (evolution instanceof ItemInteractionEvolution || evolution instanceof TradeEvolution) {
            try {
                DexEvolutionRequirementDisplay header = DexEvolutionDisplayKt.getToolTipHeaderIcon(evolution);
                if (header != null) {
                    lines.add(header.getTooltip());
                }
            } catch (Exception exception) {
                EvolutionHints.LOGGER.debug("Could not build an item/trade evolution header", exception);
            }
        }

        if (evolution instanceof BlockClickEvolution blockClick) {
            addBlockRequirement(blockClick, lines);
        }

        boolean levelAdded = false;
        boolean friendshipAdded = false;
        for (Requirement requirement : evolution.getRequirements()) {
            if (requirement instanceof LevelRequirement) {
                if (!levelAdded) {
                    lines.add(Component.translatable("evohints.requirement.level"));
                    levelAdded = true;
                }
                continue;
            }
            if (requirement instanceof FriendshipRequirement) {
                if (!friendshipAdded) {
                    lines.add(Component.translatable("evohints.requirement.friendship"));
                    friendshipAdded = true;
                }
                continue;
            }

            try {
                DexEvolutionRequirementDisplay display = DexEvolutionDisplayKt.resolveRequirementData(requirement);
                if (display != null) {
                    lines.add(display.getTooltip());
                } else {
                    lines.add(Component.translatable("evohints.requirement.custom",
                            requirement.getClass().getSimpleName()));
                }
            } catch (Exception exception) {
                EvolutionHints.LOGGER.debug("Could not build a requirement display for {}",
                        requirement.getClass().getName(), exception);
                lines.add(Component.translatable("evohints.requirement.custom",
                        requirement.getClass().getSimpleName()));
            }
        }

        if (lines.isEmpty()) {
            lines.add(Component.translatable("evohints.requirement.unknown"));
        }
        return List.copyOf(lines);
    }

    private static void addBlockRequirement(BlockClickEvolution evolution, List<Component> lines) {
        try {
            RegistryLikeCondition<Block> context = evolution.getRequiredContext();
            if (context instanceof RegistryLikeIdentifierCondition<?> identifierCondition) {
                ResourceLocation id = identifierCondition.getIdentifier();
                Block block = BuiltInRegistries.BLOCK.get(id);
                Component name = block == Blocks.AIR ? Component.literal(id.toString()) : block.getName();
                lines.add(Component.translatable("evohints.requirement.block", name));
            } else if (context instanceof RegistryLikeTagCondition<?> tagCondition) {
                lines.add(Component.translatable("evohints.requirement.block_tag",
                        Component.literal("#" + tagCondition.getTag().location())));
            }
        } catch (Exception exception) {
            EvolutionHints.LOGGER.debug("Could not build a block evolution requirement", exception);
        }
    }

    /**
     * A target is only revealed once its species has been seen. Form-level
     * knowledge is intentionally not required: seeing the species is enough to
     * avoid spoiling which Pokémon the evolution produces.
     */
    public static boolean isKnown(@Nullable Species species) {
        if (species == null) {
            return false;
        }
        try {
            ClientPokedexManager pokedex = CobblemonClient.INSTANCE.getClientPokedexData();
            return pokedex != null
                    && pokedex.getHighestKnowledgeForSpecies(species.getResourceIdentifier())
                    != PokedexEntryProgress.UNREGISTERED;
        } catch (Exception exception) {
            EvolutionHints.LOGGER.debug("Could not read Pokédex knowledge for {}", species.getResourceIdentifier(), exception);
            return false;
        }
    }
}
