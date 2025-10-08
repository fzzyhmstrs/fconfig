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
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.widget.AbstractTextWidget
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
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
    final override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        return onMouse(CustomWidget.OnClick(click, doubled))
    }

    @Internal
    final override fun onClick(click: Click, doubled: Boolean) {
        if (ConfigApi.platform().isDev()) {
            throw IllegalStateException("CustomPressableWidget onClick method called. This is a bug in 0.7.3! See the source for information.")
        }
        FC.LOGGER.error("CustomPressableWidget onClick method called. This is a bug in 0.7.3! See the source for information.")
    }


    override fun onMouse(event: CustomWidget.MouseEvent): Boolean {
        if (!this.isInteractable) {
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
    final override fun mouseDragged(click: Click, offsetX: Double, offsetY: Double): Boolean {
        return if (this.isValidClickButton(click.buttonInfo())) {
            onMouseDrag(CustomWidget.OnDrag(click, offsetX, offsetY))
        } else {
            false
        }
    }

    @Internal
    final override fun onDrag(click: Click, offsetX: Double, offsetY: Double) {
        if (ConfigApi.platform().isDev()) {
            throw IllegalStateException("CustomPressableWidget onDrag method called. This is a bug in 0.7.3! See the source for information.")
        }
        FC.LOGGER.error("CustomPressableWidget onDrag method called. This is a bug in 0.7.3! See the source for information.")
        super.onDrag(click, offsetX, offsetY)
    }

    @Internal
    final override fun mouseReleased(click: Click): Boolean {
        return if (this.isValidClickButton(click.buttonInfo())) {
            onMouseRelease(CustomWidget.OnRelease(click))
        } else {
            false
        }
    }

    @Internal
    final override fun onRelease(click: Click) {
        if (ConfigApi.platform().isDev()) {
            throw IllegalStateException("CustomPressableWidget onRelease method called. This is a bug in 0.7.3! See the source for information.")
        }
        FC.LOGGER.error("CustomPressableWidget onRelease method called. This is a bug in 0.7.3! See the source for information.")
        super.onRelease(click)
    }

    @Internal
    final override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        return onMouseScroll(CustomWidget.OnScroll(mouseX, mouseY, horizontalAmount, verticalAmount))
    }

    @Internal
    final override fun keyPressed(input: KeyInput): Boolean {
        return onKey(CustomWidget.KeyEvent(input))
    }

    override fun onKey(event: CustomWidget.KeyEvent): Boolean {
        return false
    }

    @Internal
    final override fun keyReleased(input: KeyInput): Boolean {
        return onKeyRelease(CustomWidget.KeyEvent(input))
    }

    @Internal
    final override fun charTyped(input: CharInput): Boolean {
        return onChar(CustomWidget.CharEvent(input))
    }
}