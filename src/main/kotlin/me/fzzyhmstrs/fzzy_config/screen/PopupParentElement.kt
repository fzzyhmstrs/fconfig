package me.fzzyhmstrs.fzzy_config.screen

import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.ParentElement
import java.util.*

/**
 * A parent element that supports displaying "Popups" made via [PopupWidget].
 *
 * If a PopupParentElement has one or more active popups, it will prioritize those over passing inputs to any "normal" children.
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@Environment(EnvType.CLIENT)
@JvmDefaultWithCompatibility
interface PopupParentElement: ParentElement, LastSelectable {
    val popupWidgets: LinkedList<PopupWidget>
    var justClosedWidget: Boolean

    override fun pushLast(){
        this.lastSelected = focused
    }
    override fun popLast(){
        focused = lastSelected
    }

    fun activeWidget(): PopupWidget?

    fun blurElements()

    fun initPopup(widget: PopupWidget)

    fun setPopup(widget: PopupWidget?) {
        if(widget == null){
            justClosedWidget = true
            popupWidgets.pop().onClose()
            popupWidgets.peek()?.blur()
            if (popupWidgets.isEmpty()) {
                (lastSelected as? LastSelectable)?.popLast()
                popLast()
            }
        } else {
            if (popupWidgets.isEmpty()) {
                pushLast()
                (lastSelected as? LastSelectable)?.pushLast()
            }
            this.blurElements()
            popupWidgets.push(widget)
            initPopup(widget)
        }
    }

    override fun hoveredElement(mouseX: Double, mouseY: Double): Optional<Element> {
        return if (popupWidgets.isEmpty())
            return super.hoveredElement(mouseX, mouseY)
        else
            Optional.empty()
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val popupWidget = activeWidget() ?: return super.mouseClicked(mouseX, mouseY, button)
        if (popupWidget.mouseClicked(mouseX, mouseY, button) || popupWidget.isMouseOver(mouseX, mouseY)) {
            return true
        } else if(popupWidget.closesOnMissedClick()) {
                setPopup(null)
        }
        return false
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (justClosedWidget){
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