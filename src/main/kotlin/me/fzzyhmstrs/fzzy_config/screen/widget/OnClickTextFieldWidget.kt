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

import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomTextWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomWidget
import me.fzzyhmstrs.fzzy_config.simpleId
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.RenderUtil
import me.fzzyhmstrs.fzzy_config.util.RenderUtil.drawNineSlice
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.AbstractTextWidget
import net.minecraft.client.input.KeyCodes
import org.lwjgl.glfw.GLFW
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * A button widget that masquerades as a text field widget. The text within is not editable.
 * @param textSupplier [Supplier]&lt;String&gt; - supplier of the message the "text field" displays
 * @param onClick [Consumer]&lt;OnClickTextFieldWidget&gt; - action to take when the button is pressed
 * @author fzzyhmstrs
 * @since 0.2.0, now uses AbstractTextWidget 0.6.3
 */
//client
class OnClickTextFieldWidget(private val textSupplier: Supplier<String>, private val onClick: OnInteractAction)
    :
    CustomTextWidget( 0, 0, 110, 20, FcText.EMPTY, MinecraftClient.getInstance().textRenderer)
{

    private val textures: TextureProvider = TextureSet("widget/text_field".fcId(), "widget/text_field".fcId(), "widget/text_field_highlighted".fcId())

    override fun renderText(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        context.drawNineSlice(textures.get(this.active, this.isSelected), x, y, width, height, alpha)
        val text = FcText.literal(textSupplier.get())
        val i = getWidth() - 8
        val j = textRenderer.getWidth(text)
        val k = x + 4
        val l = y + (getHeight() - textRenderer.fontHeight + 1) / 2
        val orderedText = if (j > i) FcText.trim(text, i, textRenderer) else text.asOrderedText()
        context.drawTextWithShadow(textRenderer, orderedText, k, l, textColor)
        if (j > i) {
            tooltip = Tooltip.of(text)
        }
    }

    override fun onPress(event: CustomWidget.MouseEvent): Boolean {
        onClick.interact(this, false, 0, 0, 0)
        return true
    }

    override fun onKey(event: CustomWidget.KeyEvent): Boolean {
        return if (!this.isFocused || isNavigation(event.key())) {
            false
        } else {
            if(event.isEnterOrSpace())
                onClick.interact(this, false, event.key(), event.scancode(), event.modifiers())
            else
                onClick.interact(this, true, event.key(), event.scancode(), event.modifiers())
            return true
        }
    }

    private fun isNavigation(keyCode: Int): Boolean {
        return keyCode == GLFW.GLFW_KEY_TAB
                || keyCode == GLFW.GLFW_KEY_RIGHT
                || keyCode == GLFW.GLFW_KEY_LEFT
                || keyCode == GLFW.GLFW_KEY_DOWN
                || keyCode == GLFW.GLFW_KEY_UP
                || keyCode == GLFW.GLFW_KEY_LEFT_SHIFT
                || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT
    }

    /**
     * Called when the text field widget is interacted with
     *
     * SAM: [interact] - callback with the widget as context, as well as optional keyboard input + flag if kb input is present (isKeyboard == true) or just dummies (isKeyboard == false)
     * @author fzzyhmstrs
     * @since 0.3.0
     */
    @FunctionalInterface
    fun interface OnInteractAction {
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