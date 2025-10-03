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
import net.minecraft.client.gui.navigation.GuiNavigation
import net.minecraft.client.gui.navigation.GuiNavigationPath
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import net.minecraft.text.Text
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
    : ClickableWidget(x, y, width, height, FcText.empty()), ParentElement, TooltipChild {

    private var children: MutableList<Element> = mutableListOf()
    private var drawables: List<Drawable> = emptyList()
    private var selectables: List<Selectable> = emptyList()
    private var tooltipProviders: List<TooltipChild> = emptyList()
    private var focusedSelectable: Selectable? = null
    private var focusedElement: Element? = null
    private var dragging: Boolean = false
    private var narrationAppender: Consumer<NarrationMessageBuilder> = Consumer { _-> }

    /**
     * Applies custom narration to this layout widget. This consumer will be applied on every call to narration building, using the same message level as the default narration of the widget. In particular, element POSITION and USAGE information is typically applied already. The current hovered and/or focused child widget uses the next message layer. If you have to dodge one of both of these existing additions, you will have to add one or two more layers to the builder before using it.
     * @param narrationAppender Consumer&lt;[NarrationMessageBuilder]&gt; appends arbitrary narration information to the provided builder.
     * @return this widget
     * @author fzzyhmstrs
     * @since 0.6.5
     */
    fun withNarrationAppender(narrationAppender: Consumer<NarrationMessageBuilder>): LayoutClickableWidget {
        this.narrationAppender = narrationAppender
        return this
    }

    init {
        layout.setPos(ReferencePos { this.x }, ReferencePos { this.y })
        layout.setDimensions(width, height)
        val c: MutableList<Element> = mutableListOf()
        val d: MutableList<Drawable> = mutableListOf()
        val s: MutableList<Selectable> = mutableListOf()
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

    override fun setDimensions(width: Int, height: Int) {
        super.setWidth(width)
        super.setHeight(height)
        layout.setDimensions(width, height)
    }

    override fun setDimensionsAndPosition(width: Int, height: Int, x: Int, y: Int) {
        super.setWidth(width)
        super.setHeight(height)
        super.setX(x)
        super.setY(y)
        layout.setDimensions(width, height)
        layout.update()
    }

    override fun children(): MutableList<out Element> {
        return children
    }

    override fun isDragging(): Boolean {
        return dragging
    }

    override fun setDragging(dragging: Boolean) {
        this.dragging = dragging
    }


    override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        for (d in drawables) {
            d.render(context, mouseX, mouseY, delta)
        }
        //renderCustom(context, mouseX, mouseY, delta)
    }

    override fun onClick(mouseX: Double, mouseY: Double) {
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return super<ParentElement>.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return super<ParentElement>.mouseReleased(mouseX, mouseY, button)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        return super<ParentElement>.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        return super<ParentElement>.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
    }

    override fun charTyped(input: CharInput): Boolean {
        return super<ParentElement>.charTyped(input)
    }

    override fun getFocusedPath(): GuiNavigationPath? {
        return super<ParentElement>.getFocusedPath()
    }

    override fun getNavigationPath(navigation: GuiNavigation?): GuiNavigationPath? {
        val nav = super<ParentElement>.getNavigationPath(navigation)
        return nav
    }

    override fun setFocused(focused: Boolean) {
        if (!focused) {
            this.focusedElement?.isFocused = false
            this.focusedElement = null
        }
    }

    override fun getFocused(): Element? {
        return focusedElement
    }

    override fun setFocused(focused: Element?) {
        if (focusedElement === focused) return
        this.focusedElement?.isFocused = false
        focused?.isFocused = true
        this.focusedElement = focused
    }

    override fun isFocused(): Boolean {
        return focusedElement != null
    }

    override fun keyPressed(input: KeyInput): Boolean {
        return super<ParentElement>.keyPressed(input)
    }

    override fun keyReleased(input: KeyInput): Boolean {
        return super<ParentElement>.keyReleased(input)
    }

    override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
        narrationAppender.accept(builder)
        val list: List<Selectable> = this.selectables.filter { it.isInteractable }
        val selectedElementNarrationData = Screen.findSelectedElementData(list, focusedSelectable)
        if (selectedElementNarrationData != null) {
            if (selectedElementNarrationData.selectType.isFocused) {
                focusedSelectable = selectedElementNarrationData.selectable
            }
            if (list.size > 1) {
                builder.put(NarrationPart.POSITION, Text.translatable("fc.narrator.position.child", selectedElementNarrationData.index + 1, list.size))
                if (selectedElementNarrationData.selectType.isFocused) {
                    builder.put(NarrationPart.USAGE, Text.translatable("narration.component_list.usage"))
                }
            }
            selectedElementNarrationData.selectable.appendNarrations(builder.nextMessage())
        }
    }
}