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
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomPressableWidget
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.RenderUtil.drawOutline
import me.fzzyhmstrs.fzzy_config.util.Translatable
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.gui.narration.NarratedElementType
import net.minecraft.client.gui.components.Tooltip

internal class SidebarEntry(parentElement: DynamicListWidget, scope: String, texts: Translatable.Result, icon: Decorated.DecoratedOffset, onPress: Runnable, layer: Int) :
    DynamicListWidget.Entry(parentElement, texts, DynamicListWidget.Scope(scope)) {

    override var h: Int = 16
    private val widget = SidebarWidget(texts, icon, onPress, layer)
    private val selectables: List<SelectableElement> = listOf(widget).cast()
    private val children = mutableListOf(widget)

    override fun selectableChildren(): List<SelectableElement> {
        return selectables
    }

    override fun children(): MutableList<out GuiEventListener> {
        return children
    }

    override fun renderEntry(context: GuiGraphicsExtractor, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, hovered: Boolean, focused: Boolean, delta: Float) {
        if (hovered || focused) {
            widget.setPosition(x + 2, y)
        } else {
            widget.setPosition(x, y)
        }
        widget.extractRenderState(context, mouseX, mouseY, delta)
    }

    override fun renderBorder(context: GuiGraphicsExtractor, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, hovered: Boolean, focused: Boolean, delta: Float) {
        if (focused && Minecraft.getInstance().lastInputType.isKeyboard) {
            context.drawOutline(x, y, width, height, -1)
        }
    }

    override fun appendTitleNarrations(builder: NarrationElementOutput) {
        builder.add(NarratedElementType.TITLE, "fc.button.goto.narration".translate(texts.name))
    }

    companion object {
        fun neededWidth(texts: Translatable.Result, layer: Int): Int {
            return 16 + layer * 4 + 2 + Minecraft.getInstance().font.width(texts.name) + 2
        }
    }

    class SidebarWidget(
        private val texts: Translatable.Result,
        private val icon: Decorated.DecoratedOffset,
        private val onPress: Runnable,
        private val layer: Int)
        :
        CustomPressableWidget(
            0, 0,
            16 + layer * 4 + 2 + Minecraft.getInstance().font.width(texts.name) + 2, 16,
            FcText.EMPTY) {

        init {
            if (texts.desc != null) {
                setTooltip(Tooltip.create(texts.desc!!))
            }
        }

        override fun onPress() {
            PopupWidget.pop()
            onPress.run()
        }

        override fun renderBackground(context: GuiGraphicsExtractor, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, delta: Float) {
            val offset = if (isHoveredOrFocused) 2 else 0
            icon.decorated.renderDecoration(context, x + offset + icon.offsetX + layer * 4, y + icon.offsetY, delta, this.active, this.isHoveredOrFocused)
            context.text(Minecraft.getInstance().font, texts.name, x + offset + layer * 4 + 18, y + 3, -1)
        }

        override fun renderCustom(context: GuiGraphicsExtractor, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, delta: Float) {}
    }
}