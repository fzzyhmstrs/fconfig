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
import me.fzzyhmstrs.fzzy_config.cast
import me.fzzyhmstrs.fzzy_config.config.ConfigGroup
import me.fzzyhmstrs.fzzy_config.entry.EntryCreator
import me.fzzyhmstrs.fzzy_config.nullCast
import me.fzzyhmstrs.fzzy_config.screen.context.*
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
import java.util.function.UnaryOperator
import kotlin.math.min

class ConfigEntry(parentElement: DynamicListWidget, content: ContentBuilder.BuildResult, texts: Translatable.Result) :
    DynamicListWidget.Entry(parentElement, texts.name, texts.desc, content.scope)
{

    init {
        this.visibility = content.visibility
    }

    private val layout: LayoutWidget = if (content.groupTypes.isEmpty()) {
        content.layoutWidget.setPos(this.x, this.top).compute()
    } else {
        val lo = LayoutWidget(paddingW = 0, spacingW = 0)
        for ((index, bl) in content.groupTypes.withIndex()) {
            lo.add("$index", GroupLineWidget(bl), LayoutWidget.Position.RIGHT, LayoutWidget.Position.ALIGN_LEFT, LayoutWidget.Position.HORIZONTAL_TO_TOP_EDGE)
        }
        lo.add("layout", content.layoutWidget, LayoutWidget.Position.RIGHT, LayoutWidget.Position.ALIGN_LEFT_AND_JUSTIFY, LayoutWidget.Position.HORIZONTAL_TO_TOP_EDGE)
        lo.setPos(this.x, this.top)
        lo.compute()
    }

    private val actions: List<AbstractDecorationWidget> = content.actionWidgets
    private val context: Map<ContextHandler.ContextType, ContextAction> = content.contextActions
    private var children: MutableList<Element> = mutableListOf()
    private var drawables: List<Drawable> = emptyList()
    private var selectables: List<SelectableElement> = emptyList()
    private var narratables: List<AbstractTextWidget> = emptyList()
    private var tooltipProviders: List<TooltipChild> = emptyList()

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
        selectables = s.filterNot { it is AbstractTextWidget }.filterIsInstance<Element>().cast()
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
        val keyboardFocused = focused && MinecraftClient.getInstance().navigationType.isKeyboard

        val tooltipList: MutableList<OrderedText> = mutableListOf()
        for ((index, provider) in tooltipProviders.withIndex()) {
            val tt = provider.provideTooltipLines(mouseX, mouseY, hovered || keyboardFocused, keyboardFocused)
            for (t in tt) {
                val ttt = createTooltip(t)
                if (ttt.isNotEmpty())
                    tooltipList.addAll(ttt)
            }
            if (tt.isNotEmpty() && index != tooltipProviders.lastIndex) {
                tooltipList.add(OrderedText.EMPTY)
            }
        }

        if ((hovered && !MinecraftClient.getInstance().navigationType.isKeyboard) || (focused && MinecraftClient.getInstance().navigationType.isKeyboard)) {
            tooltipList.addAll(tooltip)
        }
        if (tooltipList.isNotEmpty()) {
            if (tooltipList.last() == OrderedText.EMPTY) {
                tooltipList.removeLast()
            }
            if (keyboardFocused) {
                MinecraftClient.getInstance().currentScreen?.setTooltip(tooltipList, FocusedTooltipPositioner(ScreenRect(x + 2, y + 4, width, height)), true)
            } else {
                MinecraftClient.getInstance().currentScreen?.setTooltip(tooltipList, HoveredTooltipPositioner.INSTANCE, true)
            }
        }

    }

    override fun renderExtras(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, hovered: Boolean, focused: Boolean, delta: Float
    ) {
        if (actions.isNotEmpty()) {
            var offset = -24
            val offsetIncrement = if(actions.size == 1) 19 else min(19, (x - 24) / (actions.size - 1))
            for (action in actions) {
                action.x = x + offset
                action.y = (layout.getElement("title")?.getTop() ?: y)
                action.render(context, mouseX, mouseY, delta)
                offset -= offsetIncrement
            }
        }
    }

    override fun renderBorder(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, hovered: Boolean, focused: Boolean, delta: Float) {
        if ((hovered && !MinecraftClient.getInstance().navigationType.isKeyboard) || (focused && MinecraftClient.getInstance().navigationType.isKeyboard))
            context.drawBorder(x - 2, y - 2, width + 4, height + 4, -1)
        else if (focused || hovered)
            context.drawBorder(x - 2, y - 2, width + 4, height + 4, -6250336)
    }

    override fun renderHighlight(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, hovered: Boolean, focused: Boolean, delta: Float) {
        if (hovered)
            context.fill(x - 1, y - 1, x + width + 1, y + height + 1, 1684300900)
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

    override fun selectableChildren(): List<SelectableElement> {
        return selectables
    }

    override fun contextActions(position: Position): List<ContextApplier> {
        val content = layout.getElement("content")
        val newPosition = if (content != null && position.contextInput == ContextInput.KEYBOARD) {
            position.copy(x = content.getLeft(), y = content.getTop(), width = content.elWidth(), height = content.elHeight() )
        } else if (content != null) {
            position.copy(width = content.elWidth(), height = content.elHeight())
        } else {
            position
        }
        return context.values.filter { it.forMenu }.map { ContextApplier(it, newPosition) }
    }

    override fun handleContext(contextType: ContextHandler.ContextType, position: Position): Boolean {
        val action = context[contextType] ?: return false
        val content = layout.getElement("content")
        val newPosition = if (content != null && position.contextInput == ContextInput.KEYBOARD) {
            position.copy(x = content.getLeft(), y = content.getTop(), width = content.elWidth(), height = content.elHeight() )
        } else if (content != null) {
            position.copy(width = content.elWidth(), height = content.elHeight())
        } else {
            position
        }
        action.action.accept(newPosition)
        return true
    }

    //////////////////////////////////////

    class ContentBuilder(private val context: EntryCreator.CreatorContext, private val actionWidgets: List<AbstractDecorationWidget>) {

        constructor(context: EntryCreator.CreatorContext, actions: Set<Action>): this(context, actions.map { ActionDecorationWidget.setting(it) })

        constructor(context: EntryCreator.CreatorContext): this(context, context.actions.map { ActionDecorationWidget.setting(it) })

        private var mainLayout: LayoutWidget = LayoutWidget(paddingW = 0, spacingW = 0)
        private var contentLayout: LayoutWidget = LayoutWidget(paddingW = 0).clampWidth(110)
        private val decorationWidget = DecorationWidget()
        private var group: String = ""
        private var visibility: DynamicListWidget.Visibility = DynamicListWidget.Visibility.VISIBLE
        private var contextActions: Map<ContextHandler.ContextType, ContextAction> = mapOf()
        private val popStart = context.groups.size - context.annotations.filterIsInstance<ConfigGroup.Pop>().size


        init {
            val nameSupplier = { context.texts.name }
            val titleWidget = SuppliedTextWidget(nameSupplier, MinecraftClient.getInstance().textRenderer, 70, 20).supplyTooltipOnOverflow(nameSupplier).alignLeft()
            val prefixWidget = context.texts.prefix?.let { CustomMultilineTextWidget(it, 10, 10, 4) }
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

        fun contextActions(contextActions: Map<ContextHandler.ContextType, ContextAction>): ContentBuilder {
            this.contextActions = contextActions
            return this
        }

        fun build(): BuildResult {
            val groupTypes: MutableList<Boolean> = mutableListOf()
            for (i in 0 until context.groups.size) {
                groupTypes.add(i >= popStart)
            }
            return BuildResult(
                mainLayout,
                actionWidgets,
                DynamicListWidget.Scope(context.scope, group, context.groups.stream().toList()),
                groupTypes,
                visibility,
                contextActions)
        }

        class BuildResult(
            val layoutWidget: LayoutWidget,
            val actionWidgets: List<AbstractDecorationWidget>,
            val scope: DynamicListWidget.Scope,
            val groupTypes: List<Boolean>,
            val visibility: DynamicListWidget.Visibility,
            val contextActions: Map<ContextHandler.ContextType, ContextAction>)

    }

    ////////////////////////////////

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
            return ((mouseX >= x.toDouble()) && (mouseY >= y.toDouble()) && (mouseX < (x + this.width).toDouble()) && (mouseY < (y + this.height).toDouble()))
        }

        override fun getWidth(): Int {
            return 19
        }

        override fun forEachChild(consumer: Consumer<ClickableWidget>?) {}

        override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
            context.drawTex(action.sprite, x, y, 20, 20)
        }

        override fun provideTooltipLines(mouseX: Int, mouseY: Int, parentSelected: Boolean, keyboardFocused: Boolean): List<Text> {
            val bl1 = isMouseOver(mouseX.toDouble(), mouseY.toDouble())
            if (! bl1 && !keyboardFocused) return TooltipChild.EMPTY
            return listOf(actionTooltip)
        }
    }

    ////////////////////////////////

    private inner class GroupLineWidget(private val end: Boolean): Drawable, Widget {

        private var x: Int = 0
        private var y: Int = 0

        override fun setX(x: Int) { this.x = x }

        override fun setY(y: Int) { this.y = y }

        override fun getX(): Int { return x }

        override fun getY(): Int { return y }

        override fun getWidth(): Int { return 5 }

        override fun getHeight(): Int { return this@ConfigEntry.h }

        override fun forEachChild(consumer: Consumer<ClickableWidget>?) {
        }

        override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
            val p = this@ConfigEntry.parentElement.verticalPadding
            if (end) {
                context.fill(x, y - p, x + 1, y + height, -1)
            } else {
                context.fill(x, y - p, x + 1, y + height, -1)
                context.fill(x + 1, y - p, x + 2, y + height, -12698050)
            }
        }
    }
}