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

import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.client.input.KeyInput
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.math.MathHelper
import org.lwjgl.glfw.GLFW

internal class NavigableTextFieldWidget(private val textRenderer: TextRenderer, width: Int, height: Int, text: Text) : TextFieldWidget(textRenderer, width, height, text) {

    private val searchText = "fc.config.search".translate().formatted(Formatting.DARK_GRAY, Formatting.ITALIC)

    override fun keyPressed(input: KeyInput): Boolean {
        if (!this.isInteractable || !this.isFocused) {
            return false
        }
        if (input.isLeft && this.text.isEmpty()) {
            return false
        } else if (input.isRight && this.text.isEmpty()) {
            return false
        }
        return super.keyPressed(input)
    }

    override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.renderWidget(context, mouseX, mouseY, delta)
        if (this.text.isEmpty() && !this.isFocused) {
            val k = if (this.drawsBackground()) this.x + 4 else this.x
            val l = if (this.drawsBackground()) this.y + (this.height - 8) / 2 else this.y
            context.drawTextWithShadow(this.textRenderer, searchText, k, l, -1)
        }
    }

    private var clicked = false

    override fun onClick(mouseX: Double, mouseY: Double) {
        clicked = true
        super.onClick(mouseX, mouseY)
        clicked = false
    }

    override fun setCursor(cursor: Int, shiftKeyPressed: Boolean) {
        if (clicked) {
            this.setSelectionStart(cursor)
            if (!shiftKeyPressed) {
                this.setSelectionEnd(this.cursor)
            }
        } else {
            super.setCursor(cursor, shiftKeyPressed)
        }
    }
}