package com.evohints.client;

import com.cobblemon.mod.common.client.gui.PokemonGuiUtilsKt;
import com.cobblemon.mod.common.client.gui.ProfileTransformType;
import com.cobblemon.mod.common.client.gui.pc.BoxStorageSlot;
import com.cobblemon.mod.common.client.gui.pc.PCGUI;
import com.cobblemon.mod.common.client.gui.pc.PartyStorageSlot;
import com.cobblemon.mod.common.client.gui.pc.StorageSlot;
import com.cobblemon.mod.common.client.gui.pc.StorageWidget;
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState;
import com.cobblemon.mod.common.entity.PoseType;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.cobblemon.mod.common.util.math.QuaternionUtilsKt;
import com.evohints.EvolutionHints;
import com.evohints.mixin.StorageWidgetAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the slot indicator and the hover tooltip in Cobblemon's PC screen.
 */
public final class EvolutionHintsRenderer {
    private static final ResourceLocation INDICATOR_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EvolutionHints.MODID, "textures/gui/evolution_indicator.png");

    private static final int PREVIEW_SIZE = 22;
    private static final int TEXT_WIDTH = 190;

    private static final int PANEL_BACKGROUND = 0xF2181818;
    private static final int PANEL_BORDER = 0xFF4A4A4A;
    private static final int PANEL_ACCENT = 0xFF4CC35A;
    private static final int DIVIDER = 0x35FFFFFF;
    private static final int TEXT_PRIMARY = 0xFFFFFFFF;
    private static final int TEXT_SECONDARY = 0xFFB8B8B8;
    private static final int TEXT_UNKNOWN = 0xFF9A9A9A;

    private EvolutionHintsRenderer() {
    }

    public static void renderIndicator(GuiGraphics context, Pokemon pokemon, int slotX, int slotY) {
        // Stay out of the way until the player asks for the hint.
        if (!Screen.hasShiftDown() || !EvolutionHintsResolver.hasEvolution(pokemon)) {
            return;
        }

        int x = slotX + 1;
        int y = slotY + 15;
        PoseStack pose = context.pose();
        pose.pushPose();
        // Cobblemon draws slot overlays at z 500; stay above them.
        pose.translate(0.0D, 0.0D, 550.0D);
        context.fill(x, y, x + 9, y + 9, 0x66000000);
        // The texture is 16x16; sample all of it and scale it into the 9x9 badge.
        // The shorter overload treats width/height as both the screen size and the
        // sampled region, which cropped the icon to its top-left quarter.
        context.blit(INDICATOR_TEXTURE, x, y, 9, 9, 0.0F, 0.0F, 16, 16, 16, 16);
        pose.popPose();
    }

    public static void renderTooltip(PCGUI gui, GuiGraphics context, int mouseX, int mouseY) {
        if (!Screen.hasShiftDown()) {
            return;
        }

        StorageSlot hovered = findHoveredSlot(gui, mouseX, mouseY);
        if (hovered == null) {
            return;
        }

        Pokemon pokemon = hovered.getPokemon();
        if (pokemon == null) {
            return;
        }

        List<EvolutionHint> hints = EvolutionHintsResolver.resolve(pokemon);
        if (hints.isEmpty()) {
            return;
        }

        drawTooltip(context, hints, mouseX, mouseY);
    }

    private static StorageSlot findHoveredSlot(PCGUI gui, int mouseX, int mouseY) {
        StorageWidget storage = gui.getStorage();
        if (storage == null) {
            return null;
        }

        StorageWidgetAccessor accessor = (StorageWidgetAccessor) (Object) storage;
        for (PartyStorageSlot slot : accessor.evohints$getPartySlots()) {
            if (isHovered(slot, mouseX, mouseY)) {
                return slot;
            }
        }
        for (BoxStorageSlot slot : accessor.evohints$getBoxSlots()) {
            if (isHovered(slot, mouseX, mouseY)) {
                return slot;
            }
        }
        return null;
    }

    private static boolean isHovered(StorageSlot slot, int mouseX, int mouseY) {
        return slot.getPokemon() != null
                && slot.shouldRender()
                && slot.isHovered(mouseX, mouseY);
    }

    private static void drawTooltip(GuiGraphics context, List<EvolutionHint> hints, int mouseX, int mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        Component title = Component.translatable("evohints.tooltip.title");

        List<Row> rows = new ArrayList<>(hints.size());
        int panelWidth = Math.max(150, font.width(title) + 12);
        for (EvolutionHint hint : hints) {
            Species species = hint.targetSpecies();
            boolean known = EvolutionHintsResolver.isKnown(species);
            Component name = known
                    ? species.getTranslatedName()
                    : Component.translatable("evohints.unknown_target");

            List<FormattedCharSequence> nameLines = font.split(name, TEXT_WIDTH);
            List<FormattedCharSequence> requirementLines = new ArrayList<>();
            for (Component requirement : hint.requirements()) {
                requirementLines.addAll(font.split(requirement, TEXT_WIDTH));
            }
            if (requirementLines.isEmpty()) {
                requirementLines.addAll(font.split(
                        Component.translatable("evohints.requirement.unknown"), TEXT_WIDTH));
            }

            int textWidth = 0;
            for (FormattedCharSequence line : nameLines) {
                textWidth = Math.max(textWidth, font.width(line));
            }
            for (FormattedCharSequence line : requirementLines) {
                textWidth = Math.max(textWidth, font.width(line));
            }

            int rowHeight = Math.max(23, 2 + (nameLines.size() + requirementLines.size()) * 10);
            rows.add(new Row(known, hint, nameLines, requirementLines, rowHeight));
            panelWidth = Math.max(panelWidth, 34 + textWidth + 8);
        }

        int panelHeight = 21;
        for (Row row : rows) {
            panelHeight += row.height + 3;
        }
        panelHeight += 3;

        int x = mouseX + 12;
        int y = mouseY - 12;
        if (x + panelWidth > context.guiWidth() - 4) {
            x = mouseX - panelWidth - 12;
        }
        if (y + panelHeight > context.guiHeight() - 4) {
            y = context.guiHeight() - panelHeight - 4;
        }
        x = Math.max(4, x);
        y = Math.max(4, y);

        PoseStack pose = context.pose();
        pose.pushPose();
        pose.translate(0.0D, 0.0D, 600.0D);

        context.fill(x - 1, y - 1, x + panelWidth + 1, y + panelHeight + 1, PANEL_BORDER);
        context.fill(x, y, x + panelWidth, y + panelHeight, PANEL_BACKGROUND);
        context.fill(x, y, x + panelWidth, y + 2, PANEL_ACCENT);

        context.drawString(font, title, x + 6, y + 6, TEXT_PRIMARY, true);
        context.fill(x + 5, y + 18, x + panelWidth - 5, y + 19, DIVIDER);

        int rowY = y + 21;
        for (Row row : rows) {
            drawPreview(context, font, x + 5, rowY, row);
            int textY = rowY + 1;
            for (FormattedCharSequence line : row.nameLines) {
                context.drawString(font, line, x + 32, textY, row.known ? TEXT_PRIMARY : TEXT_UNKNOWN, true);
                textY += 10;
            }
            for (FormattedCharSequence line : row.requirementLines) {
                context.drawString(font, line, x + 32, textY, TEXT_SECONDARY, false);
                textY += 10;
            }
            rowY += row.height + 3;
        }

        pose.popPose();
    }

    private static void drawPreview(GuiGraphics context, Font font, int x, int y, Row row) {
        context.fill(x, y, x + PREVIEW_SIZE, y + PREVIEW_SIZE, 0x66000000);
        context.fill(x, y, x + PREVIEW_SIZE, y + 1, PANEL_BORDER);
        context.fill(x, y + PREVIEW_SIZE - 1, x + PREVIEW_SIZE, y + PREVIEW_SIZE, PANEL_BORDER);
        context.fill(x, y, x + 1, y + PREVIEW_SIZE, PANEL_BORDER);
        context.fill(x + PREVIEW_SIZE - 1, y, x + PREVIEW_SIZE, y + PREVIEW_SIZE, PANEL_BORDER);

        if (row.known && row.hint.renderable() != null) {
            PoseStack pose = context.pose();
            boolean scissorEnabled = false;
            boolean posePushed = false;
            try {
                context.enableScissor(x + 1, y + 1, x + PREVIEW_SIZE - 1, y + PREVIEW_SIZE - 1);
                scissorEnabled = true;
                pose.pushPose();
                posePushed = true;
                pose.translate(x + PREVIEW_SIZE / 2.0D, y + 1.0D, 0.0D);
                pose.scale(2.2F, 2.2F, 1.0F);

                FloatingState state = new FloatingState();
                PokemonGuiUtilsKt.drawProfilePokemon(
                        row.hint.renderable(),
                        pose,
                        QuaternionUtilsKt.fromEulerXYZDegrees(new Quaternionf(), new Vector3f(13.0F, 35.0F, 0.0F)),
                        PoseType.PROFILE,
                        state,
                        0.0F,
                        4.5F,
                        ProfileTransformType.PROFILE,
                        false,
                        1.0F,
                        1.0F,
                        1.0F,
                        1.0F,
                        0.0F,
                        0.0F,
                        13
                );
                return;
            } catch (Exception exception) {
                EvolutionHints.LOGGER.debug("Could not render an evolution target preview", exception);
            } finally {
                if (posePushed) {
                    pose.popPose();
                }
                if (scissorEnabled) {
                    context.disableScissor();
                }
            }
        }

        Component questionMark = Component.literal("?");
        int textX = x + (PREVIEW_SIZE - font.width(questionMark)) / 2;
        int textY = y + (PREVIEW_SIZE - font.lineHeight) / 2 + 1;
        context.drawString(font, questionMark, textX, textY, TEXT_UNKNOWN, false);
    }

    private record Row(
            boolean known,
            EvolutionHint hint,
            List<FormattedCharSequence> nameLines,
            List<FormattedCharSequence> requirementLines,
            int height
    ) {
    }
}
