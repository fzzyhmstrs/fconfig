/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.screen.decoration

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.Widget
import java.util.function.Consumer

class DecorationWidget(private var decoration: Decorated? = null, private var offsetX: Int = 0, private var offsetY: Int = 0): Widget, Drawable {

    private var x: Int = 0
    private var y: Int = 0

    override fun setX(x: Int) {
        this.x = x
    }

    override fun setY(y: Int) {
        this.y = y
    }

    override fun getX(): Int {
        return x
    }

    override fun getY(): Int {
        return y
    }

    override fun getWidth(): Int {
        return if (decoration == null) 0 else 20
    }

    override fun getHeight(): Int {
        return if (decoration == null) 0 else 20
    }

    override fun forEachChild(consumer: Consumer<ClickableWidget>?) {
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        decoration?.renderDecoration(context, x + offsetX, y + offsetY, delta)
    }

    fun setDeco(newDecoration: Decorated, newOffsetX: Int = offsetX, newOffsetY: Int = offsetY) {
        this.decoration = newDecoration
        this.offsetX = newOffsetX
        this.offsetY = newOffsetY
    }
}