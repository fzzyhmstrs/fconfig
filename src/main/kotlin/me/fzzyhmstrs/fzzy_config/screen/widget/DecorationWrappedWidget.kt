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

import me.fzzyhmstrs.fzzy_config.screen.decoration.SpriteDecorated
import me.fzzyhmstrs.fzzy_config.util.FcText
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Selectable
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.util.Identifier

/**
 * A widget that wraps another widget with a decoration
 *
 * Decorations are not rendered by this widget, something like a custom Screen or ParentElement that is checking for Decorated will typically render it
 * @param child ClickableWidget - the widget wrapped by this one
 * @param decoration Identifier - id of the decoration to render
 * @author fzzyhmstrs
 * @since 0.2.0
 */
class DecorationWrappedWidget(private val child: ClickableWidget, private val decoration: Identifier): ClickableWidget(child.x, child.y, child.width, child.height, FcText.empty()),
                                                                                                       SpriteDecorated {

    override fun decorationId(): Identifier {
        return decoration
    }

    override fun renderButton(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        child.render(context, mouseX, mouseY, delta)
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

    override fun mouseScrolled(mouseX: Double, mouseY: Double, verticalAmount: Double): Boolean {
        return child.mouseScrolled(mouseX, mouseY, verticalAmount)
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

    override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
        child.appendNarrations(builder)
    }
}