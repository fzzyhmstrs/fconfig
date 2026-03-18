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
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.gui.narration.NarratedElementType
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.layouts.LayoutElement
import net.minecraft.util.CommonColors
import net.minecraft.ChatFormatting
import kotlin.math.max

internal class SearchMenuEntry(parentElement: DynamicListWidget, scope: String, private val widget: AbstractWidget):
        DynamicListWidget.Entry(
            parentElement,
            Translatable.createScopedResult("fc.search.id.$scope", "fc.search.$scope".translate(), "fc.search.$scope.desc".translate().withStyle(
                ChatFormatting.ITALIC, ChatFormatting.GRAY)),
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

    override fun children(): MutableList<out GuiEventListener> {
        return children
    }

    override fun renderEntry(context: GuiGraphicsExtractor, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, hovered: Boolean, focused: Boolean, delta: Float) {
        widget.setPosition(x + width - 110, y)
        widget.extractRenderState(context, mouseX, mouseY, delta)
        description.setRectangle(width - 120, 0, x, y + 10)
        description.extractRenderState(context, mouseX, mouseY, delta)
        context.text(Minecraft.getInstance().font, this.texts.name, x, y, if (hovered || focused) -171 else -1)
    }

    override fun renderExtras(context: GuiGraphicsExtractor, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, hovered: Boolean, focused: Boolean, delta: Float) {
        if (hovered || focused)
            context.text(Minecraft.getInstance().font, ">", x - 8, y, -171)
    }

    override fun appendNarrations(builder: NarrationElementOutput) {
        super.appendNarrations(builder)
        this.texts.desc?.let { builder.add(NarratedElementType.HINT, it.string) }
    }
}