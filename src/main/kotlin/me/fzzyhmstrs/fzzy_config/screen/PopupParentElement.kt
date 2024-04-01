package me.fzzyhmstrs.fzzy_config.screen

import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.ParentElement
import java.util.*

@JvmDefaultWithCompatibility
interface PopupParentElement: ParentElement {
    val popupWidgets: LinkedList<PopupWidget>

    fun activeWidget(): PopupWidget?

    fun setPopup(widget: PopupWidget?)

    override fun hoveredElement(mouseX: Double, mouseY: Double): Optional<Element> {
        return if (popupWidgets.isEmpty())
            return super.hoveredElement(mouseX, mouseY)
        else
            Optional.empty()
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val popupWidget = activeWidget() ?: return super.mouseClicked(mouseX, mouseY, button)
        if (popupWidget.isMouseOver(mouseX, mouseY)){
            return popupWidget.mouseClicked(mouseX, mouseY, button)
        } else {
            if(popupWidget.closesOnMissedClick())
                setPopup(null)
        }
        return false
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val popupWidget = activeWidget() ?: return super.mouseReleased(mouseX, mouseY, button)
        if (popupWidget.isMouseOver(mouseX, mouseY) || popupWidget.isDragging) {
            return popupWidget.mouseReleased(mouseX, mouseY, button)
        }
        return false
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        val popupWidget = activeWidget() ?: return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
        if (popupWidget.isMouseOver(mouseX, mouseY))
            return popupWidget.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        val popupWidget = activeWidget() ?: return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
        if (popupWidget.isMouseOver(mouseX, mouseY))
            return popupWidget.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
        return false
    }

    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        return activeWidget()?.keyReleased(keyCode, scanCode, modifiers) ?: super.keyReleased(keyCode, scanCode, modifiers)
    }

    override fun charTyped(chr: Char, modifiers: Int): Boolean {
        return activeWidget()?.charTyped(chr, modifiers) ?: super.charTyped(chr, modifiers)
    }

}