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

import jdk.javadoc.internal.doclets.formats.html.markup.ContentBuilder
import me.fzzyhmstrs.fzzy_config.screen.widget.LayoutWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.NewConfigListWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.internal.DecorationWidget
import me.fzzyhmstrs.fzzy_config.util.Translatable
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.Selectable
import net.minecraft.client.gui.widget.MultilineTextWidget
import net.minecraft.client.gui.widget.TextWidget
import net.minecraft.text.Text
import java.util.function.UnaryOperator

class NewBaseConfigEntry(builder: ContentBuilder, parentElement: NewConfigListWidget, h: Int, name: Text, desc: Text, scope: String, group: String = "") :
    NewConfigListWidget.Entry(parentElement, h, name, desc, scope, group)
{

    /*
    Validated Field will be EntryCreator
     */

    private val layout: LayoutWidget = builder.build()
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


    class ContentBuilder(translationResult: Translatable.Result) {
        private var mainLayout: LayoutWidget = LayoutWidget(paddingW = 0, spacingW = 0)
        private var contentLayout: LayoutWidget = LayoutWidget(paddingW = 0).clampWidth(110)
        private val decoration = DecorationWidget()

        init {
            val titleWidget = TextWidget(70, 20, translationResult.name, MinecraftClient.getInstance().textRenderer)
            val prefixWidget = translationResult.prefix?.let { /*MultilineTextWidget(70, 20, it, MinecraftClient.getInstance().textRenderer)*/ }
        }

        fun layoutMain(layoutOperations: UnaryOperator<LayoutWidget>): ContentBuilder {
            mainLayout = layoutOperations.apply(mainLayout)
            return this
        }

        fun layoutContent(layoutOperations: UnaryOperator<LayoutWidget>): ContentBuilder {
            contentLayout = layoutOperations.apply(contentLayout)
            return this
        }


        internal fun build(): LayoutWidget {
            TODO()
        }

    }
}