/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.screen.widget

import com.mojang.brigadier.suggestion.SuggestionsBuilder
import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.networking.NetworkEventsClient
import me.fzzyhmstrs.fzzy_config.screen.context.ContextInput
import me.fzzyhmstrs.fzzy_config.screen.context.Position
import me.fzzyhmstrs.fzzy_config.screen.widget.DynamicListWidget.Entry
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomButtonWidget
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.widget.MultilineTextWidget
import net.minecraft.client.network.PlayerListEntry
import net.minecraft.command.CommandSource
import net.minecraft.text.Text
import java.util.*
import java.util.function.Function
import kotlin.math.max

//client
object Popups {

    internal fun openConfirmPopup(b: Position, desc: Text, restore: Runnable) {
        val client = MinecraftClient.getInstance()
        val confirmText = "fc.button.restore.confirm".translate()
        val confirmTextWidth = max(50, client.textRenderer.getWidth(confirmText) + 8)
        val cancelText = "fc.button.cancel".translate()
        val cancelTextWidth = max(50, client.textRenderer.getWidth(cancelText) + 8)
        val buttonWidth = max(confirmTextWidth, cancelTextWidth)
        val rX = if(b.contextInput == ContextInput.KEYBOARD) b.x else b.mX
        val rY = if(b.contextInput == ContextInput.KEYBOARD) b.y else b.mY

        val popup = PopupWidget.Builder("fc.button.restore".translate())
            .addDivider()
            .add("confirm_text", MultilineTextWidget(desc, MinecraftClient.getInstance().textRenderer).setCentered(true).setMaxWidth(buttonWidth + 4 + buttonWidth), LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_CENTER)
            .add("confirm_button", CustomButtonWidget.builder(confirmText) { restore.run(); PopupWidget.pop() }.size(buttonWidth, 20).build(), LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_LEFT)
            .add("cancel_button", CustomButtonWidget.builder(cancelText) { PopupWidget.pop() }.size(buttonWidth, 20).build(), "confirm_text", LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_RIGHT)
            .positionX(PopupWidget.Builder.popupContext { w -> rX + b.width/2 - w/2 })
            .positionY(PopupWidget.Builder.popupContext { h -> rY - h + 28 })
            .width(buttonWidth + 4 + buttonWidth + 16)
            .build()
        PopupWidget.push(popup)
    }

    internal fun  openEntryForwardingPopup(field: ValidatedField<*>) {
        val client = MinecraftClient.getInstance()
        val playerEntries = client.player?.networkHandler?.playerList?.associateBy { it.profile.name } ?: return
        val validator = EntryValidator.Builder<String>().both({ s -> playerEntries.containsKey(s)}).buildValidator()
        var player = ""
        val forwardText = "fc.button.forward.confirm".translate()
        val forwardTextWidth = max(50, client.textRenderer.getWidth(forwardText) + 8)
        val cancelText = "fc.button.cancel".translate()
        val cancelTextWidth = max(50, client.textRenderer.getWidth(cancelText) + 8)
        val buttonWidth = max(forwardTextWidth, cancelTextWidth)
        val popup = PopupWidget.Builder("fc.button.forward".translate())
            .addDivider()
            .add(
                "desc",
                MultilineTextWidget("fc.button.forward.active".translate(), MinecraftClient.getInstance().textRenderer).setCentered(true).setMaxWidth(buttonWidth + 4 + buttonWidth),
                LayoutWidget.Position.BELOW,
                LayoutWidget.Position.ALIGN_CENTER)
            .add(
                "player_finder",
                SuggestionBackedTextFieldWidget(
                    110, 20,
                    { player },
                    ChoiceValidator.any(),
                    validator,
                    { s -> player = s},
                    { s, cursor, choiceValidator ->
                        CommandSource.suggestMatching(playerEntries.keys.filter { choiceValidator.validateEntry(it, EntryValidator.ValidationType.STRONG).isValid() }, s.substring(0, cursor).let{ SuggestionsBuilder(it, it.lowercase(
                            Locale.ROOT), 0) })
                    }),
                LayoutWidget.Position.BELOW,
                LayoutWidget.Position.ALIGN_LEFT)
            .add(
                "forward_button",
                ActiveButtonWidget(forwardText, buttonWidth, 20, { playerEntries.containsKey(player) }, { forwardUpdate(field, playerEntries[player]); PopupWidget.pop() }),
                LayoutWidget.Position.BELOW,
                LayoutWidget.Position.ALIGN_LEFT)
            .add(
                "cancel_button",
                CustomButtonWidget.builder(cancelText) { PopupWidget.pop() }.size(buttonWidth, 20).build(), "forward_button",
                LayoutWidget.Position.ALIGN_RIGHT,
                LayoutWidget.Position.HORIZONTAL_TO_TOP_EDGE)
            .build()
        PopupWidget.push(popup)
    }

    private fun forwardUpdate(field: ValidatedField<*>, playerListEntry: PlayerListEntry?) {
        if (playerListEntry == null) return
        val update = ConfigApiImpl.serializeEntry(field, mutableListOf())
        val id = playerListEntry.profile.id
        val key = field.getEntryKey()
        val summary = field.get().toString()
        NetworkEventsClient.forwardSetting(update, id, key, summary)
    }

    internal fun openGotoPopup(entryBuilders: List<Function<DynamicListWidget, out Entry>>, neededWidth: Int, screenHeight: Int) {
        //height - padding * 2 - text spacing - text - divider spacing - divider
        val client = MinecraftClient.getInstance()
        val anchors = DynamicListWidget(
            client,
            entryBuilders,
            0, 0,
            neededWidth, 100,
            DynamicListWidget.ListSpec(
                leftPadding = 0,
                rightPadding = -4,
                verticalPadding = 0,
                hideScrollBar = true,
                listNarrationKey = "fc.narrator.position.list"))
        val maxHeight = screenHeight - 16 - 4 - 9 - 4 - 5
        anchors.fitToContent(maxHeight)

        val popup = PopupWidget.Builder("fc.button.goto".translate())
            .addDivider()
            .add("anchors", anchors, LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_LEFT)
            .positionX(PopupWidget.Builder.absScreen(0))
            .positionY(PopupWidget.Builder.popupContext { h -> screenHeight - h })
            .background("widget/popup/background_right_click".fcId())
            .noBlur()
            .build()
        PopupWidget.push(popup)
    }

}