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
import me.fzzyhmstrs.fzzy_config.screen.widget.DynamicListWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomMultilineTextWidget
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.util.Translatable
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart

internal class ChangelogEntry(parentElement: DynamicListWidget, changes: String, private val index: Int) :
    DynamicListWidget.Entry(parentElement, Translatable.createResult(changes.lit()), DynamicListWidget.Scope(index.toString() + changes)) {

    private val widget = CustomMultilineTextWidget(texts.name)
    private val selectables: List<SelectableElement> = listOf(widget).cast()
    private val children = mutableListOf(widget)

    override var h: Int
        get() = widget.height
        set(value) {}

    override fun selectableChildren(): List<SelectableElement> {
        return selectables
    }

    override fun children(): MutableList<out Element> {
        return children
    }

    override fun init() {
        widget.setPosition(this.x.get(), this.top.get())
        widget.width = this.w.get()
    }

    override fun onResize() {
        widget.width = this.w.get()
    }

    override fun renderEntry(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, hovered: Boolean, focused: Boolean, delta: Float) {
        widget.setPosition(x, y)
        widget.render(context, mouseX, mouseY, delta)
    }

    override fun renderBorder(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, hovered: Boolean, focused: Boolean, delta: Float) {
        if (focused && MinecraftClient.getInstance().navigationType.isKeyboard) {
            context.drawBorder(x, y, width, height, -1)
        }
    }

    override fun renderHighlight(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, hovered: Boolean, focused: Boolean, delta: Float) {
        if (index % 2 == 1)
            context.fill(x, y - 1, x + width, y + height, 1684300900)
    }

    override fun appendTitleNarrations(builder: NarrationMessageBuilder) {
        builder.put(NarrationPart.TITLE, texts.name)
    }
}