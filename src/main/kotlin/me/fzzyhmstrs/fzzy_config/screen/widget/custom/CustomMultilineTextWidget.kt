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

import me.fzzyhmstrs.fzzy_config.config.ConfigAction
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.TextAlignment
import net.minecraft.client.gui.ActiveTextCollector
import net.minecraft.client.gui.ActiveTextCollector.ClickableStyleFinder
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.GuiGraphicsExtractor.HoveredTextEffects
import net.minecraft.util.FormattedCharSequence
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.Component
import net.minecraft.locale.Language
import net.minecraft.util.Util
import net.minecraft.util.Mth
import kotlin.math.min

/**
 * Multiline text widget that aligns its text to the left and has a resizable width.
 * @param message [Text] the text to split and display
 * @param lineHeight space between lines, default 9 (MC standard)
 * @param topPadding pixels of padding above the text. Will add to the widget's height.
 * @param bottomPadding pixels of padding below the text. Will add to the widget's height.
 * @param leftPadding pixels of padding on the left side of the text.
 * @param rightPadding pixels of padding on the right side of the text.
 * @author fzzyhmstrs
 * @since 0.6.0, left and right padding 0.6.5, handles click and hover events 0.7.0
 */
class CustomMultilineTextWidget @JvmOverloads constructor(message: Component, private val lineHeight: Int = 9, private val topPadding: Int = 0, private val bottomPadding: Int = topPadding, private val leftPadding: Int = 0, private val rightPadding: Int = 0) :
    CustomTextWidget(0, 0, 50, 0, message, Minecraft.getInstance().font) {

    private val cache = Util.singleKeyCache<Key, MultilineText> { _ ->
        MultilineText.create(font, getMessage(), width)
    }

    private var alignRight = false

    override fun visitLines(textConsumer: ActiveTextCollector) {
        val text = cache.getValue(getKey(width - leftPadding - rightPadding))
        text.draw(if (alignRight) TextAlignment.RIGHT else TextAlignment.LEFT, this.width - leftPadding - rightPadding, x + leftPadding,  y + topPadding, lineHeight, textConsumer)
    }

    override fun getHeight(): Int {
        val text = cache.getValue(getKey(width - leftPadding - rightPadding))
        val lines = text.count()
        return (lines * lineHeight) + topPadding + bottomPadding
    }

    override fun setHeight(height: Int) {
    }

    fun alignRight(): CustomMultilineTextWidget {
        this.alignRight = true
        return this
    }

    fun maxWidthNeeded(): Int {
        val text = cache.getValue(getKey(width - leftPadding - rightPadding))
        return text.maxWidth
    }

    private fun getKey(width: Int): Key {
        return Key(message, width)
    }

    override fun onPress(event: CustomWidget.MouseEvent): Boolean {
        val clickHandler = ClickableStyleFinder(this.font, event.x().toInt(), event.y().toInt())
        this.visitLines(clickHandler)
        val style = clickHandler.result()
        if (style != null && style.clickEvent != null) {
            val ce = style.clickEvent ?: return false
            return ConfigAction.handleClickEvent(ce)
        }
        return false
    }

    private data class Key(val message: Component, val width: Int)

    interface MultilineText {

        fun draw(alignment: TextAlignment, width: Int, x: Int, y: Int, lineHeight: Int, consumer: ActiveTextCollector)

        fun count(): Int

        val maxWidth: Int

        class Line(val text: FormattedCharSequence, val width: Int) {
            fun text(): FormattedCharSequence {
                return this.text
            }
            fun width(): Int {
                return this.width
            }
        }

        companion object {

            fun create(renderer: Font, vararg texts: Component): MultilineText {
                return create(renderer, Int.MAX_VALUE, Int.MAX_VALUE, *texts)
            }

            fun create(renderer: Font, maxWidth: Int, vararg texts: Component): MultilineText {
                return create(renderer, maxWidth, Int.MAX_VALUE, *texts)
            }

            fun create(renderer: Font, text: Component, maxWidth: Int): MultilineText {
                return create(renderer, maxWidth, Int.MAX_VALUE, text)
            }

            fun create(renderer: Font, maxWidth: Int, maxLines: Int, vararg texts: Component): MultilineText {
                return if (texts.isEmpty()) EMPTY else object : MultilineText {

                    private var lines: List<Line> = listOf()
                    private var language: Language? = null

                    private fun getLines(): List<Line> {
                        val language: Language = Language.getInstance()
                        if (language === this.language) {
                            return lines
                        } else {
                            this.language = language
                            val list: MutableList<FormattedCharSequence> = mutableListOf()
                            for (text in texts) {
                                list.addAll(renderer.split(text, maxWidth))
                            }
                            val iterator: Iterator<FormattedCharSequence> = list.subList(0, min(list.size, maxLines)).iterator()

                            val list2: MutableList<Line> = mutableListOf()
                            while (iterator.hasNext()) {
                                val orderedText: FormattedCharSequence = iterator.next()
                                list2.add(Line(orderedText, renderer.width(orderedText)))
                            }
                            lines = list2
                            return lines
                        }
                    }

                    override fun draw(
                        alignment: TextAlignment,
                        width: Int,
                        x: Int,
                        y: Int,
                        lineHeight: Int,
                        consumer: ActiveTextCollector
                    ) {
                        var i: Int = y
                        val var7: Iterator<Line> = getLines().iterator()
                        while (var7.hasNext()) {
                            val line: Line = var7.next()
                            if (alignment == TextAlignment.RIGHT) {
                                val xx = x + width
                                consumer.accept(alignment, xx, i, line.text())
                            } else {
                                consumer.accept(alignment, x, i, line.text())
                            }
                            i += lineHeight
                        }
                    }

                    override fun count(): Int {
                        return getLines().size
                    }

                    override val maxWidth: Int
                        get() = min(maxWidth, getLines().stream().mapToInt { obj: Line -> obj.width() }.max().orElse(0))
                }
            }

            val EMPTY: MultilineText = object : MultilineText {

                override fun draw(alignment: TextAlignment, width: Int, x: Int, y: Int, lineHeight: Int, consumer: ActiveTextCollector) {}

                override fun count(): Int = 0

                override val maxWidth: Int
                    get() = 0
            }
        }
    }
}