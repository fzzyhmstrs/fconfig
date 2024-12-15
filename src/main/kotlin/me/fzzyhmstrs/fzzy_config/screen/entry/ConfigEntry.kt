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

import me.fzzyhmstrs.fzzy_config.annotations.Action
import me.fzzyhmstrs.fzzy_config.entry.EntryCreator
import me.fzzyhmstrs.fzzy_config.screen.decoration.Decorated
import me.fzzyhmstrs.fzzy_config.screen.decoration.DecorationWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.LayoutWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.NewConfigListWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.SuppliedTextWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.TooltipChild
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomMultilineTextWidget
import me.fzzyhmstrs.fzzy_config.util.RenderUtil.drawTex
import me.fzzyhmstrs.fzzy_config.util.Translatable
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.Selectable
import net.minecraft.client.gui.widget.AbstractTextWidget
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.Widget
import net.minecraft.text.Text
import java.util.function.Consumer
import java.util.function.UnaryOperator
import kotlin.math.min

class ConfigEntry(parentElement: NewConfigListWidget, content: ContentBuilder.BuildResult, texts: Translatable.Result) :
    NewConfigListWidget.Entry(parentElement, texts.name, texts.desc, content.scope)
{

    init {
        this.visibility = content.visibility
    }

    private val actions: List<ActionDecorationWidget> = content.actionWidgets
    private val layout: LayoutWidget = content.layoutWidget.setPos(this.x, this.top)
    private var children: MutableList<Element> = mutableListOf()
    private var drawables: List<Drawable> = listOf()
    private var selectables: List<Selectable> = listOf()
    private var narratables: List<AbstractTextWidget> = listOf()
    private var tooltipProviders: List<TooltipChild> = listOf()

    override fun init() {
        layout.setPos(this.x, this.top)
        val c: MutableList<Element> = mutableListOf()
        val d: MutableList<Drawable> = mutableListOf()
        val s: MutableList<Selectable> = mutableListOf()
        val n: MutableList<AbstractTextWidget> = mutableListOf()
        val t: MutableList<TooltipChild> = mutableListOf()
        layout.categorize(c, d, s) { w ->
            if (w is AbstractTextWidget)
                n.add(w)
            if (w is TooltipChild)
                t.add(w)
        }
        narratables = n.filter { widget -> widget.message != name }
        t.addAll(actions)
        tooltipProviders = t
    }

    override fun renderEntry(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, hovered: Boolean, focused: Boolean, delta: Float) {
        for (drawable in drawables) {
            drawable.render(context, mouseX, mouseY, delta)
        }
        if (actions.isNotEmpty()) {
            var offset = -24
            val offsetIncrement = if(actions.size == 1) 19 else min(19, (x - 24) / (actions.size - 1))
            for (action in actions) {
                action.x = x + offset
                action.render(context, mouseX, mouseY, delta)
                offset -= offsetIncrement
            }
        }
        val keyboardFocused = focused && MinecraftClient.getInstance().navigationType.isKeyboard
        val parentSelected = hovered || focused
    }

    override fun renderBorder(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, hovered: Boolean, focused: Boolean, delta: Float) {
        TODO()
    }

    override fun children(): MutableList<out Element> {
        return children
    }

    override var h: Int
        get() = layout.height
        set(value) {
            layout.height = value
        }

    override fun selectableChildren(): List<Selectable> {
        return selectables
    }

    class ContentBuilder(private val context: EntryCreator.CreatorContext, private val actionWidgets: List<ActionDecorationWidget>) {
        private var mainLayout: LayoutWidget = LayoutWidget(paddingW = 0, spacingW = 0)
        private var contentLayout: LayoutWidget = LayoutWidget(paddingW = 0).clampWidth(110)
        private val decorationWidget = DecorationWidget()
        private var group: String = ""
        private var visibility: NewConfigListWidget.Visibility = NewConfigListWidget.Visibility.VISIBLE

        init {
            val nameSupplier = { context.texts.name }
            val titleWidget = SuppliedTextWidget(nameSupplier, MinecraftClient.getInstance().textRenderer, 70, 20).supplyTooltipOnOverflow(nameSupplier).alignLeft()
            val prefixWidget = context.texts.prefix?.let { CustomMultilineTextWidget(it, 10) }
            if (prefixWidget != null) {
                mainLayout.add("prefix", prefixWidget, LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_JUSTIFY)
                mainLayout.add("title", titleWidget, "prefix", LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_LEFT_AND_JUSTIFY)
            } else {
                mainLayout.add("title", titleWidget, LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_LEFT_AND_JUSTIFY)
            }
            mainLayout.add("content", contentLayout, "title", LayoutWidget.Position.ALIGN_RIGHT, LayoutWidget.Position.HORIZONTAL_TO_TOP_EDGE)
            mainLayout.add("deco", decorationWidget, "content", LayoutWidget.Position.LEFT, LayoutWidget.Position.HORIZONTAL_TO_TOP_EDGE)
        }

        fun layoutMain(layoutOperations: UnaryOperator<LayoutWidget>): ContentBuilder {
            mainLayout = layoutOperations.apply(mainLayout)
            return this
        }

        fun layoutContent(layoutOperations: UnaryOperator<LayoutWidget>): ContentBuilder {
            contentLayout = layoutOperations.apply(contentLayout)
            return this
        }

        fun decoration(decoration: Decorated): ContentBuilder {
            this.decorationWidget.setDeco(decoration)
            return this
        }

        fun group(group: String): ContentBuilder {
            this.group = group
            return this
        }

        fun visibility(visibility: NewConfigListWidget.Visibility): ContentBuilder {
            this.visibility = visibility
            return this
        }


        fun build(): BuildResult {
            return BuildResult(
                mainLayout.compute(),
                actionWidgets,
                NewConfigListWidget.Scope(context.scope, group, context.groups.stream().toList()),
                visibility)
        }

        class BuildResult(
            val layoutWidget: LayoutWidget,
            val actionWidgets: List<ActionDecorationWidget>,
            val scope: NewConfigListWidget.Scope,
            val visibility: NewConfigListWidget.Visibility)

    }

    class ActionDecorationWidget private constructor(private val action: Action, private val actionTooltip: Text = action.settingTooltip): Widget, Drawable, TooltipChild {

        companion object {
            fun setting(action: Action): ActionDecorationWidget {
                return ActionDecorationWidget(action)
            }

            fun section(action: Action): ActionDecorationWidget {
                return ActionDecorationWidget(action, action.sectionTooltip)
            }

            fun config(action: Action): ActionDecorationWidget {
                return ActionDecorationWidget(action, action.configTooltip)
            }
        }

        private var x: Int = 0
        private var y: Int = 0

        override fun setX(x: Int) { this.x = x }

        override fun setY(y: Int) { this.y = y }

        override fun getX(): Int { return x }

        override fun getY(): Int { return y }

        override fun getWidth(): Int { return 20 }

        override fun getHeight(): Int { return 20 }

        fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
            return ((mouseX >= x.toDouble()) && (mouseY >= x.toDouble()) && (mouseX < (x + this.width).toDouble()) && (mouseY < (y + this.height).toDouble()))
        }

        override fun forEachChild(consumer: Consumer<ClickableWidget>?) {}

        override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
            context.drawTex(action.sprite, x, y, 20, 20)
        }

        override fun provideTooltipLines(mouseX: Double, mouseY: Double, parentSelected: Boolean, keyboardFocused: Boolean): List<Text> {
            if (!isMouseOver(mouseX, mouseY) && !keyboardFocused) return TooltipChild.EMPTY
            return listOf(actionTooltip)
        }
    }
}