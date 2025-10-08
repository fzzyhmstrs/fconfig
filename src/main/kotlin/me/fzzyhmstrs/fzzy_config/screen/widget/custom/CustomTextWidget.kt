/*
 * Copyright (c) 2025 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.screen.widget.custom

import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.widget.AbstractTextWidget
import net.minecraft.text.Text
import org.jetbrains.annotations.ApiStatus.Internal

/**
 * Implementation of [AbstractTextWidget] that also implements [CustomWidget] for better version stability.
 * @author fzzyhmstrs
 * @since 0.7.3
 */
abstract class CustomTextWidget(x: Int, y: Int, width: Int, height: Int, message: Text, textRenderer: TextRenderer =  MinecraftClient.getInstance().textRenderer) : AbstractTextWidget(x, y, width, height, message, textRenderer), CustomWidget {

    abstract fun renderText(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float)

    final override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        renderText(context, mouseX, mouseY, deltaTicks)
    }

    open fun onPress(event: CustomWidget.MouseEvent): Boolean {
        return false
    }

    @Internal
    final override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return onMouse(CustomWidget.OnClick(mouseX, mouseY, button))
    }

    @Internal
    final override fun onClick(mouseX: Double, mouseY: Double) {
        if (ConfigApi.platform().isDev()) {
            throw IllegalStateException("CustomPressableWidget onClick method called. This is a bug in 0.7.3! See the source for information.")
        }
        FC.LOGGER.error("CustomPressableWidget onClick method called. This is a bug in 0.7.3! See the source for information.")
    }


    override fun onMouse(event: CustomWidget.MouseEvent): Boolean {
        if (!this.isSelected) {
            return false
        } else {
            if (this.isMouse(event)) {
                val bl = this.isMouseOver(event.x(), event.y())
                if (bl && this.onPress(event)) {
                    this.playDownSound(MinecraftClient.getInstance().soundManager)
                    return true
                }
            }
            return false
        }
    }

    @Internal
    final override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        return if (this.isValidClickButton(button)) {
            onMouseDrag(CustomWidget.OnDrag(mouseX, mouseY, button, deltaX, deltaY))
        } else {
            false
        }
    }

    @Internal
    final override fun onDrag(mouseX: Double, mouseY: Double, deltaX: Double, deltaY: Double) {
        if (ConfigApi.platform().isDev()) {
            throw IllegalStateException("CustomPressableWidget onDrag method called. This is a bug in 0.7.3! See the source for information.")
        }
        FC.LOGGER.error("CustomPressableWidget onDrag method called. This is a bug in 0.7.3! See the source for information.")
        super.onDrag(mouseX, mouseY, deltaX, deltaY)
    }

    @Internal
    final override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return if (this.isValidClickButton(button)) {
            onMouseRelease(CustomWidget.OnRelease(mouseX, mouseY, button))
        } else {
            false
        }
    }

    @Internal
    final override fun onRelease(mouseX: Double, mouseY: Double) {
        if (ConfigApi.platform().isDev()) {
            throw IllegalStateException("CustomPressableWidget onRelease method called. This is a bug in 0.7.3! See the source for information.")
        }
        FC.LOGGER.error("CustomPressableWidget onRelease method called. This is a bug in 0.7.3! See the source for information.")
        super.onRelease(mouseX, mouseY)
    }

    @Internal
    final override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        return onMouseScroll(CustomWidget.OnScroll(mouseX, mouseY, horizontalAmount, verticalAmount))
    }

    @Internal
    final override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        return onKey(CustomWidget.KeyEvent(keyCode, scanCode, modifiers))
    }

    override fun onKey(event: CustomWidget.KeyEvent): Boolean {
        return false
    }

    @Internal
    final override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        return onKeyRelease(CustomWidget.KeyEvent(keyCode, scanCode, modifiers))
    }

    @Internal
    final override fun charTyped(chr: Char, modifiers: Int): Boolean {
        return onChar(CustomWidget.CharEvent(chr, modifiers))
    }
}