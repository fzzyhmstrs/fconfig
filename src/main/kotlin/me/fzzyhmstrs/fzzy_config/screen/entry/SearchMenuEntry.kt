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

internal class SearchMenuEntry(parentElement: DynamicListWidget, scope: String, private val widget: ClickableWidget):
        DynamicListWidget.Entry(
            parentElement,
            Translatable.createScopedResult("fc.search.id.$scope", "fc.search.$scope".translate(), "fc.search.$scope.desc".translate().formatted(Formatting.ITALIC, Formatting.GRAY)),
            DynamicListWidget.Scope(scope))
{
    private val description = CustomMultilineTextWidget(this.texts.desc ?: FcText.empty(), leftPadding = 10)

    init {
        description.width = this.w.get() - 120
    }

    override var h: Int
        get() = max(20, 10 + description.height)
        set(value) {}
    private val selectables: List<SelectableElement> = listOf(widget).cast()
    private val children = mutableListOf(widget)

    override fun selectableChildren(): List<SelectableElement> {
        return selectables
    }

    override fun children(): MutableList<out Element> {
        return children
    }

    override fun renderEntry(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, hovered: Boolean, focused: Boolean, delta: Float) {
        widget.setPosition(x + width - 110, y)
        widget.render(context, mouseX, mouseY, delta)
        description.setDimensionsAndPosition(width - 120, 0, x, y + 10)
        description.render(context, mouseX, mouseY, delta)
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, this.texts.name, x, y, if (hovered || focused) -171 else -1)
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