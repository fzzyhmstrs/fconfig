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

import me.fzzyhmstrs.fzzy_config.util.FcText
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Selectable
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.Text

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
class LabelWrappedWidget(private val child: ClickableWidget, private val label: Text?)
    : ClickableWidget(child.x, child.y, child.width, child.height, label ?: FcText.EMPTY) {

    override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        child.render(context, mouseX, mouseY, delta)
        if (label != null) {
            context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, label.asOrderedText(), x, y + height - 9, -1)
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return child.mouseClicked(mouseX, mouseY, button)
    }
    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        return child.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
    }
    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return child.mouseReleased(mouseX, mouseY, button)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        return child.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        return child.keyPressed(keyCode, scanCode, modifiers)
    }
    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        return child.keyReleased(keyCode, scanCode, modifiers)
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

    override fun isSelected(): Boolean {
        return child.isSelected
    }

    override fun getType(): Selectable.SelectionType {
        return child.type
    }

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return child.isMouseOver(mouseX, mouseY)
    }

    override fun getTooltip(): Tooltip? {
        return child.tooltip
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
        return if (label == null) child.height else child.height + 11
    }

    override fun setWidth(width: Int) {
        child.width = width
    }

    override fun setHeight(height: Int) {
        child.height = height
    }

    override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
        child.appendNarrations(builder)
    }
}