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

import com.mojang.blaze3d.systems.RenderSystem
import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.RenderUtil.drawTex
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.navigation.GuiNavigationType
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.input.KeyCodes
import net.minecraft.client.sound.SoundManager
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.MathHelper
import org.lwjgl.glfw.GLFW
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * A vertically oriented SliderWidget, works as one would expect a sldier widget to work, but doesn't render the slider value that needs to be done separately via the [valueApplier]
 * @param wrappedValue [Supplier]&lt;Double&gt; - supplier of value to display
 * @param x Int - x position of the widget
 * @param y Int - y position of the widget
 * @param width Int - width of the widget
 * @param height Int - height of the widget
 * @param message Text - narration message for the widget; this isn't rendered
 * @param valueApplier [Consumer]&lt;Double&gt; - accepts new user inputs
 * @author fzzyhmstrs
 * @since 0.2.0
 */
//client
class VerticalSliderWidget(private val wrappedValue: Supplier<Double>, x: Int, y: Int, width: Int, height: Int, message: Text, private val valueApplier: Consumer<Double>)
    :
    ClickableWidget(x, y, width, height, message)
{
    companion object {
        private val TEXTURE = "widget/vertical_slider".fcId()
        private val TEXTURE_HIGHLIGHTED = "widget/vertical_slider_highlighted".fcId()
        private val HANDLE = "widget/vertical_slider_handle".fcId()
        private val HANDLE_HIGHLIGHTED = "widget/vertical_slider_handle_highlighted".fcId()
    }

    private var mouseHasBeenClicked = false
    private var sliderFocused = false
    private var value: Double = wrappedValue.get()

    private fun getTexture(): Identifier {
        return if(isFocused && !sliderFocused) {
            TEXTURE_HIGHLIGHTED
        } else
            TEXTURE
    }

    private fun getHandlerTexture(): Identifier {
        return if(isHovered || sliderFocused) {
            HANDLE_HIGHLIGHTED
        } else
            HANDLE
    }

    override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        if (wrappedValue.get() != value) {
            value = wrappedValue.get()
        }
        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        RenderSystem.enableDepthTest()
        context.drawTex(getTexture(), x, y, getWidth(), getHeight())
        context.drawTex(getHandlerTexture(), x, y + (value * (height - 8).toDouble()).toInt(), getWidth(), 8)
    }

    override fun setFocused(focused: Boolean) {
        super.setFocused(focused)
        if (!focused) {
            this.sliderFocused = false
            return
        }
        val guiNavigationType = MinecraftClient.getInstance().navigationType
        if (guiNavigationType == GuiNavigationType.MOUSE || guiNavigationType == GuiNavigationType.KEYBOARD_TAB) {
            sliderFocused = true
        }
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (KeyCodes.isToggle(keyCode)) {
            sliderFocused = !sliderFocused
            return true
        }
        if (sliderFocused) {
            val bl = keyCode == GLFW.GLFW_KEY_UP
            if (bl || keyCode == GLFW.GLFW_KEY_DOWN) {
                val f = if (bl) -1.0f else 1.0f
                this.setValue(value + (f / (height - 8).toFloat()).toDouble())
                return true
            }
        }
        return false
    }

    override fun onClick(mouseX: Double, mouseY: Double) {
        mouseHasBeenClicked = true
        setValueFromMouse(mouseY)
    }

    override fun onDrag(mouseX: Double, mouseY: Double, deltaX: Double, deltaY: Double) {
        setValueFromMouse(mouseY)
        super.onDrag(mouseX, mouseY, deltaX, deltaY)
    }

    override fun playDownSound(soundManager: SoundManager?) {
    }

    override fun onRelease(mouseX: Double, mouseY: Double) {
        if (mouseHasBeenClicked)
            super.playDownSound(MinecraftClient.getInstance().soundManager)
    }

    private fun setValueFromMouse(mouseY: Double) {
        this.setValue((mouseY - (y + 4).toDouble()) / (height - 8).toDouble())
    }

    private fun setValue(value: Double) {
        val d = this.value
        this.value = MathHelper.clamp(value, 0.0, 1.0)
        if (d != this.value) {
            valueApplier.accept(this.value)
        }
    }

    override fun getNarrationMessage(): MutableText {
        return "gui.narrate.slider".translate(message)
    }

    override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
        builder.put(NarrationPart.TITLE, this.narrationMessage as Text)
        if (active) {
            if (this.isFocused) {
                builder.put(NarrationPart.USAGE, "fc.button.slider.usage.focused".translate())
            } else {
                builder.put(NarrationPart.USAGE, "narration.slider.usage.hovered".translate())
            }
        }
    }


}