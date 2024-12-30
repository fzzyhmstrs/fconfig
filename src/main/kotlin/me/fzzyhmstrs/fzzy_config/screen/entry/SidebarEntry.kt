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
import me.fzzyhmstrs.fzzy_config.util.Translatable
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.text.Text

internal class SidebarEntry(parentElement: DynamicListWidget, scope: String, texts: Translatable.Result, icon: Decorated, onPress: Runnable, layer: Int) :
    DynamicListWidget.Entry(parentElement, texts.name, texts.desc, DynamicListWidget.Scope(scope)) {

    override var h: Int = 16
    private val widget = SidebarWidget(texts, icon, onPress, layer)
    private val selectables: List<SelectableElement> = listOf(widget).cast()
    private val children = mutableListOf(widget)

    override fun selectableChildren(): List<SelectableElement> {
        return selectables
    }

    override fun children(): MutableList<out Element> {
        return children
    }

    override fun renderEntry(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, hovered: Boolean, focused: Boolean, delta: Float) {
        if (hovered || focused) {
            widget.setPosition(x + 2, y)
        } else {
            widget.setPosition(x, y)
        }
        widget.render(context, mouseX, mouseY, delta)
    }

    override fun renderBorder(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, hovered: Boolean, focused: Boolean, delta: Float) {
        if (focused && MinecraftClient.getInstance().navigationType.isKeyboard) {
            context.drawBorder(x, y, width, height, -1)
        }
    }

    companion object {
        fun neededWidth(texts: Translatable.Result, layer: Int): Int {
            return 16 + layer * 4 + 2 + MinecraftClient.getInstance().textRenderer.getWidth(texts.name) + 2
        }
    }

    class SidebarWidget(
        private val texts: Translatable.Result,
        private val icon: Decorated,
        private val onPress: Runnable,
        private val layer: Int)
        :
        CustomPressableWidget(
            0, 0,
            16 + layer * 4 + 2 + MinecraftClient.getInstance().textRenderer.getWidth(texts.name) + 2, 16,
            FcText.EMPTY) {

        init {
            if (texts.desc != null) {
                this.tooltip = Tooltip.of(texts.desc)
            }
        }

        override fun onPress() {
            PopupWidget.pop()
            onPress.run()
        }

        override fun renderBackground(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, delta: Float) {
            val offset = if (isSelected) 2 else 0
            icon.renderDecoration(context, x + offset + layer * 4, y, delta, this.active, this.isSelected)
            context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, texts.name, x + offset + layer * 4 + 18, y + 3, -1)
        }

        override fun renderCustom(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, delta: Float) {}
    }
}