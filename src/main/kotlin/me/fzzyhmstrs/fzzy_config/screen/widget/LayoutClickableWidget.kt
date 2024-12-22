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

import me.fzzyhmstrs.fzzy_config.simpleId
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.pos.ReferencePos
import net.minecraft.client.gui.*
import net.minecraft.client.gui.navigation.GuiNavigation
import net.minecraft.client.gui.navigation.GuiNavigationPath
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.Text
import net.minecraft.util.Identifier

/**
 * Clickable widget that contains a LayoutWidget which provides and lays out children of this widget. The layout will be automatically constrained to the dimensions of the widget, and updated as the position and size changes.
 * @param x X position of the widget in pixels
 * @param y Y position of the widget in pxels
 * @param width width of the widget in pixels
 * @param height height of the widget in pixels
 * @param layout [LayoutWidget] the layout wrapped by this widget
 * @author fzzyhmstrs
 * @since 0.6.0
 */
class LayoutClickableWidget(x: Int, y: Int, width: Int, height: Int, private val layout: LayoutWidget): ClickableWidget(x, y, width, height, FcText.empty()),
                                                                                                        ParentElement, TooltipChild {

    private var children: MutableList<Element> = mutableListOf()
    private var drawables: List<Drawable> = listOf()
    private var selectables: List<Selectable> = listOf()
    private var tooltipProviders: List<TooltipChild> = listOf()
    private var focusedSelectable: Selectable? = null
    private var focusedElement: Element? = null
    private var dragging: Boolean = false

    init {
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
        layout.setPos(ReferencePos { this.x }, ReferencePos { this.y })
        layout.setDimensions(width, height)
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

    override fun charTyped(chr: Char, modifiers: Int): Boolean {
        return super<ParentElement>.charTyped(chr, modifiers)
    }

    override fun getNavigationPath(navigation: GuiNavigation?): GuiNavigationPath? {
        return super<ParentElement>.getNavigationPath(navigation)
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
        this.focusedElement?.isFocused = false
        focused?.isFocused = true
        this.focusedElement = focused
    }

    override fun isFocused(): Boolean {
        return focusedElement != null
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        return super<ParentElement>.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
      return super<ParentElement>.keyReleased(keyCode, scanCode, modifiers)
    }

    override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
        val list: List<Selectable> = this.selectables.filter { it.isNarratable }
        val selectedElementNarrationData = Screen.findSelectedElementData(list, focusedSelectable)
        if (selectedElementNarrationData != null) {
            if (selectedElementNarrationData.selectType.isFocused) {
                focusedSelectable = selectedElementNarrationData.selectable
            }
            if (list.size > 1) {
                builder.put(NarrationPart.POSITION, Text.translatable("narrator.position.object_list", selectedElementNarrationData.index + 1, list.size))
                if (selectedElementNarrationData.selectType.isFocused) {
                    builder.put(NarrationPart.USAGE, Text.translatable("narration.component_list.usage"))
                }
            }
            selectedElementNarrationData.selectable.appendNarrations(builder.nextMessage())
        }
    }

    private companion object {

        private val tex =  "widget/button".simpleId()
        private val disabled = "widget/button_disabled".simpleId()
        private val highlighted = "widget/button_highlighted".simpleId()

        fun get(enabled: Boolean, focused: Boolean): Identifier {
            return if (enabled) {
                if (focused) highlighted else tex
            } else {
                disabled
            }
        }
    }
}