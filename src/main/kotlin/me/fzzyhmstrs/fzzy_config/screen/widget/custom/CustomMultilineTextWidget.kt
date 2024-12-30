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

import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.MultilineText
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.widget.AbstractTextWidget
import net.minecraft.text.Text
import net.minecraft.util.Util

//TODO
class CustomMultilineTextWidget(message: Text, private val lineHeight: Int = 9, private val topPadding: Int = 0, private val bottomPadding: Int = topPadding) :
    AbstractTextWidget(0, 0, 50, 0, message, MinecraftClient.getInstance().textRenderer) {

    private val cache = Util.cachedMapper<Key, MultilineText> { _ ->
        MultilineText.create(textRenderer, getMessage(), width)
    }

    override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val text = cache.map(getKey())
        text.drawWithShadow(context, x, y + topPadding, lineHeight, textColor)
    }

    override fun getHeight(): Int {
        return (cache.map(getKey()).count() * lineHeight) + topPadding + bottomPadding
    }

    override fun setHeight(height: Int) {
    }

    private fun getKey(): Key {
        return Key(message, width)
    }

    private data class Key(val message: Text, val width: Int)
}