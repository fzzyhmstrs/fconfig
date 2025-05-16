/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.screen

import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget
import me.fzzyhmstrs.fzzy_config.util.TriState
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.ParentElement
import java.util.*

/**
 * A parent element that supports displaying "Popups" made via [PopupWidget].
 *
 * If a PopupParentElement has one or more active popups, it will prioritize those over passing inputs to any "normal" children. If you have any input handling you need to implement, make sure to handle it and return before calling super, so avoid any active popups potentially doubling up on captured input handling.
 * @see me.fzzyhmstrs.fzzy_config.screen.PopupWidgetScreen
 * @author fzzyhmstrs
 * @since 0.2.0
 */
//client
@JvmDefaultWithCompatibility
interface PopupParentElement: ParentElement, PopupController {

    override val child: LastSelectable?
        get() = focused as? LastSelectable

    override fun pushLast() {
        this.focused?.isFocused = false
        this.lastSelected = focused
    }
    override fun popLast() {
        lastSelected?.isFocused = true
        focused = lastSelected
    }

    override fun hoveredElement(mouseX: Double, mouseY: Double): Optional<Element> {
        return if (popupWidgets.isEmpty())
            return super.hoveredElement(mouseX, mouseY)
        else
            Optional.empty()
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val popupWidget = activeWidget() ?: return mouseClick(mouseX, mouseY, button)
        if (popupWidget.mouseClicked(mouseX, mouseY, button) || popupWidget.isMouseOver(mouseX, mouseY)) {
            return true
        } else if(popupWidget.closesOnMissedClick() != TriState.FALSE) {
            setPopupInternal(null, mouseX, mouseY, false)
            if (popupWidget.closesOnMissedClick().asBoolean)
                return mouseClick(mouseX, mouseY, button)
        }
        return false
    }

    private fun mouseClick(mouseX: Double, mouseY: Double, button: Int): Boolean {
        for (element in this.children()) {
            if (element.mouseClicked(mouseX, mouseY, button)) {
                this.focused = element
                if (button == 0) {
                    this.isDragging = true
                }

                return true
            }
        }
        return false
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (justClosedWidget) {
            justClosedWidget = false
            return false
        }
        val popupWidget = activeWidget() ?: return super.mouseReleased(mouseX, mouseY, button)
        if (popupWidget.isMouseOver(mouseX, mouseY) || popupWidget.isDragging) {
            return popupWidget.mouseReleased(mouseX, mouseY, button)
        }
        return false
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, verticalAmount: Double): Boolean {
        val popupWidget = activeWidget() ?: return super.mouseScrolled(mouseX, mouseY, verticalAmount)
        return popupWidget.mouseScrolled(mouseX, mouseY, verticalAmount)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        val popupWidget = activeWidget() ?: return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
        return popupWidget.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
    }

    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        return activeWidget()?.keyReleased(keyCode, scanCode, modifiers) ?: super.keyReleased(keyCode, scanCode, modifiers)
    }

    override fun charTyped(chr: Char, modifiers: Int): Boolean {
        return activeWidget()?.charTyped(chr, modifiers) ?: super.charTyped(chr, modifiers)
    }
}