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
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.AbstractTextWidget
import net.minecraft.screen.ScreenTexts
import net.minecraft.text.OrderedText
import net.minecraft.text.StringVisitable
import net.minecraft.text.Text
import net.minecraft.util.Language
import java.awt.SystemColor.text
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * An [AbstractTextWidget] that renders text from a supplier of text, not a static text instance
 * @param messageSupplier [Supplier]&lt;[Text]&gt; - supplies text to this widget
 * @param textRenderer [TextRenderer] - textRenderer instance
 * @param width Int - width of the widget in pixels
 * @param height Int - height of the widget in pixels
 * @author fzzyhmstrs
 * @since 0.3.1, removed align:direction: methods and now implements TooltipChild in 0.6.0
 */
class SuppliedTextWidget(private val messageSupplier: Supplier<Text>, textRenderer: TextRenderer, width: Int, height: Int): AbstractTextWidget(0, 0, width, height, messageSupplier.get(), textRenderer), TooltipChild {

    constructor(messageSupplier: Supplier<Text>, textRenderer: TextRenderer): this(messageSupplier, textRenderer, textRenderer.getWidth(messageSupplier.get().asOrderedText()), textRenderer.fontHeight)

    private var horizontalAlignment = 0.5f
    private var overflowTooltip: Supplier<Text>? = null

    /**
     * Aligns the widget text to the alignment fraction provided
     * @param horizontalAlignment Float - fraction between 0f and 1f specifying the horizontal alignment. 0f = fully left-aligned, 1f = fully right-aligned
     * @return [SuppliedTextWidget] this widget
     * @author fzzyhmstrs
     * @since 0.3.1
     */
    fun align(horizontalAlignment: Float): SuppliedTextWidget {
        this.horizontalAlignment = horizontalAlignment
        return this
    }

    /**
     * supplies a text for tooltips if the text overflows the width of the widget.
     *
     * This can be identical to the primary text supplier, but it can be used to, for example, provide a tooltip that is split with line breaks, where the widget text is separated with commas.
     * @param tooltipText [Supplier]&lt;[Text]&gt; - the text for the screen tooltip
     * @return [SuppliedTextWidget] this widget
     * @author fzzyhmstrs
     * @since 0.3.3
     */
    fun supplyTooltipOnOverflow(tooltipText: Supplier<Text>): SuppliedTextWidget {
        overflowTooltip = tooltipText
        return this
    }

    override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val text = messageSupplier.get()
        val i = getWidth()
        val j = textRenderer.getWidth(text)
        val k = x + (horizontalAlignment * (max(i - j, 0)).toFloat()).roundToInt()
        val l = y + (getHeight() - textRenderer.fontHeight + 1) / 2
        val orderedText = if (j > i) this.trim(text, i) else text.asOrderedText()
        context.drawTextWithShadow(textRenderer, orderedText, k, l, textColor)
        if (overflowTooltip != null) {
            if (j > i) {
                tooltip = Tooltip.of(overflowTooltip?.get())
            }
        }
    }

    private fun trim(text: Text, width: Int): OrderedText? {
        val stringVisitable = textRenderer.trimToWidth(text, width - textRenderer.getWidth(ScreenTexts.ELLIPSIS))
        return Language.getInstance().reorder(StringVisitable.concat(stringVisitable, ScreenTexts.ELLIPSIS))
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return false
    }

    override fun provideTooltipLines(mouseX: Int, mouseY: Int, parentSelected: Boolean, keyboardFocused: Boolean): List<Text> {
        if (!parentSelected) return TooltipChild.EMPTY
        return overflowTooltip?.let { listOf(it.get()) }?.takeIf { textRenderer.getWidth(it[0]) > getWidth() } ?: TooltipChild.EMPTY
    }

    override fun appendClickableNarrations(builder: NarrationMessageBuilder?) {
        overflowTooltip?.let { builder?.put(NarrationPart.TITLE, it.get()) }
    }

}