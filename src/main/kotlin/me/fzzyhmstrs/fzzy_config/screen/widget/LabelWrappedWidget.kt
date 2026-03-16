/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.screen.widget

import me.fzzyhmstrs.fzzy_config.nullCast
import net.minecraft.client.Minecraft
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.client.gui.navigation.FocusNavigationEvent
import net.minecraft.client.gui.ComponentPath
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.gui.narration.NarratedElementType
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component

/**
 * A widget that wraps another widget with a display label. The label will appear under the widget aligned left
 *
 * ```
 * [  WIDGET  ]
 * My label
 * ```
 * @param child ClickableWidget - the widget wrapped by this one
 * @param label [Text] - text label to add to the wrapped widget
 * @author fzzyhmstrs
 * @since 0.6.0
 */
class LabelWrappedWidget(private val child: AbstractWidget, private val label: Component, private val showLabel: Boolean = true)
    : AbstractWidget(child.x, child.y, child.width, child.height, label), TooltipChild {

    override fun extractWidgetRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        child.extractRenderState(context, mouseX, mouseY, delta)
        if (showLabel) {
            context.text(Minecraft.getInstance().font, label.visualOrderText, x, y + getHeight() - 9, -1)
        }
    }

    override fun mouseClicked(click: MouseButtonEvent, doubled: Boolean): Boolean {
        return child.mouseClicked(click, doubled)
    }
    override fun mouseDragged(click: MouseButtonEvent, offsetX: Double, offsetY: Double): Boolean {
        return child.mouseDragged(click, offsetX, offsetY)
    }
    override fun mouseReleased(click: MouseButtonEvent): Boolean {
        return child.mouseReleased(click)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        return child.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun keyPressed(input: KeyEvent): Boolean {
        return child.keyPressed(input)
    }
    override fun keyReleased(input: KeyEvent): Boolean {
        return child.keyReleased(input)
    }

    override fun charTyped(input: CharacterEvent): Boolean {
        return child.charTyped(input)
    }

    override fun setFocused(focused: Boolean) {
        child.isFocused = focused
    }
    override fun isFocused(): Boolean {
        return child.isFocused
    }

    override fun isHovered(): Boolean {
        return child.isHovered
    }

    override fun isHoveredOrFocused(): Boolean {
        return child.isHoveredOrFocused
    }

    override fun narrationPriority(): NarratableEntry.NarrationPriority {
        return child.narrationPriority()
    }

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return child.isMouseOver(mouseX, mouseY)
    }

    override fun setX(x: Int) {
        super.setX(x)
        child.x = x
    }

    override fun setY(y: Int) {
        super.setY(y)
        child.y = y
    }

    override fun getWidth(): Int {
        return child.width
    }

    override fun getHeight(): Int {
        return if (showLabel) child.height + 11 else child.height
    }

    override fun setWidth(width: Int) {
        child.width = width
    }

    override fun setHeight(height: Int) {
        child.height = height
    }

    override fun nextFocusPath(navigation: FocusNavigationEvent): ComponentPath? {
        val childPath = child.nextFocusPath(navigation)
        return if(childPath != null) Layered(this, childPath) else null
    }

    override fun updateWidgetNarration(builder: NarrationElementOutput) {
        builder.add(NarratedElementType.TITLE, this.label)
        child.updateNarration(builder.nest())
    }

    override fun provideTooltipLines(mouseX: Int, mouseY: Int, parentSelected: Boolean, keyboardFocused: Boolean): List<Component> {
        return child.nullCast<TooltipChild>()?.provideTooltipLines(mouseX, mouseY, parentSelected, keyboardFocused) ?: TooltipChild.EMPTY
    }

    private class Layered(private val element: GuiEventListener, private val childPath: ComponentPath): ComponentPath {

        override fun component(): GuiEventListener {
            return element
        }

        override fun applyFocus(focused: Boolean) {
            element.isFocused = focused
            childPath.applyFocus(focused)
        }
    }
}