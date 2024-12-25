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

import com.mojang.blaze3d.systems.RenderSystem
import me.fzzyhmstrs.fzzy_config.screen.widget.TooltipChild
import me.fzzyhmstrs.fzzy_config.simpleId
import me.fzzyhmstrs.fzzy_config.util.RenderUtil.drawTex
import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ButtonTextures
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.input.KeyCodes
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.ColorHelper
import net.minecraft.util.math.MathHelper

open class CustomPressableWidget(x: Int, y: Int, width: Int, height: Int, message: Text) : ClickableWidget(x, y, width, height, message), TooltipChild {

    protected open val textures: PressableTextures = DEFAULT_TEXTURES

    open fun onPress() {}

    open fun renderCustom(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, delta: Float) {
        val minecraftClient = MinecraftClient.getInstance()
        val i = if (this.active) 16777215 else 10526880
        this.drawMessage(context, minecraftClient.textRenderer, x, y, width, height, i or (MathHelper.ceil(this.alpha * 255.0f) shl 24))
    }

    open fun renderBackground(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, delta: Float) {
        RenderSystem.enableBlend()
        RenderSystem.enableDepthTest()
        context.drawTex(textures.get(active, this.isSelected), x, y, width, height, ColorHelper.getWhite(this.alpha))
    }

    override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, x, y, width, height, mouseX, mouseY, delta)
        renderCustom(context, x, y, width, height, mouseX, mouseY, delta)
    }

    open fun drawMessage(context: DrawContext, textRenderer: TextRenderer, x: Int, y: Int, width: Int, height: Int, color: Int) {
        this.drawScrollableText(context, textRenderer, x, y, width, height, 2, color)
    }

    protected open fun drawScrollableText(context: DrawContext, textRenderer: TextRenderer, x: Int, y: Int, width: Int, height: Int, xMargin: Int, color: Int) {
        val i = x + xMargin
        val j = x + width - xMargin
        drawScrollableText(context, textRenderer, this.message, i, y, j, y + height, color)
    }

    override fun onClick(mouseX: Double, mouseY: Double) {
        this.onPress()
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (!this.active || !this.visible) {
            return false
        } else if (KeyCodes.isToggle(keyCode)) {
            this.playDownSound(MinecraftClient.getInstance().soundManager)
            this.onPress()
            return true
        } else {
            return false
        }
    }

    override fun appendClickableNarrations(builder: NarrationMessageBuilder?) {
        appendDefaultNarrations(builder)
    }

    data class PressableTextures(private val tex: Identifier, private val disabled: Identifier, private val highlighted: Identifier) {
        fun get(enabled: Boolean, focused: Boolean): Identifier {
            return if (enabled) {
                if (focused) highlighted else tex
            } else {
                disabled
            }
        }
    }

    protected companion object {
        val tex =  "widget/button".simpleId()
        val disabled = "widget/button_disabled".simpleId()
        val highlighted = "widget/button_highlighted".simpleId()

        val DEFAULT_TEXTURES = PressableTextures(tex, disabled, highlighted)
    }
}