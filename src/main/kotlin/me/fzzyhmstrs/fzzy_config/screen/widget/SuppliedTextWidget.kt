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

import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomTextWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomWidget
import me.fzzyhmstrs.fzzy_config.util.FcText
import net.minecraft.client.gui.TextAlignment
import net.minecraft.client.gui.ActiveTextCollector
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.gui.narration.NarratedElementType
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.network.chat.Component
import java.util.function.Supplier
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * A [CustomTextWidget] that renders text from a supplier of text, not a static text instance
 * @param messageSupplier [Supplier]&lt;[Text]&gt; - supplies text to this widget
 * @param textRenderer [TextRenderer] - textRenderer instance
 * @param width Int - width of the widget in pixels
 * @param height Int - height of the widget in pixels
 * @author fzzyhmstrs
 * @since 0.3.1, removed align:direction: methods and now implements TooltipChild in 0.6.0, uses java.util.Supplier instead of googles in 0.6.8 and deprecates constructors with google Supplier. Google supplier constructors removed 0.7.0.
 */
class SuppliedTextWidget(private val messageSupplier: Supplier<Component>, textRenderer: Font, width: Int, height: Int): CustomTextWidget(0, 0, width, height, messageSupplier.get(), textRenderer), TooltipChild {

    constructor(messageSupplier: Supplier<Component>, textRenderer: Font): this(messageSupplier, textRenderer, textRenderer.width(messageSupplier.get().visualOrderText), textRenderer.lineHeight)

    private var horizontalAlignment = 0.5f
    private var overflowTooltip: Supplier<Component>? = null

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
    fun supplyTooltipOnOverflow(tooltipText: Supplier<Component>): SuppliedTextWidget {
        overflowTooltip = tooltipText
        return this
    }

    override fun visitLines(textConsumer: ActiveTextCollector) {
        val text = messageSupplier.get()
        val i = getWidth()
        val j = font.width(text)
        val k = x + (horizontalAlignment * (max(i - j, 0)).toFloat()).roundToInt()
        val l = y + (getHeight() - font.lineHeight) / 2
        val orderedText = if (j > i) FcText.trim(text, i, font) else text.visualOrderText
        textConsumer.accept(TextAlignment.LEFT, k, l, orderedText)
    }

    override fun onPress(event: CustomWidget.MouseEvent): Boolean {
        return false
    }

    override fun provideTooltipLines(mouseX: Int, mouseY: Int, parentSelected: Boolean, keyboardFocused: Boolean): List<Component> {
        if (!((parentSelected && isFocused) || isMouseOver(mouseX.toDouble(), mouseY.toDouble()))) return TooltipChild.EMPTY
        return overflowTooltip?.let { listOf(it.get()) }?.takeIf { font.width(it[0]) > getWidth() } ?: TooltipChild.EMPTY
    }

    override fun updateWidgetNarration(builder: NarrationElementOutput) {
        overflowTooltip?.let { builder?.add(NarratedElementType.TITLE, it.get()) }
    }

}