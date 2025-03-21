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

import me.fzzyhmstrs.fzzy_config.nullCast
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget
import me.fzzyhmstrs.fzzy_config.util.TriState
import net.minecraft.client.MinecraftClient
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
    /**
     * A stack for holding popupwidgets while allowing for easy list iteration as needed. For rendering this stack should be traversed in reverse order, which LinkedList makes easy with `descendingIterator`
     * @author fzzyhmstrs
     * @since 0.2.0, override from PopupController 0.6.7, scheduled for removal 0.7.0
     */
    @Deprecated("Override of PopupController Method. This instance scheduled for removal 0.7.0")
    override val popupWidgets: LinkedList<PopupWidget>

    /**
     * Boolean prevents `mouseReleased` from triggering on the Popup or Widget underneath the active popup if it's closed on `mouseClicked`
     * @author fzzyhmstrs
     * @since 0.2.0, override from PopupController 0.6.7, scheduled for removal 0.7.0
     */
    @Deprecated("Override of PopupController Method. This instance scheduled for removal 0.7.0")
    override var justClosedWidget: Boolean

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

    @Deprecated("Override of PopupController Method. This instance scheduled for removal 0.7.0")
    override fun activeWidget(): PopupWidget? {
        return super.activeWidget()
    }

    /**
     * Called by this parent element when it pushes a PopupWidget to its stack. This method should "blur" the focus of the underlying children in this parent element; using `blur()` from Screen, for example.
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @Deprecated("Override of PopupController Method. This instance scheduled for removal 0.7.0")
    override fun blurElements()

    /**
     * called when a Popup is pushed to this element, after blurring.
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @Deprecated("Override of PopupController Method. This instance scheduled for removal 0.7.0")
    override fun initPopup(widget: PopupWidget)

    /**
     * Applies a popup widget to this parent. If null is passed, removes the top (newest) popup instead
     * @param widget [PopupWidget], nullable. If not null, will be added to the top of this parent's popup stack, otherwise the top element will be popped
     * @param mouseX Double, nullable. If not null, will be used to reset mouse hover context when the last popup is removed.
     * @param mouseY Double, nullable. If not null, will be used to reset mouse hover context when the last popup is removed.
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @Deprecated("Override of PopupController Method. This instance scheduled for removal 0.7.0")
    override fun setPopup(widget: PopupWidget?, mouseX: Double?, mouseY: Double?) {
        super.setPopup(widget, mouseX, mouseY)
    }

    /**
     * Applies a popup widget to this parent. If null is passed, removes the top (newest) popup instead
     * @param widget [PopupWidget], nullable. If not null, will be added to the top of this parent's popup stack, otherwise the top element will be popped
     * @param mouseX Double, nullable. If not null, will be used to reset mouse hover context when the last popup is removed.
     * @param mouseY Double, nullable. If not null, will be used to reset mouse hover context when the last popup is removed.
     * @author fzzyhmstrs
     * @since 0.6.6
     */
    @Deprecated("Override of PopupController Method. This instance scheduled for removal 0.7.0")
    override fun setPopupImmediate(widget: PopupWidget?, mouseX: Double?, mouseY: Double?) {
        super.setPopupInternal(widget, mouseX, mouseY)
    }

    @Deprecated("Override of PopupController Method. This instance scheduled for removal 0.7.0")
    override fun setPopupInternal(widget: PopupWidget?, mouseX: Double?, mouseY: Double?) {
        super.setPopupInternal(widget, mouseX, mouseY)
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
            setPopupInternal(null, mouseX, mouseY)
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

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        val popupWidget = activeWidget() ?: return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
        return popupWidget.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
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