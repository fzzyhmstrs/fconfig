/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.screen.widget.internal

import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.screen.widget.Scalable
import me.fzzyhmstrs.fzzy_config.util.RenderUtil.drawNineSlice
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.Widget
import java.util.function.Consumer

//client
internal class DividerWidget(width: Int): Widget, Drawable, Scalable {
    private var xx = 0
    private var yy = 0
    private var ww = width
    private var hh = 1

    override fun setX(x: Int) {
        this.xx = x
    }
    override fun getX(): Int {
        return xx
    }

    override fun setY(y: Int) {
        this.yy = y
    }
    override fun getY(): Int {
        return yy
    }

    override fun getWidth(): Int {
        return ww
    }
    override fun setWidth(width: Int) {
        ww = width
    }

    override fun getHeight(): Int {
        return hh
    }
    override fun setHeight(height: Int) {
        hh = height
    }

    override fun forEachChild(consumer: Consumer<ClickableWidget>) {
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        context.drawNineSlice(DIVIDER, xx, yy - 3, ww, hh + 6, 3, 3, 64, 7)
    }

    companion object {
        private val DIVIDER = "widget/popup/divider".fcId()
    }


}