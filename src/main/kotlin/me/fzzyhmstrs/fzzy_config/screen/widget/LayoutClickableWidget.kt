/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.screen.widget

import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.pos.ReferencePos
import net.minecraft.client.gui.*
import net.minecraft.client.gui.navigation.FocusNavigationEvent
import net.minecraft.client.gui.ComponentPath
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.gui.narration.NarratedElementType
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.ContainerEventHandler
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import java.util.function.Consumer

/**
 * Clickable widget that contains a LayoutWidget which provides and lays out children of this widget. The layout will be automatically constrained to the dimensions of the widget, and updated as the position and size changes.
 * @param x X position of the widget in pixels
 * @param y Y position of the widget in pixels
 * @param width width of the widget in pixels
 * @param height height of the widget in pixels
 * @param layout [LayoutWidget] the layout wrapped by this widget
 * @author fzzyhmstrs
 * @since 0.6.0
 */
class LayoutClickableWidget(x: Int, y: Int, width: Int, height: Int, private val layout: LayoutWidget)
    : AbstractWidget(x, y, width, height, FcText.empty()), ContainerEventHandler, TooltipChild {

    private var children: MutableList<GuiEventListener> = mutableListOf()
    private var drawables: List<Renderable> = emptyList()
    private var selectables: List<NarratableEntry> = emptyList()
    private var tooltipProviders: List<TooltipChild> = emptyList()
    private var focusedSelectable: NarratableEntry? = null
    private var focusedElement: GuiEventListener? = null
    private var dragging: Boolean = false
    private var narrationAppender: Consumer<NarrationElementOutput> = Consumer { _-> }

    /**
     * Applies custom narration to this layout widget. This consumer will be applied on every call to narration building, using the same message level as the default narration of the widget. In particular, element POSITION and USAGE information is typically applied already. The current hovered and/or focused child widget uses the next message layer. If you have to dodge one of both of these existing additions, you will have to add one or two more layers to the builder before using it.
     * @param narrationAppender Consumer&lt;[NarrationMessageBuilder]&gt; appends arbitrary narration information to the provided builder.
     * @return this widget
     * @author fzzyhmstrs
     * @since 0.6.5
     */
    fun withNarrationAppender(narrationAppender: Consumer<NarrationElementOutput>): LayoutClickableWidget {
        this.narrationAppender = narrationAppender
        return this
    }

    init {
        layout.setPos(ReferencePos { this.x }, ReferencePos { this.y })
        layout.setDimensions(width, height)
        val c: MutableList<GuiEventListener> = mutableListOf()
        val d: MutableList<Renderable> = mutableListOf()
        val s: MutableList<NarratableEntry> = mutableListOf()
        val t: MutableList<TooltipChild> = mutableListOf()
        layout.categorize(c, d, s) { w ->
            if (w is TooltipChild)
                t.add(w)
        }
        children = c
        drawables = d
        selectables = s
        tooltipProviders = t
    }

    override fun setX(x: Int) {
        super.setX(x)
        layout.update()
    }

    override fun setY(y: Int) {
        super.setY(y)
        layout.update()
    }

    override fun setPosition(x: Int, y: Int) {
        super.setX(x)
        super.setY(y)
        layout.update()
    }

    override fun setWidth(width: Int) {
        super.setWidth(width)
        layout.width = width
    }

    override fun setHeight(height: Int) {
        super.setHeight(height)
        layout.height = height
    }

    override fun setSize(width: Int, height: Int) {
        super.setWidth(width)
        super.setHeight(height)
        layout.setDimensions(width, height)
    }

    override fun setRectangle(width: Int, height: Int, x: Int, y: Int) {
        super.setWidth(width)
        super.setHeight(height)
        super.setX(x)
        super.setY(y)
        layout.setDimensions(width, height)
        layout.update()
    }

    override fun children(): MutableList<out GuiEventListener> {
        return children
    }

    override fun isDragging(): Boolean {
        return dragging
    }

    override fun setDragging(dragging: Boolean) {
        this.dragging = dragging
    }


    override fun extractWidgetRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        for (d in drawables) {
            d.extractRenderState(context, mouseX, mouseY, delta)
        }
        //renderCustom(context, mouseX, mouseY, delta)
    }

    override fun onClick(click: MouseButtonEvent, doubled: Boolean) {
    }

    override fun mouseClicked(click: MouseButtonEvent, doubled: Boolean): Boolean {
        return super<ContainerEventHandler>.mouseClicked(click, doubled)
    }

    override fun mouseReleased(click: MouseButtonEvent): Boolean {
        return super<ContainerEventHandler>.mouseReleased(click)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        return super<ContainerEventHandler>.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun mouseDragged(click: MouseButtonEvent, offsetX: Double, offsetY: Double): Boolean {
        return super<ContainerEventHandler>.mouseDragged(click, offsetX, offsetY)
    }

    override fun charTyped(input: CharacterEvent): Boolean {
        return super<ContainerEventHandler>.charTyped(input)
    }

    override fun getCurrentFocusPath(): ComponentPath? {
        return super<ContainerEventHandler>.currentFocusPath
    }

    override fun nextFocusPath(navigation: FocusNavigationEvent): ComponentPath? {
        val nav = super<ContainerEventHandler>.nextFocusPath(navigation)
        return nav
    }

    override fun setFocused(focused: Boolean) {
        if (!focused) {
            this.focusedElement?.isFocused = false
            this.focusedElement = null
        }
    }

    override fun getFocused(): GuiEventListener? {
        return focusedElement
    }

    override fun setFocused(focused: GuiEventListener?) {
        if (focusedElement === focused) return
        this.focusedElement?.isFocused = false
        focused?.isFocused = true
        this.focusedElement = focused
    }

    override fun isFocused(): Boolean {
        return focusedElement != null
    }

    override fun keyPressed(input: KeyEvent): Boolean {
        return super<ContainerEventHandler>.keyPressed(input)
    }

    override fun keyReleased(input: KeyEvent): Boolean {
        return super<ContainerEventHandler>.keyReleased(input)
    }

    override fun updateWidgetNarration(builder: NarrationElementOutput) {
        narrationAppender.accept(builder)
        val list: List<NarratableEntry> = this.selectables.filter { it.isActive }
        val selectedElementNarrationData = Screen.findNarratableWidget(list, focusedSelectable)
        if (selectedElementNarrationData != null) {
            if (selectedElementNarrationData.priority.isTerminal) {
                focusedSelectable = selectedElementNarrationData.entry
            }
            if (list.size > 1) {
                builder.add(NarratedElementType.POSITION, Component.translatable("fc.narrator.position.child", selectedElementNarrationData.index + 1, list.size))
                if (selectedElementNarrationData.priority.isTerminal) {
                    builder.add(NarratedElementType.USAGE, Component.translatable("narration.component_list.usage"))
                }
            }
            selectedElementNarrationData.entry.updateNarration(builder.nest())
        }
    }

    override fun provideTooltipLines(mouseX: Int, mouseY: Int, parentSelected: Boolean, keyboardFocused: Boolean): List<Component> {
        return tooltipProviders.flatMap { it.provideTooltipLines(mouseX, mouseY, parentSelected, keyboardFocused) }
    }

    override fun provideNarrationLines(): List<Component> {
        return tooltipProviders.flatMap { it.provideNarrationLines() }
    }
}