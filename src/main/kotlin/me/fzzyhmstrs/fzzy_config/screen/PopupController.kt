/*
* Copyright (c) 2025 Fzzyhmstrs
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
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.ParentElement
import java.util.*

/**
 * Basic controller interface for GUIs that want to implement a popup system. This does not handle interaction handling nor rendering, that needs to be integrated into the screen being built.
 *
 * There are some helper methods for rendering popups in a predicatable manner. Make sure to call both!
 * @see me.fzzyhmstrs.fzzy_config.screen.PopupWidgetScreen
 * @see me.fzzyhmstrs.fzzy_config.screen.PopupParentElement
 * @author fzzyhmstrs
 * @since 0.6.7, moved from PopupParentElement originally 0.2.0
 */
//client
@JvmDefaultWithCompatibility
interface PopupController: LastSelectable {
    /**
     * A stack for holding popupwidgets while allowing for easy list iteration as needed. For rendering this stack should be traversed in reverse order, which LinkedList makes easy with `descendingIterator`
     * @author fzzyhmstrs
     * @since 0.6.7, moved from PopupParentElement originally 0.2.0
     */
    val popupWidgets: LinkedList<PopupWidget>

    /**
     * Boolean prevents `mouseReleased` from triggering on the Popup or Widget underneath the active popup if it's closed on `mouseClicked`
     * @author fzzyhmstrs
     * @since 0.6.7, moved from PopupParentElement originally 0.2.0
     */
    var justClosedWidget: Boolean

    val child: LastSelectable?

    fun activeWidget(): PopupWidget? {
        return popupWidgets.peek()
    }

    /**
     * Called by this parent element when it pushes a PopupWidget to its stack. This method should "blur" the focus of the underlying children in this parent element; using `blur()` from Screen, for example.
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun blurElements()

    /**
     * called when a Popup is pushed to this element, after blurring.
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun initPopup(widget: PopupWidget)

    /**
     * Applies a popup widget to this parent. If null is passed, removes the top (newest) popup instead
     * @param widget [PopupWidget], nullable. If not null, will be added to the top of this parent's popup stack, otherwise the top element will be popped
     * @param mouseX Double, nullable. If not null, will be used to reset mouse hover context when the last popup is removed.
     * @param mouseY Double, nullable. If not null, will be used to reset mouse hover context when the last popup is removed.
     * @author fzzyhmstrs
     * @since 0.6.7, moved from PopupParentElement originally 0.2.0
     */
    fun setPopup(widget: PopupWidget?, mouseX: Double? = null, mouseY: Double? = null) {
        push(PopupEntry(this, widget, mouseX, mouseY))
    }

    /**
     * Applies a popup widget to this parent. If null is passed, removes the top (newest) popup instead
     * @param widget [PopupWidget], nullable. If not null, will be added to the top of this parent's popup stack, otherwise the top element will be popped
     * @param mouseX Double, nullable. If not null, will be used to reset mouse hover context when the last popup is removed.
     * @param mouseY Double, nullable. If not null, will be used to reset mouse hover context when the last popup is removed.
     * @author fzzyhmstrs
     * @since 0.6.7, moved from PopupParentElement originally 0.6.6
     */
    fun setPopupImmediate(widget: PopupWidget?, mouseX: Double? = null, mouseY: Double? = null) {
        setPopupInternal(widget, mouseX, mouseY)
    }

    fun setPopupInternal(widget: PopupWidget?, mouseX: Double? = null, mouseY: Double? = null) {
        if(widget == null) {
            if (popupWidgets.isEmpty())
                return
            justClosedWidget = true
            popupWidgets.pop().onClose()
            popupWidgets.peek()?.blur()
            if (popupWidgets.isEmpty()) {
                (lastSelected as? LastSelectable)?.popLast()
                if (mouseX != null && mouseY != null) {
                    this.resetHover(mouseX, mouseY)
                    lastSelected?.nullCast<LastSelectable>()?.resetHover(mouseX, mouseY)
                }
                popLast()
            }
        } else {
            if (popupWidgets.isEmpty()) {
                child?.pushLast()
                pushLast()
            }
            this.blurElements()
            popupWidgets.push(widget)
            initPopup(widget)
        }
    }

    /**
     * Pushes the current matrix and moves the content rendering back to make room for the popups. This needs to be called with [postRender] or the game will crash from a non-empty matrix stack.
     *
     * Generally the render flow should be
     * 1. misc. preparation
     * 2. **preRender**
     * 3. render main screen content
     * 4. postRender
     * @see PopupWidgetScreen
     * @author fzzyhmstrs
     * @since 0.6.7
     */
    fun preRender(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        context.matrices.push()
        if (popupWidgets.isNotEmpty())
            context.matrices.translate(0f, 0f, -500f * popupWidgets.size)
    }

    /**
     * Renders the current open popups in descending order (oldest first/the farthest back) and then pops matrices. This needs to be called with [preRender] or the game will crash from a non-empty matrix stack.
     *
     * Generally the render flow should be
     * 1. misc. preparation
     * 2. preRender
     * 3. render main screen content
     * 4. **postRender**
     * @see PopupWidgetScreen
     * @author fzzyhmstrs
     * @since 0.6.7
     */
    fun postRender(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        if (popupWidgets.isNotEmpty())
            context.matrices.translate(0f, 0f, 500f)
        for ((index, popup) in popupWidgets.descendingIterator().withIndex()) {
            if(index == popupWidgets.lastIndex)
                popup.render(context, mouseX, mouseY, delta)
            else
                popup.render(context, 0, 0, delta)
            context.matrices.translate(0f, 0f, 500f)
        }
        context.matrices.pop()
    }

    data class PopupEntry(val parent: PopupController, val widget: PopupWidget?, val mouseX: Double? = null, val mouseY: Double? = null)

    companion object {
        private val popupStack: LinkedList<PopupEntry> = LinkedList()

        private fun push(entry: PopupEntry) {
            popupStack.push(entry)
        }

        internal fun pop() {
            val popupParentElement = MinecraftClient.getInstance().currentScreen?.nullCast<PopupController>() ?: return
            popupParentElement.setPopupInternal(null, null, null)
        }

        internal fun popAll() {
            while (popupStack.isNotEmpty()) {
                val e = popupStack.pop()
                e.parent.setPopupInternal(e.widget, e.mouseX, e.mouseY)
            }
        }
    }
}