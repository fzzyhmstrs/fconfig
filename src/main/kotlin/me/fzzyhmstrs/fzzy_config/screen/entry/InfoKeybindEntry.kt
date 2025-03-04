/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.screen.entry

import me.fzzyhmstrs.fzzy_config.cast
import me.fzzyhmstrs.fzzy_config.screen.decoration.Decorated
import me.fzzyhmstrs.fzzy_config.screen.widget.DynamicListWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.TextureIds
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomButtonWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomMultilineTextWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomPressableWidget
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.Translatable
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedKeybind
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.Selectable
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.Widget
import net.minecraft.util.Colors
import net.minecraft.util.Formatting
import kotlin.math.max

internal class InfoKeybindEntry(parentElement: DynamicListWidget, index: Int, scope: String, keybind: ValidatedKeybind):
        DynamicListWidget.Entry(
            parentElement,
            Translatable.Result("fc.button.info.$scope".translate(), "fc.button.info.$scope.desc".translate().formatted(Formatting.ITALIC, Formatting.GRAY)),
            DynamicListWidget.Scope(scope))
{

    private val odd = index % 2 != 0
    private val description = CustomMultilineTextWidget(this.texts.desc ?: FcText.empty(), leftPadding = 10)
    private val restore = CustomButtonWidget.builder(TextureIds.RESTORE_LANG) { keybind.restore() }
        .size(20, 20)
        .textures(TextureIds.RESTORE_SET)
        .activeSupplier { !keybind.isDefault() }
        .narrationSupplier(CustomButtonWidget.ACTIVE_ONLY_ACTIVE_NARRATION_SUPPLIER)
        .tooltipSupplier { active -> if (active) TextureIds.RESTORE_LANG else FcText.EMPTY }
        .noMessage()
        .build()
    private val widget = keybind.widgetEntry()

    init {
        widget.width = 105
        description.width = this.w.get() - 130
    }

    override var h: Int
        get() = max(20, 10 + description.height)
        set(value) {}
    private val selectables: List<SelectableElement> = listOf(widget, restore).cast()
    private val children = mutableListOf(widget, restore)

    override fun selectableChildren(): List<SelectableElement> {
        return selectables
    }

    override fun children(): MutableList<out Element> {
        return children
    }

    override fun renderEntry(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, hovered: Boolean, focused: Boolean, delta: Float) {
        restore.setPosition(x + width - 20, y)
        restore.render(context, mouseX, mouseY, delta)
        widget.setPosition(x + width - 125, y)
        widget.render(context, mouseX, mouseY, delta)
        description.setDimensionsAndPosition(width - 130, 0, x, y + 10)
        description.render(context, mouseX, mouseY, delta)
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, this.texts.name, x, y, if (hovered || focused) -171 else -1)
    }

    override fun renderHighlight(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, hovered: Boolean, focused: Boolean, delta: Float) {
        if (odd)
            context.fill(x - 2, y - 2, x + width + 2, y + height + 2, 1684300900)
    }

    override fun renderExtras(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, hovered: Boolean, focused: Boolean, delta: Float) {
        if (hovered || focused)
            context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, ">", x - 8, y, -171)
    }

    override fun appendNarrations(builder: NarrationMessageBuilder) {
        super.appendNarrations(builder)
        this.texts.desc?.let { builder.put(NarrationPart.HINT, it.string) }
    }
}
