/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.screen.widget

import me.fzzyhmstrs.fzzy_config.util.FcText
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.client.input.KeyCodes
import net.minecraft.entity.ai.pathing.NavigationType
import org.lwjgl.glfw.GLFW
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * A button widget that masquerades as a text field widget. The text within is not editable.
 * @param textSupplier [Supplier]&lt;String&gt; - supplier of the message the "text field" displays
 * @param onClick [Consumer]&lt;OnClickTextFieldWidget&gt; - action to take when the button is pressed
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@Environment(EnvType.CLIENT)
class OnClickTextFieldWidget(private val textSupplier: Supplier<String>, private val onClick: OnInteractAction)
    :
    TextFieldWidget(MinecraftClient.getInstance().textRenderer,0,0, 110, 20, FcText.empty())
{
    init {
        setMaxLength(1000)
        this.text = textSupplier.get()
        this.setCursorToStart(false)
    }

    override fun renderWidget(context: DrawContext?, mouseX: Int, mouseY: Int, delta: Float) {
        super.renderWidget(context, mouseX, mouseY, delta)
        this.text = textSupplier.get()
        this.setCursorToStart(false)
    }

    override fun onClick(mouseX: Double, mouseY: Double) {
        onClick.interact(this, false, 0,0,0)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        return if (!this.isFocused || isNavigation(keyCode)) {
            false
        } else {
            if(KeyCodes.isToggle(keyCode))
                onClick.interact(this, false, keyCode, scanCode, modifiers)
            else
                onClick.interact(this, true, keyCode, scanCode, modifiers)
            return true
        }
    }
    private fun isNavigation(keyCode: Int): Boolean{
        return keyCode == GLFW.GLFW_KEY_TAB || keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_DOWN || keyCode == GLFW.GLFW_KEY_UP
    }

    /**
     * Called when the text field widget is interacted with
     *
     * SAM: [interact] - callback with the widget as context, as well as optional keyboard input + flag if kb input is present (isKeyboard == true) or just dummies (isKeyboard == false)
     * @author fzzyhmstrs
     * @since 0.3.0
     */
    @FunctionalInterface
    fun interface OnInteractAction{
        /**
         * interaction callback from an [OnClickTextFieldWidget]
         * @param widget [OnClickTextFieldWidget] - context from the widget calling back
         * @param isKeyboard Boolean - if this callback is passing valid keyboard inputs. If false, keyCode, scanCode, and modifiers are dummy values
         * @param keyCode Int - if isKeyboard, the keycode passed through from `keyPressed`
         * @param scanCode Int - if isKeyboard, the scancode passed through from `keyPressed`
         * @param modifiers Int - if isKeyboard, the modifiers passed through from `keyPressed`
         * @author fzzyhmstrs
         * @since 0.3.0
         */
        fun interact(widget: OnClickTextFieldWidget, isKeyboard: Boolean, keyCode: Int, scanCode: Int, modifiers: Int)
    }

}