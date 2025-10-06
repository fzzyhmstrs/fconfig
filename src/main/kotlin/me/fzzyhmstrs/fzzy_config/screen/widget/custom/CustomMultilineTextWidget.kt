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
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.widget.AbstractTextWidget
import net.minecraft.text.OrderedText
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Language
import net.minecraft.util.Util
import net.minecraft.util.math.MathHelper
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
class CustomMultilineTextWidget @JvmOverloads constructor(message: Text, private val lineHeight: Int = 9, private val topPadding: Int = 0, private val bottomPadding: Int = topPadding, private val leftPadding: Int = 0, private val rightPadding: Int = 0) :
    AbstractTextWidget(0, 0, 50, 0, message, MinecraftClient.getInstance().textRenderer) {

    private val cache = Util.cachedMapper<Key, MultilineText> { _ ->
        MultilineText.create(textRenderer, getMessage(), width)
    }

    private var alignRight = false

    override fun renderButton(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val text = cache.map(getKey(width - leftPadding - rightPadding))
        text.drawWithShadow(context, alignRight, width - leftPadding - rightPadding, x + leftPadding,  y + topPadding, lineHeight, textColor)
        if (!isMouseOver(mouseX.toDouble(), mouseY.toDouble())) return
        val d = mouseX - this.x - leftPadding
        val dd = mouseY - this.y - topPadding
        val line = dd / lineHeight
        val style = text.getStyleAt(MinecraftClient.getInstance(), line, d.toDouble()) ?: return
        context.drawHoverEvent(textRenderer, style, mouseX, mouseY)
    }

    override fun getHeight(): Int {
        val text = cache.map(getKey(width - leftPadding - rightPadding))
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
        val text = cache.map(getKey(width - leftPadding - rightPadding))
        return text.maxWidth
    }

    private fun getKey(width: Int): Key {
        return Key(message, width)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!isMouseOver(mouseX, mouseY)) return false
        val client = MinecraftClient.getInstance()
        val screen = client.currentScreen ?: return false
        val text = cache.map(getKey(width - leftPadding - rightPadding))
        val d = mouseX - this.x - leftPadding
        val dd = mouseY - this.y - topPadding
        val line = (dd / lineHeight).toInt()
        val style = text.getStyleAt(client, line, d) ?: return false
        val bl = screen.handleTextClick(style)
        if (bl) playDownSound(client.soundManager)
        return bl
    }

    private data class Key(val message: Text, val width: Int)

    interface MultilineText {

        fun drawWithShadow(context: DrawContext, alignRight: Boolean, width: Int, x: Int, y: Int, lineHeight: Int, color: Int)

        fun draw(context: DrawContext, alignRight: Boolean, width: Int, x: Int, y: Int, lineHeight: Int, color: Int): Int

        fun count(): Int

        fun getStyleAt(client: MinecraftClient, line: Int, width: Double): Style?

        val maxWidth: Int

        class Line(val text: OrderedText, val width: Int) {
            fun text(): OrderedText {
                return this.text
            }
            fun width(): Int {
                return this.width
            }
        }

        companion object {

            fun create(renderer: TextRenderer, vararg texts: Text): MultilineText {
                return create(renderer, Int.MAX_VALUE, Int.MAX_VALUE, *texts)
            }

            fun create(renderer: TextRenderer, maxWidth: Int, vararg texts: Text): MultilineText {
                return create(renderer, maxWidth, Int.MAX_VALUE, *texts)
            }

            fun create(renderer: TextRenderer, text: Text, maxWidth: Int): MultilineText {
                return create(renderer, maxWidth, Int.MAX_VALUE, text)
            }

            fun create(renderer: TextRenderer, maxWidth: Int, maxLines: Int, vararg texts: Text): MultilineText {
                return if (texts.isEmpty()) EMPTY else object : MultilineText {

                    private var lines: List<Line> = listOf()
                    private var language: Language? = null

                    private fun getLines(): List<Line> {
                        val language: Language = Language.getInstance()
                        if (language === this.language) {
                            return lines
                        } else {
                            this.language = language
                            val list: MutableList<OrderedText> = mutableListOf()
                            for (text in texts) {
                                list.addAll(renderer.wrapLines(text, maxWidth))
                            }
                            val iterator: Iterator<OrderedText> = list.subList(0, min(list.size, maxLines)).iterator()

                            val list2: MutableList<Line> = mutableListOf()
                            while (iterator.hasNext()) {
                                val orderedText: OrderedText = iterator.next()
                                list2.add(Line(orderedText, renderer.getWidth(orderedText)))
                            }
                            lines = list2
                            return lines
                        }
                    }


                    override fun drawWithShadow(context: DrawContext, alignRight: Boolean, width: Int, x: Int, y: Int, lineHeight: Int, color: Int) {
                        var i: Int = y

                        val var7: Iterator<Line> = getLines().iterator()
                        while (var7.hasNext()) {
                            val line: Line = var7.next()
                            if (alignRight) {
                                val xx = x + width - renderer.getWidth(line.text)
                                context.drawTextWithShadow(renderer, line.text, xx, i, color)
                            } else {
                                context.drawTextWithShadow(renderer, line.text, x, i, color)
                            }
                            i += lineHeight
                        }
                    }

                    override fun draw(context: DrawContext, alignRight: Boolean, width: Int, x: Int, y: Int, lineHeight: Int, color: Int): Int {
                        var i: Int = y

                        val var7: Iterator<Line> = getLines().iterator()
                        while (var7.hasNext()) {
                            val line: Line = var7.next()
                            if (alignRight) {
                                val xx = x + width - renderer.getWidth(line.text)
                                context.drawText(renderer, line.text, xx, i, color, false)
                            } else {
                                context.drawText(renderer, line.text, x, i, color, false)
                            }
                            i += lineHeight
                        }

                        return i
                    }

                    override fun count(): Int {
                        return getLines().size
                    }

                    override fun getStyleAt(client: MinecraftClient, line: Int, width: Double): Style? {
                        if (line > (count() - 1) || line < 0) return null
                        val l = getLines()[line]
                        return client.textRenderer.textHandler.getStyleAt(l.text(), MathHelper.floor(width))
                    }

                    override val maxWidth: Int
                        get() = min(maxWidth, getLines().stream().mapToInt { obj: Line -> obj.width() }.max().orElse(0))
                }
            }

            val EMPTY: MultilineText = object : MultilineText {

                override fun drawWithShadow(context: DrawContext, alignRight: Boolean, width: Int, x: Int, y: Int, lineHeight: Int, color: Int) {
                }

                override fun draw(context: DrawContext, alignRight: Boolean, width: Int, x: Int, y: Int, lineHeight: Int, color: Int): Int {
                    return y
                }

                override fun count(): Int = 0

                override fun getStyleAt(client: MinecraftClient, line: Int, width: Double): Style? {
                    return null
                }

                override val maxWidth: Int
                    get() = 0
            }
        }
    }
}