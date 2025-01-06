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
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.lwjgl.glfw.GLFW

internal class NavigableTextFieldWidget(private val textRenderer: TextRenderer, width: Int, height: Int, text: Text) : TextFieldWidget(textRenderer, 0, 0, width, height, text) {

    private val searchText = "fc.config.search".translate().formatted(Formatting.DARK_GRAY, Formatting.ITALIC)

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
         if (!this.isNarratable || !this.isFocused) {
            return false
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT && this.text.isEmpty()) {
            return false
        } else if (keyCode == GLFW.GLFW_KEY_RIGHT && this.text.isEmpty()) {
            return false
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun renderButton(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.renderButton(context, mouseX, mouseY, delta)
        if (this.text.isEmpty() && !this.isFocused) {
            val k = if (this.innerWidth != this.width) this.x + 4 else this.x
            val l = if (this.innerWidth != this.width) this.y + (this.height - 8) / 2 else this.y
            context.drawTextWithShadow(this.textRenderer, searchText, k, l, -1)
        }
    }
}