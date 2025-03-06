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
import me.fzzyhmstrs.fzzy_config.screen.context.*
import me.fzzyhmstrs.fzzy_config.screen.widget.DynamicListWidget.Entry
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomButtonWidget
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.RenderUtil
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.widget.MultilineTextWidget
import net.minecraft.client.network.PlayerListEntry
import net.minecraft.command.CommandSource
import net.minecraft.text.Text
import java.util.*
import java.util.function.BiFunction
import kotlin.math.max

//client
object Popups {

    init {
        RenderUtil.addBackground("widget/popup/background_right_click".fcId(), RenderUtil.Background(4, 4, 64, 64))
    }

    /**
     * Opens a context menu (aka. "right click menu") at the specified position and with the specified context action information as provided by the [ContextResultBuilder]
     *
     * Element position context should be applied to the builder [Position] when possible (e.g. the x/y and width/height of the current focused element). When the position context provided is:
     * - [ContextInput.KEYBOARD]: The popup will appear inline with the x position of the element data of the Position, and slightly below the bottom of the element, unless bounded by the bottom of the screen
     * - [ContextInput.MOUSE]: The popup will appear below and to the right of the mouse position unless bounded by the screen.
     * @param builder [ContextResultBuilder] - a populated builder of context actions. Groups and entries will be displayed in the order they were applied to the builder.
     * @author fzzyhmstrs
     * @since 0.6.1
     */
    fun openContextMenuPopup(builder: ContextResultBuilder) {
        val positionContext = builder.position()
        val popup = PopupWidget.Builder("fc.config.right_click".translate(), 2, 2)
            .positionX(PopupWidget.Builder.absScreen(
                if (positionContext.contextInput == ContextInput.KEYBOARD)
                    positionContext.x
                else
                    positionContext.mX))
            .positionY(PopupWidget.Builder.absScreen(
                if (positionContext.contextInput == ContextInput.KEYBOARD)
                    positionContext.y
                else
                    positionContext.mY))
            .background("widget/popup/background_right_click".fcId())
            .noBlur()
            .onClick { mX, mY, over, button ->
                if (ContextType.CONTEXT_MOUSE.relevant(button, ctrl = false, shift = false, alt = false) && !over) {
                    PopupWidget.pop(mX, mY)
                    PopupWidget.ClickResult.PASS
                } else {
                    PopupWidget.ClickResult.USE
                }
            }
        for ((group, actions) in builder.build()) {
            if (actions.isEmpty()) continue
            popup.addDivider()
            for ((type, action) in actions) {
                popup.add(
                    "${group}_$type",
                    ContextActionWidget(action, positionContext, ContextActionWidget.getNeededWidth(action)),
                    LayoutWidget.Position.BELOW,
                    LayoutWidget.Position.ALIGN_LEFT
                )
            }
        }
        PopupWidget.push(popup.build())
    }

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

    internal fun openEntryForwardingPopup(field: ValidatedField<*>) {
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
                CustomButtonWidget.builder(forwardText) { forwardUpdate(field, playerEntries[player]); PopupWidget.pop() }.size(buttonWidth, 20).activeSupplier { playerEntries.containsKey(player) }.build(),
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

    internal fun openGotoPopup(entryBuilders: List<BiFunction<DynamicListWidget, Int, out Entry>>, neededWidth: Int, screenHeight: Int) {
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

        val popup = PopupWidget.Builder(TextureIds.GOTO_LANG)
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