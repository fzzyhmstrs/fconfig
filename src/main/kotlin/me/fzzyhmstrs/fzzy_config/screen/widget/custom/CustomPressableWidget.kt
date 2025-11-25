/*
 * Copyright (c) 2024 Fzzyhmstrs
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
import me.fzzyhmstrs.fzzy_config.screen.widget.TextureProvider
import me.fzzyhmstrs.fzzy_config.screen.widget.TextureSet
import me.fzzyhmstrs.fzzy_config.screen.widget.TooltipChild
import me.fzzyhmstrs.fzzy_config.simpleId
import me.fzzyhmstrs.fzzy_config.util.PortingUtils
import me.fzzyhmstrs.fzzy_config.util.RenderUtil.drawNineSlice
import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import net.minecraft.text.Text
import net.minecraft.util.math.MathHelper
import org.jetbrains.annotations.ApiStatus.Internal

/**
 * Custom implementation of a [PressableWidget][net.minecraft.client.gui.widget.PressableWidget] that provides several improvements to rendering.
 * @param x X position of the widget
 * @param y Y position of the widget
 * @param width Width in pixels of the widget
 * @param height Height in pixels of the widget
 * @param message [Text] label to draw over the center of the button.
 * @author fzzyhmstrs
 * @since 0.5.?
 */
open class CustomPressableWidget(x: Int, y: Int, width: Int, height: Int, message: Text) : ClickableWidget(x, y, width, height, message), CustomWidget, TooltipChild {

    protected open val textures: TextureProvider = DEFAULT_TEXTURES

    /**
     * Action invoked whenever the button is clicked on or activated with Enter.
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    open fun onPress() {}

    /**
     * Custom foreground rendering for the widget. By default, will render the label.
     * @param context [DrawContext]
     * @param x X position for rendering. This is *not* necessarily the widgets x position. Subclasses can shift this position, to, for example, shift the label over to make room for an icon.
     * @param y Y position for rendering. This is *not* necessarily the widgets y position. Subclasses can shift this position, to, for example, shift the label up to fit something underneath it.
     * @param width render width. Not necessarily widget width. If you modify [x], it's recommended to counter-modify this to keep rendered space consistent with the background
     * @param height render height. Not necessarily widget height. If you modify [y], it's recommended to counter-modify this to keep rendered space consistent with the background
     * @param mouseX current horizontal screen position of the mouse
     * @param mouseY current vertical screen position of the mouse
     * @param delta screen frame delta
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    open fun renderCustom(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, delta: Float) {
        val minecraftClient = MinecraftClient.getInstance()
        val i = if (this.active) 16777215 else 10526880
        this.drawMessage(context, minecraftClient.textRenderer, x, y, width, height, PortingUtils.fullAlpha(i))
    }

    /**
     * Custom background rendering for the widget. By default, will render the standard MC button texture set.
     * @param context [DrawContext]
     * @param x X position for rendering. This is *not* necessarily the widgets x position.
     * @param y Y position for rendering. This is *not* necessarily the widgets y position.
     * @param width render width. Not necessarily widget width. If you modify [x], it's recommended to counter-modify this to keep rendered space consistent with the foreground as applicable
     * @param height render height. Not necessarily widget height. If you modify [y], it's recommended to counter-modify this to keep rendered space consistent with the foreground as applicable
     * @param mouseX current horizontal screen position of the mouse
     * @param mouseY current vertical screen position of the mouse
     * @param delta screen frame delta
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    open fun renderBackground(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, delta: Float) {
        context.drawNineSlice(textures.get(active, this.isSelected), x, y, width, height, this.alpha)
    }

    override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, x, y, width, height, mouseX, mouseY, delta)
        renderCustom(context, x, y, width, height, mouseX, mouseY, delta)
    }

    /**
     * Draws the widgets message with standard edge padding and positioning.
     * @param context [DrawContext]
     * @param textRenderer [TextRenderer]
     * @param x X position for rendering. This is *not* necessarily the widgets x position. Subclasses can shift this position, to, for example, shift the label over to make room for an icon.
     * @param y Y position for rendering. This is *not* necessarily the widgets y position. Subclasses can shift this position, to, for example, shift the label up to fit something underneath it.
     * @param width render width. Not necessarily widget width. If you modify [x], it's recommended to counter-modify this to keep rendered space consistent with the background
     * @param height render height. Not necessarily widget height. If you modify [y], it's recommended to counter-modify this to keep rendered space consistent with the background
     * @param color Int representation of text color
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    open fun drawMessage(context: DrawContext, textRenderer: TextRenderer, x: Int, y: Int, width: Int, height: Int, color: Int) {
        this.drawScrollableText(context, textRenderer, x, y, width, height, 2, color)
    }

    /**
     * Draws the positioned text of this widget with supplied edge padding and positioning.
     * @param context [DrawContext]
     * @param textRenderer [TextRenderer]
     * @param x X position for rendering. This is *not* necessarily the widgets x position. Subclasses can shift this position, to, for example, shift the label over to make room for an icon.
     * @param y Y position for rendering. This is *not* necessarily the widgets y position. Subclasses can shift this position, to, for example, shift the label up to fit something underneath it.
     * @param width render width. Not necessarily widget width. If you modify [x], it's recommended to counter-modify this to keep rendered space consistent with the background
     * @param height render height. Not necessarily widget height. If you modify [y], it's recommended to counter-modify this to keep rendered space consistent with the background
     * @param xMargin pixels of padding left and right for the text cutoff
     * @param color Int representation of text color
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    protected open fun drawScrollableText(context: DrawContext, textRenderer: TextRenderer, x: Int, y: Int, width: Int, height: Int, xMargin: Int, color: Int) {
        val i = x + xMargin
        val j = x + width - xMargin
        val drawer = context.getHoverListener(this, DrawContext.HoverType.NONE)
        val text = if (color == -1) this.getMessage() else this.getMessage().copy().styled { s -> s.withColor(color) }
        drawer.text(text, i, j, y, y + height)
    }

    @Internal
    @Deprecated("Will be marked final in 0.8.0. Use onMouse instead")
    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        return onMouse(CustomWidget.OnClick(click, doubled))
    }

    @Internal
    @Deprecated("Won't work as intended, CustomPressableWidget calls onPress directly. Will be marked final in 0.8.0. Use onMouse instead.")
    override fun onClick(click: Click, doubled: Boolean) {
        if (ConfigApi.platform().isDev()) {
            throw IllegalStateException("CustomPressableWidget onClick method called. This is a bug in 0.7.3! See the source for information.")
        }
        FC.LOGGER.error("CustomPressableWidget onClick method called. This is a bug in 0.7.3! See the source for information.")
        this.onPress()
    }


    override fun onMouse(event: CustomWidget.MouseEvent): Boolean {
        if (!this.isInteractable) {
            return false
        } else {
            if (this.isMouse(event)) {
                val bl = this.isMouseOver(event.x(), event.y())
                if (bl) {
                    this.playDownSound(MinecraftClient.getInstance().soundManager)
                    this.onPress()
                    return true
                }
            }
            return false
        }
    }

    @Internal
    @Deprecated("Will be marked final in 0.8.0. Use onMouseDrag instead")
    override fun mouseDragged(click: Click, offsetX: Double, offsetY: Double): Boolean {
        return if (this.isValidClickButton(click.buttonInfo())) {
            onMouseDrag(CustomWidget.OnDrag(click, offsetX, offsetY))
        } else {
            false
        }
    }

    @Internal
    @Deprecated("Won't work as intended, CustomPressableWidget calls onMouseDrag instead. Will be marked final in 0.8.0. Use onMouseDrag instead.")
    override fun onDrag(click: Click, offsetX: Double, offsetY: Double) {
        if (ConfigApi.platform().isDev()) {
            throw IllegalStateException("CustomPressableWidget onDrag method called. This is a bug in 0.7.3! See the source for information.")
        }
        FC.LOGGER.error("CustomPressableWidget onDrag method called. This is a bug in 0.7.3! See the source for information.")
        super.onDrag(click, offsetX, offsetY)
    }

    @Internal
    @Deprecated("Will be marked final in 0.8.0. Use onMouseRelease instead")
    override fun mouseReleased(click: Click): Boolean {
        return if (this.isValidClickButton(click.buttonInfo())) {
            onMouseRelease(CustomWidget.OnRelease(click))
        } else {
            false
        }
    }

    @Internal
    @Deprecated("Won't work as intended, CustomPressableWidget calls onMouseRelease instead. Will be marked final in 0.8.0. Use onMouseRelease instead.")
    override fun onRelease(click: Click) {
        if (ConfigApi.platform().isDev()) {
            throw IllegalStateException("CustomPressableWidget onRelease method called. This is a bug in 0.7.3! See the source for information.")
        }
        FC.LOGGER.error("CustomPressableWidget onRelease method called. This is a bug in 0.7.3! See the source for information.")
        super.onRelease(click)
    }

    @Internal
    @Deprecated("Will be marked final in 0.8.0. Use onMouseScroll instead")
    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        return onMouseScroll(CustomWidget.OnScroll(mouseX, mouseY, horizontalAmount, verticalAmount))
    }

    @Internal
    @Deprecated("Will be marked final in 0.8.0. Use onKey instead")
    override fun keyPressed(input: KeyInput): Boolean {
        return onKey(CustomWidget.KeyEvent(input))
    }

    override fun onKey(event: CustomWidget.KeyEvent): Boolean {
        if (!this.active || !this.visible) {
            return false
        } else if (event.isEnterOrSpace()) {
            this.playDownSound(MinecraftClient.getInstance().soundManager)
            this.onPress()
            return true
        } else {
            return false
        }
    }

    @Internal
    @Deprecated("Will be marked final in 0.8.0. Use onKeyRelease instead")
    override fun keyReleased(input: KeyInput): Boolean {
        return onKeyRelease(CustomWidget.KeyEvent(input))
    }

    @Internal
    @Deprecated("Will be marked final in 0.8.0. Use onChar instead")
    override fun charTyped(input: CharInput): Boolean {
        return onChar(CustomWidget.CharEvent(input))
    }

    @Internal
    override fun appendClickableNarrations(builder: NarrationMessageBuilder?) {
        appendDefaultNarrations(builder)
    }

    companion object {
        @JvmStatic
        protected val tex =  "widget/button".simpleId()
        @JvmStatic
        protected val disabled = "widget/button_disabled".simpleId()
        @JvmStatic
        protected val highlighted = "widget/button_highlighted".simpleId()

        /**
         * The default texture set of the widget. The same textures used by MC buttons.
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        val DEFAULT_TEXTURES = TextureSet(tex, disabled, highlighted)
    }
}