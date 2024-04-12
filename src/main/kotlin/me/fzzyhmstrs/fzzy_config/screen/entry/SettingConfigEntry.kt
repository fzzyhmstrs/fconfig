package me.fzzyhmstrs.fzzy_config.screen.entry

import me.fzzyhmstrs.fzzy_config.screen.widget.ConfigListWidget
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.Text
import org.lwjgl.glfw.GLFW

@Environment(EnvType.CLIENT)
internal open class SettingConfigEntry(
    name: Text,
    description: Text,
    isRestartTriggering: Boolean,
    parent: ConfigListWidget,
    widget: ClickableWidget,
    val copyAction: Runnable?,
    val pasteAction: Runnable?,
    protected val rightClickAction: RightClickAction?)
    :
    BaseConfigEntry(name, description, isRestartTriggering, parent, widget)
{
    private var clickedWidget = false

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (((Screen.hasShiftDown() && keyCode == GLFW.GLFW_KEY_F10) || keyCode == GLFW.GLFW_KEY_MENU) && rightClickAction != null){
            rightClickAction.rightClick(this.widget.x,this.widget.y,this)
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if(button == 1 && rightClickAction != null){
            rightClickAction.rightClick(mouseX.toInt(),mouseY.toInt(),this)
            clickedWidget = false
            return true
        }
        return super.mouseClicked(mouseX, mouseY, button).also { clickedWidget = it }
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return if(clickedWidget) widget.mouseReleased(mouseX, mouseY, button) else super.mouseReleased(mouseX, mouseY, button)
    }
}