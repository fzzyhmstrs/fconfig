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
import net.minecraft.client.gui.Font
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import net.minecraft.util.Mth
import org.lwjgl.glfw.GLFW

internal class NavigableTextFieldWidget(private val textRenderer: Font, width: Int, height: Int, text: Component) : EditBox(textRenderer, width, height, text) {

    private val searchText = "fc.config.search".translate().withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)

    override fun keyPressed(input: KeyEvent): Boolean {
        if (!this.isActive || !this.isFocused) {
            return false
        }
        if (input.isLeft && this.value.isEmpty()) {
            return false
        } else if (input.isRight && this.value.isEmpty()) {
            return false
        }
        return super.keyPressed(input)
    }

    override fun extractWidgetRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.extractWidgetRenderState(context, mouseX, mouseY, delta)
        if (this.value.isEmpty() && !this.isFocused) {
            val k = if (this.isBordered) this.x + 4 else this.x
            val l = if (this.isBordered) this.y + (this.height - 8) / 2 else this.y
            context.text(this.textRenderer, searchText, k, l, -1)
        }
    }

    private var clicked = false

    override fun onClick(click: MouseButtonEvent, doubled: Boolean) {
        clicked = true
        super.onClick(click, doubled)
        clicked = false
    }

    override fun moveCursorTo(cursor: Int, shiftKeyPressed: Boolean) {
        if (clicked) {
            this.cursorPosition = cursor
            if (!shiftKeyPressed) {
                this.setHighlightPos(this.cursorPosition)
            }
        } else {
            super.moveCursorTo(cursor, shiftKeyPressed)
        }
    }
}