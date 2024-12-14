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

import me.fzzyhmstrs.fzzy_config.screen.widget.LayoutWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.NewConfigListWidget
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.Selectable
import net.minecraft.text.Text

open class NewBaseConfigEntry protected constructor(private val layout: LayoutWidget, parentElement: NewConfigListWidget, h: Int, name: Text, desc: Text, scope: String, group: String = "") :
    NewConfigListWidget.Entry(parentElement, h, name, desc, scope, group)
{

    private var selectables: List<Selectable> = listOf()
    private var drawables: List<Drawable> = listOf()
    private var children: MutableList<out Element> = mutableListOf()

    override fun init() {
        layout.setPos(this.x, this.top)
    }

    override fun renderEntry(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, delta: Float) {
        for (drawable in drawables) {
            drawable.render(context, mouseX, mouseY, delta)
        }
    }

    override fun children(): MutableList<out Element> {
        return children
    }

    override fun selectableChildren(): List<Selectable> {
        return selectables
    }
}