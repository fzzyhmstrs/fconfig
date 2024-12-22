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
import me.fzzyhmstrs.fzzy_config.config.ConfigGroup
import me.fzzyhmstrs.fzzy_config.entry.EntryCreator
import me.fzzyhmstrs.fzzy_config.nullCast
import me.fzzyhmstrs.fzzy_config.screen.decoration.AbstractDecorationWidget
import me.fzzyhmstrs.fzzy_config.screen.decoration.Decorated
import me.fzzyhmstrs.fzzy_config.screen.decoration.DecorationWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.DynamicListWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.LayoutWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.SuppliedTextWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.TooltipChild
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomMultilineTextWidget
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.isEmpty
import me.fzzyhmstrs.fzzy_config.util.RenderUtil.drawTex
import me.fzzyhmstrs.fzzy_config.util.Translatable
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.*
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import net.minecraft.client.gui.tooltip.FocusedTooltipPositioner
import net.minecraft.client.gui.tooltip.HoveredTooltipPositioner
import net.minecraft.client.gui.widget.AbstractTextWidget
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.Widget
import net.minecraft.text.OrderedText
import net.minecraft.text.Text
import java.util.*
import java.util.function.Consumer
import java.util.function.Supplier
import java.util.function.UnaryOperator
import kotlin.math.min

class ConfigEntry(parentElement: DynamicListWidget, content: ContentBuilder.BuildResult, texts: Translatable.Result) :
    DynamicListWidget.Entry(parentElement, texts.name, texts.desc, content.scope)
{

    init {
        this.visibility = content.visibility
    }

    private val actions: List<AbstractDecorationWidget> = content.actionWidgets
    private val layout: LayoutWidget = if (content.groupTypes.isEmpty()) {
        content.layoutWidget.setPos(this.x, this.top)
    } else {
        val lo = LayoutWidget(paddingW = 0, spacingW = 0)
        for ((index, bl) in content.groupTypes.withIndex()) {
            lo.add("$index", GroupLineWidget(bl), LayoutWidget.Position.RIGHT, LayoutWidget.Position.ALIGN_LEFT, LayoutWidget.Position.HORIZONTAL_TO_TOP_EDGE)
        }
        lo.add("layout", content.layoutWidget, LayoutWidget.Position.RIGHT, LayoutWidget.Position.ALIGN_LEFT_AND_JUSTIFY, LayoutWidget.Position.HORIZONTAL_TO_TOP_EDGE)
        lo.setPos(this.x, this.top)
    }
    private var children: MutableList<Element> = mutableListOf()
    private var drawables: List<Drawable> = listOf()
    private var selectables: List<Selectable> = listOf()
    private var narratables: List<AbstractTextWidget> = listOf()
    private var tooltipProviders: List<TooltipChild> = listOf()
    private val tooltip: List<OrderedText> by lazy {
        createTooltip(desc ?: FcText.empty())
    }
    private val narrationPrefix by lazy {
        val b = StringBuilder()
        for (provider in tooltipProviders) {
            val ts = provider.provideNarrationLines()
            for (t in ts) {
                t.visit { s -> b.append(s); Optional.empty<Unit>() }
            }
            b.append(". ")
        }
        b.toString()
    }

    override fun init() {
        layout.setPos(this.x, this.top)
        layout.setWidth(this.w.get())
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
        c.addAll(actions.mapNotNull { it.nullCast() })
        t.addAll(actions.mapNotNull { it.nullCast() })
        children = c
        drawables = d
        selectables = s
        narratables = n
        tooltipProviders = t
    }

    override fun onResize() {
        layout.setWidth(this.w.get())
        layout.update()
    }

    override fun onScroll(dY: Int) {
        layout.update()
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
                action.y = y
                action.render(context, mouseX, mouseY, delta)
                offset -= offsetIncrement
            }
        }
        if (!(hovered || focused)) return
        val keyboardFocused = focused && MinecraftClient.getInstance().navigationType.isKeyboard

        val tooltipList: MutableList<OrderedText> = mutableListOf()
        for (provider in tooltipProviders) {
            for (t in provider.provideTooltipLines(mouseX, mouseY, true, keyboardFocused)) {
                tooltipList.addAll(createTooltip(t))
            }
            tooltipList.add(OrderedText.EMPTY)
        }
        tooltipList.addAll(tooltip)
        if (keyboardFocused) {
            MinecraftClient.getInstance().currentScreen?.setTooltip(tooltipList, FocusedTooltipPositioner(ScreenRect(x, y, width, height)), true)
        } else {
            MinecraftClient.getInstance().currentScreen?.setTooltip(tooltipList, HoveredTooltipPositioner.INSTANCE, true)
        }
    }

    override fun renderBorder(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, hovered: Boolean, focused: Boolean, delta: Float) {
        if (hovered)
            context.drawBorder(x - 2, y - 2, width + 4, height + 4, -1)
        else if (focused)
            context.drawBorder(x - 2, y - 2, width + 4, height + 4, -6250336)
    }

    override fun appendNarrations(builder: NarrationMessageBuilder) {
        super.appendNarrations(builder)
        builder.put(NarrationPart.TITLE, name)
        if (MinecraftClient.getInstance().navigationType.isKeyboard) {
            builder.put(NarrationPart.HINT, narrationPrefix + createTooltipString(tooltip))
        } else {
            builder.put(NarrationPart.HINT, createTooltipString(tooltip))
        }
    }

    private fun createTooltip(input: Text): List<OrderedText> {
        val list: MutableList<OrderedText> = mutableListOf()
        if (input.isEmpty()) return list
        list.addAll(MinecraftClient.getInstance().textRenderer.wrapLines(input, 190))
        return list
    }


    private fun createTooltipString(tt: List<OrderedText>): String {
        val builder = StringBuilder()
        for (tip in tt) {
            tip.accept { _, _, codepoint ->
                builder.appendCodePoint(codepoint)
                true
            }
            builder.append(". ")
        }
        return builder.toString()
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

    class ContentBuilder(private val context: EntryCreator.CreatorContext, private val actionWidgets: List<AbstractDecorationWidget>) {

        constructor(context: EntryCreator.CreatorContext, actions: Set<Action>): this(context, actions.map { ActionDecorationWidget.setting(it) })

        constructor(context: EntryCreator.CreatorContext): this(context, context.actions.map { ActionDecorationWidget.setting(it) })

        private var mainLayout: LayoutWidget = LayoutWidget(paddingW = 0, spacingW = 0)
        private var contentLayout: LayoutWidget = LayoutWidget(paddingW = 0).clampWidth(110)
        private val decorationWidget = DecorationWidget()
        private var group: String = ""
        private var visibility: DynamicListWidget.Visibility = DynamicListWidget.Visibility.VISIBLE
        private val popStart = context.groups.size - context.annotations.filterIsInstance<ConfigGroup.Pop>().size

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

        fun decoration(decoration: Decorated, offsetX: Int = 0, offsetY: Int = 0): ContentBuilder {
            this.decorationWidget.setDeco(decoration, offsetX, offsetY)
            return this
        }

        fun group(group: String): ContentBuilder {
            this.group = group
            return this
        }

        fun visibility(visibility: DynamicListWidget.Visibility): ContentBuilder {
            this.visibility = visibility
            return this
        }

        fun build(): BuildResult {
            val groupTypes: MutableList<Boolean> = mutableListOf()
            for (i in 0 until context.groups.size) {
                groupTypes.add(i >= popStart)
            }
            return BuildResult(
                mainLayout.compute(),
                actionWidgets,
                DynamicListWidget.Scope(context.scope, group, context.groups.stream().toList()),
                groupTypes,
                visibility)
        }

        class BuildResult(
            val layoutWidget: LayoutWidget,
            val actionWidgets: List<AbstractDecorationWidget>,
            val scope: DynamicListWidget.Scope,
            val groupTypes: List<Boolean>,
            val visibility: DynamicListWidget.Visibility)

    }

    class ActionDecorationWidget private constructor(private val action: Action, private val actionTooltip: Text = action.settingTooltip): AbstractDecorationWidget(), TooltipChild {

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

        fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
            return ((mouseX >= x.toDouble()) && (mouseY >= x.toDouble()) && (mouseX < (x + this.width).toDouble()) && (mouseY < (y + this.height).toDouble()))
        }

        override fun forEachChild(consumer: Consumer<ClickableWidget>?) {}

        override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
            context.drawTex(action.sprite, x, y, 20, 20)
        }

        override fun provideTooltipLines(mouseX: Int, mouseY: Int, parentSelected: Boolean, keyboardFocused: Boolean): List<Text> {
            if (!isMouseOver(mouseX.toDouble(), mouseY.toDouble()) && !keyboardFocused) return TooltipChild.EMPTY
            return listOf(actionTooltip)
        }
    }

    private class GroupLineWidget(private val end: Boolean): Drawable, Widget {

        private var x: Int = 0
        private var y: Int = 0
        private var h: Supplier<Int>? = null

        fun setH(h: Supplier<Int>) {
            this.h = h
        }

        override fun setX(x: Int) { this.x = x }

        override fun setY(y: Int) { this.y = y }

        override fun getX(): Int { return x }

        override fun getY(): Int { return y }

        override fun getWidth(): Int { return 5 }

        override fun getHeight(): Int { return h?.get() ?: 1 }

        override fun forEachChild(consumer: Consumer<ClickableWidget>?) {
        }


        override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
            val p = DynamicListWidget.verticalPadding.get()
            context.fill(x, y - p, x + 1, y + height, -1)
            context.fill(x + 1, y - p, x + 2, y + height, -12698050)
        }
    }
}