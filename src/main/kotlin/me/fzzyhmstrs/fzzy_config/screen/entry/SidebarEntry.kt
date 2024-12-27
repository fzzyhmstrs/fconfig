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
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomPressableWidget
import me.fzzyhmstrs.fzzy_config.util.Translatable
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Element
import net.minecraft.text.Text

class SidebarEntry(parentElement: DynamicListWidget, scope: String, texts: Translatable.Result, private val widget: SidebarWidget) :
    DynamicListWidget.Entry(parentElement, texts.name, texts.desc, DynamicListWidget.Scope(scope)) {

    override var h: Int = 16
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
        if (hovered)
            context.drawBorder(x, y, width, height, -1)
        else if (focused)
            context.drawBorder(x, y, width, height, -6250336)
    }

    override fun renderHighlight(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, hovered: Boolean, focused: Boolean, delta: Float) {
        context.fill(x, y, x + width, y + height, -16777216)
    }


    class SidebarWidget(message: Text, private val icon: Decorated, private val onPress: Runnable) : CustomPressableWidget(0, 0, 110, 16, message) {

        override fun onPress() {
            onPress.run()
        }

        override fun renderBackground(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, delta: Float) {
            icon.renderDecoration(context, x, y, delta, false, false)
        }

        override fun renderCustom(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, delta: Float) {
            super.renderCustom(context, x + 18, y, width - 18, height, mouseX, mouseY, delta)
        }
    }
}