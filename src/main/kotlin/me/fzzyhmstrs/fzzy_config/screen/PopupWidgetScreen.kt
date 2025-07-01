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
import me.fzzyhmstrs.fzzy_config.screen.internal.SuggestionWindow
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.text.Text
import org.jetbrains.annotations.ApiStatus.Internal
import org.lwjgl.glfw.GLFW
import java.util.*

/**
 * A base screen that supports popup widgets. See [PopupParentElement] for documentation on underlying implementations.
 *
 * To add and remove popups from this screen, call [PopupWidget.push] and [PopupWidget.pop]
 *
 * For most intents and purposes, treat this screen like a standard [Screen], with a couple caveats:
 * - Make sure to call super on [resize], [render], and [keyPressed] otherwise popup functionality will break
 * - call super.render AFTER any custom rendering, or popups may appear improperly below custom elements
 * - call super.keyPressed BEFORE custom key presses, otherwise clicks may improperly propagate through popups to elements underneath them
 * @param title Text, the screen title
 * @see PopupParentElement
 * @see PopupWidget.Api
 * @author fzzyhmstrs
 * @since 0.2.0, made render final and added renderContents 0.6.0
 */
//client
open class PopupWidgetScreen(title: Text) : Screen(title), PopupParentElement {

    override val popupWidgets: LinkedList<PopupWidget> = LinkedList()
    override var justClosedWidget: Boolean = false
    override var lastSelected: Element? = null
    protected var hoveredElement: Element? = null

    override var suggestionWindow: SuggestionWindow? = null

    @Internal
    override fun blurElements() {
        this.blur()
    }

    @Internal
    override fun initPopup(widget: PopupWidget) {
        widget.position(width, height)
    }

    protected open fun initPopup() {
        popupWidgets.descendingIterator().forEach {
            it.position(width, height)
        }
    }

    override fun resetHover(mouseX: Double, mouseY: Double) {
        hoveredElement = if (popupWidgets.isNotEmpty()) null else children().firstOrNull { it.isMouseOver(mouseX, mouseY) }
        hoveredElement.nullCast<LastSelectable>()?.resetHover(mouseX, mouseY)
    }

    override fun resize(client: MinecraftClient, width: Int, height: Int) {
        super.resize(client, width, height)
        initPopup()
    }

    override fun setFocused(focused: Element?) {
        if (this.focused === focused) return
        super<Screen>.setFocused(focused)
    }

    /**
     * Marked final to preserve proper popup ordering and rendering
     * @since 0.6.0
     */
    final override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        hoveredElement = if (popupWidgets.isNotEmpty()) null else children().firstOrNull { it.isMouseOver(mouseX.toDouble(), mouseY.toDouble()) }
        preRender(context, mouseX, mouseY, delta)
        renderContents(context, mouseX, mouseY, delta)
        postRender(context, mouseX, mouseY, delta)
    }

    /**
     * Render call that should be used to render the main contents of a subclass. This is used instead of overriding [render] for proper positioning of the popup stack.
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    open fun renderContents(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        if (popupWidgets.isEmpty())
            super.render(context, mouseX, mouseY, delta)
        else
            super.render(context, 0, 0, delta)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        val popupWidget = activeWidget() ?: return super<Screen>.keyPressed(keyCode, scanCode, modifiers)
        if (popupWidget.keyPressed(keyCode, scanCode, modifiers))
            return true
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            setPopup(null)
            return true
        }
        return false
    }

    final override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val popupWidget = activeWidget() ?: return onClick(mouseX, mouseY, button)
        val result = popupWidget.preClick(mouseX, mouseY, button)
        if (result == PopupWidget.ClickResult.PASS) {
            return onClick(mouseX, mouseY, button)
        }
        return super<PopupParentElement>.mouseClicked(mouseX, mouseY, button)
    }

    open fun onClick(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return super<PopupParentElement>.mouseClicked(mouseX, mouseY, button)
    }

    override fun addScreenNarrations(messageBuilder: NarrationMessageBuilder) {
        activeWidget()?.appendNarrations(messageBuilder) ?: super.addScreenNarrations(messageBuilder)
    }


}