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

import com.google.common.base.Supplier
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
 * An [AbstractTextWidget] that renders text from a supplier of text, not a static text instance
 * @param messageSupplier [Supplier]&lt;[Text]&gt; - supplies text to this widget
 * @param textRenderer [TextRenderer] - textRenderer instance
 * @param width Int - width of the widget in pixels
 * @param height Int - height of the widget in pixels
 * @author fzzyhmstrs
 * @since 0.3.1
 */
class SuppliedTextWidget(private val messageSupplier: Supplier<Text>, textRenderer: TextRenderer, width: Int, height: Int): AbstractTextWidget(0,0,width, height, messageSupplier.get(), textRenderer) {

    constructor(messageSupplier: Supplier<Text>, textRenderer: TextRenderer): this(messageSupplier, textRenderer, textRenderer.getWidth(messageSupplier.get().asOrderedText()), textRenderer.fontHeight)

    private var horizontalAlignment = 0.5f

    /**
     * Aligns the widget text to the alignment fraction provided
     * @param horizontalAlignment Float - fraction between 0f and 1f specifying the horizontal alignment. 0f = fully left-aligned, 1f = fully right-aligned
     * @return [SuppliedTextWidget] this widget
     * @author fzzyhmstrs
     * @since 0.3.1
     */
    private fun align(horizontalAlignment: Float): SuppliedTextWidget {
        this.horizontalAlignment = horizontalAlignment
        return this
    }

    /**
     * Aligns the widget text to the left
     * @return [SuppliedTextWidget] this widget
     * @author fzzyhmstrs
     * @since 0.3.1
     */
    fun alignLeft(): SuppliedTextWidget {
        return this.align(0.0f)
    }
    /**
     * Aligns the widget text to the center
     * @return [SuppliedTextWidget] this widget
     * @author fzzyhmstrs
     * @since 0.3.1
     */
    fun alignCenter(): SuppliedTextWidget {
        return this.align(0.5f)
    }
    /**
     * Aligns the widget text to the right
     * @return [SuppliedTextWidget] this widget
     * @author fzzyhmstrs
     * @since 0.3.1
     */
    fun alignRight(): SuppliedTextWidget {
        return this.align(1.0f)
    }

    override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val text = messageSupplier.get()
        val i = getWidth()
        val j = textRenderer.getWidth(text)
        val k = x + (horizontalAlignment * (i - j).toFloat()).roundToInt()
        val l = y + (getHeight() - textRenderer.fontHeight) / 2
        val orderedText = if (j > i) this.trim(text, i) else text.asOrderedText()
        context.drawTextWithShadow(textRenderer, orderedText, k, l, textColor)
    }

    private fun trim(text: Text, width: Int): OrderedText? {
        val stringVisitable = textRenderer.trimToWidth(text, width - textRenderer.getWidth(ScreenTexts.ELLIPSIS))
        return Language.getInstance().reorder(StringVisitable.concat(stringVisitable, ScreenTexts.ELLIPSIS))
    }
}