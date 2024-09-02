/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.screen.entry

import me.fzzyhmstrs.fzzy_config.annotations.Action
import me.fzzyhmstrs.fzzy_config.screen.widget.internal.ConfigListWidget
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
    actions: Set<Action>,
    parent: ConfigListWidget,
    widget: ClickableWidget,
    val copyAction: Runnable?,
    val pasteAction: Runnable?,
    protected val rightClickAction: RightClickAction?)
    :
    BaseConfigEntry(name, description, actions, parent, widget)
{
    private var clickedWidget = false

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (((Screen.hasShiftDown() && keyCode == GLFW.GLFW_KEY_F10) || keyCode == GLFW.GLFW_KEY_MENU) && rightClickAction != null) {
            rightClickAction.rightClick(this.widget.x, this.widget.y, this)
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if(button == 1 && rightClickAction != null) {
            rightClickAction.rightClick(mouseX.toInt(), mouseY.toInt(), this)
            clickedWidget = false
            return true
        }
        return super.mouseClicked(mouseX, mouseY, button).also { clickedWidget = it }
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return if(clickedWidget) widget.mouseReleased(mouseX, mouseY, button) else super.mouseReleased(mouseX, mouseY, button)
    }
}