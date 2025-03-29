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
import me.fzzyhmstrs.fzzy_config.impl.config.KeybindsConfig
import me.fzzyhmstrs.fzzy_config.impl.config.SearchConfig
import me.fzzyhmstrs.fzzy_config.networking.NetworkEventsClient
import me.fzzyhmstrs.fzzy_config.screen.context.*
import me.fzzyhmstrs.fzzy_config.screen.entry.InfoKeybindEntry
import me.fzzyhmstrs.fzzy_config.screen.entry.SearchMenuEntry
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomButtonWidget
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.MultilineTextWidget
import net.minecraft.client.gui.widget.TextWidget
import net.minecraft.client.network.PlayerListEntry
import net.minecraft.command.CommandSource
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.net.URI
import java.util.*
import java.util.function.BiFunction
import kotlin.math.max

//client
object Popups {

    /**
     * Opens a context menu (aka. "right click menu") at the specified position and with the specified context action information as provided by the [ContextResultBuilder]
     *
     * Element position context should be applied to the builder [Position] when possible (e.g. the x/y and width/height of the current focused element). When the position context provided is:
     * - [ContextInput.KEYBOARD]: The popup will appear inline with the x position of the element data of the Position, and slightly below the bottom of the element, unless bounded by the bottom of the screen
     * - [ContextInput.MOUSE]: The popup will appear below and to the right of the mouse position unless bounded by the screen.
     * @param builder [ContextResultBuilder] - a populated builder of context actions. Groups and entries will be displayed in the order they were applied to the builder.
     * @param immediate Optional boolean, default false. If true, will open the popup immediately instead of on the next tick.
     * @author fzzyhmstrs
     * @since 0.6.1, immediate added 0.6.6
     */
    @JvmOverloads
    fun openContextMenuPopup(builder: ContextResultBuilder, immediate: Boolean = false) {
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
            .closeAndPassOnClick()
            .onClick { _, _, over, button ->
                if (ContextType.CONTEXT_MOUSE.relevant(button, ctrl = false, shift = false, alt = false) && !over) {
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
        if (immediate)
            PopupWidget.pushImmediate(popup.build())
        else
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

    internal fun openGotoPopup(entryBuilders: List<BiFunction<DynamicListWidget, Int, out DynamicListWidget.Entry>>, neededWidth: Int, screenHeight: Int) {
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

    internal fun openInfoPopup(screen: Screen) {
        val textRenderer = MinecraftClient.getInstance().textRenderer
        val list: MutableList<BiFunction<DynamicListWidget, Int, out DynamicListWidget.Entry>> = mutableListOf()
        list.add { dlw, i -> InfoKeybindEntry(dlw, i, "page_up", KeybindsConfig.INSTANCE.pageUp) }
        list.add { dlw, i -> InfoKeybindEntry(dlw, i, "page_down", KeybindsConfig.INSTANCE.pageDown) }
        list.add { dlw, i -> InfoKeybindEntry(dlw, i, "home", KeybindsConfig.INSTANCE.home) }
        list.add { dlw, i -> InfoKeybindEntry(dlw, i, "end", KeybindsConfig.INSTANCE.end) }
        list.add { dlw, i -> InfoKeybindEntry(dlw, i, "copy", KeybindsConfig.INSTANCE.copy) }
        list.add { dlw, i -> InfoKeybindEntry(dlw, i, "paste", KeybindsConfig.INSTANCE.paste) }
        list.add { dlw, i -> InfoKeybindEntry(dlw, i, "find", KeybindsConfig.INSTANCE.find) }
        list.add { dlw, i -> InfoKeybindEntry(dlw, i, "save", KeybindsConfig.INSTANCE.save) }
        list.add { dlw, i -> InfoKeybindEntry(dlw, i, "undo", KeybindsConfig.INSTANCE.undo) }
        list.add { dlw, i -> InfoKeybindEntry(dlw, i, "context_keyboard", KeybindsConfig.INSTANCE.contextKeyboard) }
        list.add { dlw, i -> InfoKeybindEntry(dlw, i, "context_mouse", KeybindsConfig.INSTANCE.contextMouse) }
        list.add { dlw, i -> InfoKeybindEntry(dlw, i, "back", KeybindsConfig.INSTANCE.back) }
        list.add { dlw, i -> InfoKeybindEntry(dlw, i, "search", KeybindsConfig.INSTANCE.search) }
        list.add { dlw, i -> InfoKeybindEntry(dlw, i, "info", KeybindsConfig.INSTANCE.info) }
        list.add { dlw, i -> InfoKeybindEntry(dlw, i, "full_exit", KeybindsConfig.INSTANCE.fullExit) }
        val listWidget = DynamicListWidget(MinecraftClient.getInstance(), list, 0, 0, 10000, 0, DynamicListWidget.ListSpec(leftPadding = 10, rightPadding = 4, listNarrationKey = "fc.narrator.position.list"))
        val popup = PopupWidget.Builder("fc.button.info".translate())
            .addDivider()
            .add("header", ClickableTextWidget(screen, "fc.button.info.fc".translate("Fzzy Config".lit().styled { style ->
                style.withFormatting(Formatting.AQUA, Formatting.UNDERLINE)
                    .withClickEvent(ClickEvent.OpenUrl(URI.create("https://moddedmc.wiki/en/project/fzzy-config/docs")))
                    .withHoverEvent(HoverEvent.ShowText("fc.button.info.fc.tip".translate()))
            }), textRenderer), LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_CENTER)
            .addDivider()
            .add("keybinds", listWidget, LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_JUSTIFY_WEAK)
            .addDivider()
            .add("alert", TextWidget("fc.button.info.alert".translate(), textRenderer), LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_CENTER)
            .addDoneWidget()
            .widthFunction { sw, _ -> (sw * 0.92).toInt() }
            .heightFunction { sh, h ->
                val newHeight = (sh * 0.9).toInt()
                val heightDelta = newHeight - h
                listWidget.height += heightDelta
                newHeight
            }
            .onClose { KeybindsConfig.INSTANCE.save() }
            .build()
        PopupWidget.push(popup)
    }

    internal fun openSearchMenuPopup() {
        val list: MutableList<BiFunction<DynamicListWidget, Int, out DynamicListWidget.Entry>> = mutableListOf()
        list.add { dlw, _ -> SearchMenuEntry(dlw, "modifier", SearchConfig.INSTANCE.modifier.widgetEntry()) }
        list.add { dlw, _ -> SearchMenuEntry(dlw, "behavior", SearchConfig.INSTANCE.behavior.widgetEntry()) }
        list.add { dlw, _ -> SearchMenuEntry(dlw, "clearSearch", SearchConfig.INSTANCE.clearSearch.widgetEntry()) }
        val listWidget = DynamicListWidget(MinecraftClient.getInstance(), list, 0, 0, 10000, 0, DynamicListWidget.ListSpec(leftPadding = 10, rightPadding = 4, listNarrationKey = "fc.narrator.position.list"))
        val popup = PopupWidget.Builder(TextureIds.MENU_LANG)
            .add("search_settings", listWidget, LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_JUSTIFY_WEAK)
            .addDoneWidget()
            .widthFunction { sw, _ -> (sw * 0.92).toInt() }
            .heightFunction { sh, h ->
                val newHeight = (sh * 0.9).toInt()
                val heightDelta = newHeight - h
                listWidget.height += heightDelta
                newHeight
            }
            .onClose { SearchConfig.INSTANCE.save() }
            .build()
        PopupWidget.push(popup)
    }
}