/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.screen.widget.internal

import com.mojang.blaze3d.systems.RenderSystem
import me.fzzyhmstrs.fzzy_config.simpleId
import me.fzzyhmstrs.fzzy_config.util.RenderUtil.drawTex
import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.input.KeyCodes
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.ColorHelper
import net.minecraft.util.math.MathHelper

abstract class CustomPressableWidget(x: Int, y: Int, width: Int, height: Int, message: Text) : ClickableWidget(x, y, width, height, message) {

    abstract fun onPress()

    open fun renderCustom(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val minecraftClient = MinecraftClient.getInstance()
        RenderSystem.enableBlend()
        RenderSystem.enableDepthTest()
        context.drawTex(get(active, this.isSelected), this.x, this.y, this.getWidth(), this.getHeight(), ColorHelper.getWhite(this.alpha))
        val i = if (this.active) 16777215 else 10526880
        this.drawMessage(context, minecraftClient.textRenderer, i or (MathHelper.ceil(this.alpha * 255.0f) shl 24))
    }

    override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderCustom(context, mouseX, mouseY, delta)
    }

    open fun drawMessage(context: DrawContext?, textRenderer: TextRenderer?, color: Int) {
        this.drawScrollableText(context, textRenderer, 2, color)
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



    private companion object {

        private val tex =  "widget/button".simpleId()
        private val disabled = "widget/button_disabled".simpleId()
        private val highlighted = "widget/button_highlighted".simpleId()

        fun get(enabled: Boolean, focused: Boolean): Identifier {
            return if (enabled) {
                if (focused) this.highlighted else this.tex
            } else {
                this.disabled
            }
        }
    }
}