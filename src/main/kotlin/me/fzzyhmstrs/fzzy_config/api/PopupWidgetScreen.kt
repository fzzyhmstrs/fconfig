package me.fzzyhmstrs.fzzy_config.api

import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.widget.*
import net.minecraft.text.Text
import org.lwjgl.glfw.GLFW

/**
 * A screen that p
 */
@Environment(EnvType.CLIENT)
open class PopupWidgetScreen(title: Text) : Screen(title) {

    private var popupWidget: PopupWidget? = null

    protected open fun initPopup() {
        popupWidget?.position(width, height)
    }

    override fun resize(client: MinecraftClient, width: Int, height: Int) {
        super.resize(client, width, height)
        initPopup()
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if(popupWidget != null){
            if (popupWidget?.isMouseOver(mouseX, mouseY) == true){
                return popupWidget?.mouseClicked(mouseX, mouseY, button) ?: super.mouseClicked(mouseX, mouseY, button)
            }
            return false
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if(popupWidget != null){
            if (popupWidget?.isMouseOver(mouseX, mouseY) == true || popupWidget?.isDragging == true) {
                return popupWidget?.mouseReleased(mouseX, mouseY, button) ?: super.mouseReleased(mouseX, mouseY, button)
            }
            return false
        }
        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        if(popupWidget != null){
            if (popupWidget?.isMouseOver(mouseX, mouseY) == true){
                return popupWidget?.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount) ?: super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if(popupWidget != null){
            if (keyCode == GLFW.GLFW_KEY_ESCAPE){
                popupWidget?.onClose()
                popupWidget = null
                return true
            }
            if(popupWidget?.keyPressed(keyCode, scanCode, modifiers) == true)
                return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        return popupWidget?.keyReleased(keyCode, scanCode, modifiers) ?: super.keyReleased(keyCode, scanCode, modifiers)
    }

    override fun charTyped(chr: Char, modifiers: Int): Boolean {
        return popupWidget?.charTyped(chr, modifiers) ?: super.charTyped(chr, modifiers)
    }

    override fun addScreenNarrations(messageBuilder: NarrationMessageBuilder) {
        popupWidget?.appendNarrations(messageBuilder) ?: super.addScreenNarrations(messageBuilder)
    }

    fun setPopup(widget: PopupWidget?){
        popupWidget?.onClose()
        widget?.position(width, height)
        popupWidget = widget
    }

}