/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.screen.widget

import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.AbstractTextWidget
import net.minecraft.screen.ScreenTexts
import net.minecraft.text.OrderedText
import net.minecraft.text.StringVisitable
import net.minecraft.text.Text
import net.minecraft.util.Language
import net.minecraft.util.math.MathHelper
import kotlin.math.roundToInt

/**
 * A [AbstractTextWidget] that can process click and hover actions on rendered text
 * @param parent Screen - this widgets parent screen
 * @param message Text - the text to render with this widget
 * @param textRenderer [TextRenderer] - textrenderer instance
 * @author fzzyhmstrs
 * @since 0.2.0
 */
class ClickableTextWidget(private val parent: Screen, message: Text, textRenderer: TextRenderer): AbstractTextWidget(0,0,textRenderer.getWidth(message.asOrderedText()), textRenderer.fontHeight, message, textRenderer) {

    private var horizontalAlignment = 0.5f

    private fun align(horizontalAlignment: Float): ClickableTextWidget {
        this.horizontalAlignment = horizontalAlignment
        return this
    }

    fun alignLeft(): ClickableTextWidget {
        return this.align(0.0f)
    }

    fun alignCenter(): ClickableTextWidget {
        return this.align(0.5f)
    }

    fun alignRight(): ClickableTextWidget {
        return this.align(1.0f)
    }

    override fun renderButton(context: DrawContext?, mouseX: Int, mouseY: Int, delta: Float) {
        val text = message
        val i = getWidth()
        val j = textRenderer.getWidth(text)
        val k = x + (horizontalAlignment * (i - j).toFloat()).roundToInt()
        val l = y + (getHeight() - textRenderer.fontHeight) / 2
        val orderedText = if (j > i) this.trim(text, i) else text.asOrderedText()
        context!!.drawTextWithShadow(textRenderer, orderedText, k, l, textColor)
    }

    private fun trim(text: Text, width: Int): OrderedText? {
        val stringVisitable = textRenderer.trimToWidth(text, width - textRenderer.getWidth(ScreenTexts.ELLIPSIS))
        return Language.getInstance().reorder(StringVisitable.concat(stringVisitable, ScreenTexts.ELLIPSIS))
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!isMouseOver(mouseX, mouseY)) return false
        val d = mouseX - this.x
        val style = textRenderer.textHandler.getStyleAt(message.asOrderedText(), MathHelper.floor(d)) ?: return false
        return parent.handleTextClick(style)
    }

}