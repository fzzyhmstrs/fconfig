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
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.ParentElement
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
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

    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        val popupWidget = activeWidget() ?: return mouseClick(click, doubled)
        if (popupWidget.mouseClicked(click, doubled) || popupWidget.isMouseOver(click.x, click.y)) {
            return true
        } else if(popupWidget.closesOnMissedClick() != TriState.FALSE) {
            setPopupInternal(null, click.x, click.y, false)
            if (popupWidget.closesOnMissedClick().asBoolean)
                return mouseClick(click, doubled)
        }
        return false
    }

    private fun mouseClick(click: Click, doubled: Boolean): Boolean {
        for (element in this.children()) {
            if (element.mouseClicked(click, doubled)) {
                this.focused = element
                if (click.button() == 0) {
                    this.isDragging = true
                }

                return true
            }
        }
        return false
    }

    override fun mouseReleased(click: Click): Boolean {
        if (justClosedWidget) {
            justClosedWidget = false
            return false
        }
        val popupWidget = activeWidget() ?: return super.mouseReleased(click)
        if (popupWidget.isMouseOver(click.x, click.y) || popupWidget.isDragging) {
            return popupWidget.mouseReleased(click)
        }
        return false
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        val popupWidget = activeWidget() ?: return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
        return popupWidget.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun mouseDragged(click: Click, offsetX: Double, offsetY: Double): Boolean {
        val popupWidget = activeWidget() ?: return super.mouseDragged(click, offsetX, offsetY)
        return popupWidget.mouseDragged(click, offsetX, offsetY)
    }

    override fun keyReleased(input: KeyInput): Boolean {
        return activeWidget()?.keyReleased(input) ?: super.keyReleased(input)
    }

    override fun charTyped(input: CharInput): Boolean {
        return activeWidget()?.charTyped(input) ?: super.charTyped(input)
    }
}