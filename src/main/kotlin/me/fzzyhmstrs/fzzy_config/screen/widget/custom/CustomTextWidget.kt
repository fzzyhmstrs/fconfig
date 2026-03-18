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
import me.fzzyhmstrs.fzzy_config.util.FcText
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.TextAlignment
import net.minecraft.client.gui.ActiveTextCollector
import net.minecraft.client.gui.Font
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.AbstractStringWidget
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component
import org.jetbrains.annotations.ApiStatus.Internal

/**
 * Implementation of [AbstractTextWidget] that also implements [CustomWidget] for better version stability.
 * @author fzzyhmstrs
 * @since 0.7.3
 */
abstract class CustomTextWidget(x: Int, y: Int, width: Int, height: Int, message: Component, textRenderer: Font =  Minecraft.getInstance().font) : AbstractStringWidget(x, y, width, height, message, textRenderer), CustomWidget {

    override fun visitLines(textConsumer: ActiveTextCollector) {
        val text = message
        val i = getWidth()
        val j = font.width(text)
        val k = this.x + this.getWidth() / 2
        val l = y + (getHeight() - font.lineHeight) / 2
        val orderedText = if (j > i) FcText.trim(text, i, font) else text.visualOrderText
        textConsumer.accept(TextAlignment.CENTER, k, l, orderedText)
    }

    open fun renderCustom(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, deltaTicks: Float) {}

    final override fun extractWidgetRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        renderCustom(context, mouseX, mouseY, deltaTicks)
        super.extractWidgetRenderState(context, mouseX, mouseY, deltaTicks)
    }

    open fun onPress(event: CustomWidget.MouseEvent): Boolean {
        return false
    }

    @Internal
    final override fun mouseClicked(click: MouseButtonEvent, doubled: Boolean): Boolean {
        return onMouse(CustomWidget.OnClick(click, doubled))
    }

    @Internal
    final override fun onClick(click: MouseButtonEvent, doubled: Boolean) {
        if (ConfigApi.platform().isDev()) {
            throw IllegalStateException("CustomPressableWidget onClick method called. This is a bug in 0.7.3! See the source for information.")
        }
        FC.LOGGER.error("CustomPressableWidget onClick method called. This is a bug in 0.7.3! See the source for information.")
    }


    override fun onMouse(event: CustomWidget.MouseEvent): Boolean {
        if (!this.isActive) {
            return false
        } else {
            if (this.isMouse(event)) {
                val bl = this.isMouseOver(event.x(), event.y())
                if (bl && this.onPress(event)) {
                    this.playDownSound(Minecraft.getInstance().soundManager)
                    return true
                }
            }
            return false
        }
    }

    @Internal
    final override fun mouseDragged(click: MouseButtonEvent, offsetX: Double, offsetY: Double): Boolean {
        return if (this.isValidClickButton(click.buttonInfo())) {
            onMouseDrag(CustomWidget.OnDrag(click, offsetX, offsetY))
        } else {
            false
        }
    }

    @Internal
    final override fun onDrag(click: MouseButtonEvent, offsetX: Double, offsetY: Double) {
        if (ConfigApi.platform().isDev()) {
            throw IllegalStateException("CustomPressableWidget onDrag method called. This is a bug in 0.7.3! See the source for information.")
        }
        FC.LOGGER.error("CustomPressableWidget onDrag method called. This is a bug in 0.7.3! See the source for information.")
        super.onDrag(click, offsetX, offsetY)
    }

    @Internal
    final override fun mouseReleased(click: MouseButtonEvent): Boolean {
        return if (this.isValidClickButton(click.buttonInfo())) {
            onMouseRelease(CustomWidget.OnRelease(click))
        } else {
            false
        }
    }

    @Internal
    final override fun onRelease(click: MouseButtonEvent) {
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
    final override fun keyPressed(input: KeyEvent): Boolean {
        return onKey(CustomWidget.KeyEvent(input))
    }

    override fun onKey(event: CustomWidget.KeyEvent): Boolean {
        return false
    }

    @Internal
    final override fun keyReleased(input: KeyEvent): Boolean {
        return onKeyRelease(CustomWidget.KeyEvent(input))
    }

    @Internal
    final override fun charTyped(input: CharacterEvent): Boolean {
        return onChar(CustomWidget.CharEvent(input))
    }
}