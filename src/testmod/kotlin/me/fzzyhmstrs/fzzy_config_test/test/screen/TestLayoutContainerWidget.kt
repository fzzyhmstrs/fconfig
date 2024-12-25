/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config_test.test.screen

import me.fzzyhmstrs.fzzy_config.screen.widget.LayoutWidget
import me.fzzyhmstrs.fzzy_config.util.FcText
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.Selectable
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.widget.ContainerWidget
import net.minecraft.util.Colors
import java.util.function.UnaryOperator

class TestLayoutContainerWidget(layoutBuilder: UnaryOperator<LayoutWidget>) : ContainerWidget(0, 0, 20, 20, FcText.empty()) {

    private val layout: LayoutWidget = layoutBuilder.apply(LayoutWidget(paddingW = 1, spacingW = 0)).compute()
    private val children: MutableList<Element> = mutableListOf()
    private val drawables: MutableList<Drawable> = mutableListOf()
    private val selectables: MutableList<Selectable> = mutableListOf()

    init {
        layout.categorize(children, drawables, selectables)
        this.setWidth(layout.width)
        this.setHeight(layout.height)
    }

    override fun setX(x: Int) {
        layout.x = x
        super.setX(x)
    }

    override fun setY(y: Int) {
        layout.y = y
        super.setY(y)
    }

    override fun setPosition(x: Int, y: Int) {
        layout.setPosition(x, y)
        super.setPosition(x, y)
    }

    override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        for (d in drawables) {
            d.render(context, mouseX, mouseY, delta)
        }
        context.drawBorder(x, y, width, height, Colors.WHITE)
    }

    override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
        appendDefaultNarrations(builder)
    }

    override fun children(): MutableList<out Element> {
        return children
    }


}