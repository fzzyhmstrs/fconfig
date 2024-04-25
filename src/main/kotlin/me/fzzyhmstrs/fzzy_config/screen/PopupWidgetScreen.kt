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

import com.mojang.blaze3d.systems.RenderSystem
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.tooltip.TooltipPositioner
import net.minecraft.client.gui.widget.*
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
 * - call super.keyPressed BEFORE custom keypresses, otherwise clicks may improperly propagate through popups to elements underneath them
 * @param title Text, the screen title
 * @see PopupParentElement
 * @see PopupWidget.Api
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@Environment(EnvType.CLIENT)
open class PopupWidgetScreen(title: Text) : Screen(title), PopupParentElement {

    override val popupWidgets: LinkedList<PopupWidget> = LinkedList()
    override var justClosedWidget: Boolean = false
    override var lastSelected: Element? = null

    @Internal
    override fun blurElements() {
        val guiNavigationPath = this.focusedPath
        guiNavigationPath?.setFocused(false)
    }

    @Internal
    override fun initPopup(widget: PopupWidget) {
        widget.position(width,height)
    }

    protected open fun initPopup() {
        popupWidgets.descendingIterator().forEach {
            it.position(width,height)
        }
    }

    override fun resize(client: MinecraftClient, width: Int, height: Int) {
        super.resize(client, width, height)
        initPopup()
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        if (popupWidgets.isEmpty())
            super.render(context, mouseX, mouseY, delta)
        else {
            context.matrices.push()
            context.matrices.translate(0f,0f,-450f*popupWidgets.size)
            super.render(context, 0, 0, delta)
            context.matrices.pop()
        }
        context.matrices.push()
        context.matrices.translate(0f,0f,-450f*popupWidgets.size + 450f)
        for ((index,popup) in popupWidgets.descendingIterator().withIndex()) {
            if(index == popupWidgets.lastIndex)
                popup.render(context, mouseX, mouseY, delta)
            else
                popup.render(context, 0, 0, delta)
            context.matrices.translate(0f,0f,450f)
        }
        context.matrices.pop()
        if (popupWidgets.isNotEmpty())
            RenderSystem.disableDepthTest()
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

    override fun addScreenNarrations(messageBuilder: NarrationMessageBuilder) {
        activeWidget()?.appendNarrations(messageBuilder) ?: super.addScreenNarrations(messageBuilder)
    }

    override fun setTooltip(tooltip: Tooltip?, positioner: TooltipPositioner?, focused: Boolean) {
        super.setTooltip(tooltip, positioner, focused)
    }
}