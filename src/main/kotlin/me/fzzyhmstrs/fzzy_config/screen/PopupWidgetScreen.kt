package me.fzzyhmstrs.fzzy_config.screen

import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.widget.*
import net.minecraft.text.Text
import org.lwjgl.glfw.GLFW
import java.util.*

/**
 * A screen that p
 */
@Environment(EnvType.CLIENT)
open class PopupWidgetScreen(title: Text) : Screen(title), PopupParentElement {

    override val popupWidgets: LinkedList<PopupWidget> = LinkedList()
    override var justClosedWidget: Boolean = false
    override var lastSelected: Element? = null

    override fun blurElements() {
        this.blur()
    }

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
        else
            super.render(context, 0, 0, delta)
        for ((index,popup) in popupWidgets.descendingIterator().withIndex()) {
            if(index == popupWidgets.lastIndex)
                popup.render(context, mouseX, mouseY, delta)
            else
                popup.render(context, 0, 0, delta)
            context.matrices.translate(0f,0f,500f)
        }
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
}
