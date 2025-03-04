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

/**
 * Multiline text widget that aligns its text to the left and has a resizable width.
 * @param message [Text] the text to split and display
 * @param lineHeight space between lines, default 9 (MC standard)
 * @param topPadding pixels of padding above the text. Will add to the widget's height.
 * @param bottomPadding pixels of padding below the text. Will add to the widget's height.
 * @param leftPadding pixels of padding on the left side of the text.
 * @param rightPadding pixels of padding on the right side of the text.
 * @author fzzyhmstrs
 * @since 0.6.0, left and right padding 0.6.5
 */
class CustomMultilineTextWidget @JvmOverloads constructor(message: Text, private val lineHeight: Int = 9, private val topPadding: Int = 0, private val bottomPadding: Int = topPadding, private val leftPadding: Int = 0, private val rightPadding: Int = 0) :
    AbstractTextWidget(0, 0, 50, 0, message, MinecraftClient.getInstance().textRenderer) {

    private val cache = Util.cachedMapper<Key, MultilineText> { _ ->
        MultilineText.create(textRenderer, getMessage(), width)
    }

    override fun renderButton(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val text = cache.map(getKey(width - leftPadding - rightPadding))
        text.drawWithShadow(context, x + leftPadding, y + topPadding, lineHeight, textColor)
    }

    override fun getHeight(): Int {
        return (cache.map(getKey(width - leftPadding - rightPadding)).count() * lineHeight) + topPadding + bottomPadding
    }

    private fun getKey(width: Int): Key {
        return Key(message, width)
    }

    private data class Key(val message: Text, val width: Int)
}