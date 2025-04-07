/*
 * Copyright (c) 2024-2025 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.screen.internal

import com.google.common.collect.Maps
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import net.minecraft.client.gui.screen.narration.Narration
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import net.minecraft.text.Text
import net.minecraft.text.TextVisitFactory
import java.util.function.Consumer

internal class ConfigScreenNarrator(vararg narrateOnceStrings: String) {

    private val narrateOnce: Set<String> = narrateOnceStrings.map { it.lowercase() }.toSet()
    private val narrations: MutableMap<PartIndex, Message> = Maps.newTreeMap(
        Comparator.comparing { partIndex: PartIndex -> partIndex.part }
            .thenComparing { partIndex: PartIndex -> partIndex.depth })

    private var currentMessageIndex: Int = 0
    private var seenOnce: MutableSet<String> = mutableSetOf()

    private companion object {

        private fun printNarration(narration: Narration<*>): String {
            val builder = StringBuilder()
            narration.forEachSentence(builder::append)
            return builder.toString()
        }
    }

    fun resetNarrateOnce() {
        seenOnce.clear()
    }

    /**
     * Creates the narration messages for the next narration using a
     * [NarrationMessageBuilder].
     *
     * @param builderConsumer a consumer that adds the narrations to a [NarrationMessageBuilder]
     */
    fun buildNarrations(builderConsumer: Consumer<NarrationMessageBuilder>) {
        currentMessageIndex++
        builderConsumer.accept(MessageBuilder(0))
    }

    /**
     * Builds a text representation of the narrations produced by the last call to
     * [buildNarrations][.buildNarrations].
     *
     * @implNote Contains all sentences in the narrations of the current narration
     * message separated by `". "`, ordered as described in
     * [NarrationMessageBuilder].
     * @return the created narrator text
     *
     * @param includeUnchanged if `true`, the text will include unchanged messages that have
     * already been included in the output of this method previously
     */
    fun buildNarratorText(includeUnchanged: Boolean): String {
        val stringBuilder = StringBuilder()
        val consumer: Consumer<String> = object : Consumer<String> {
            private var first = true
            private val seen: MutableSet<String> = mutableSetOf()

            override fun accept(string: String) {
                if (!this.first) {
                    stringBuilder.append(". ")
                }
                val lc = string.lowercase()
                if (!seen.contains(lc) && !seenOnce.contains(lc)) {
                    seen.add(lc)
                    if (narrateOnce.contains(lc))
                        seenOnce.add(lc)
                    this.first = false
                    stringBuilder.append(string)
                }
            }
        }
        //FC.DEVLOG.info("Narrations")
        narrations.forEach { (index, message) ->
            //FC.DEVLOG.info("$index > $message")
            if (message.index == this.currentMessageIndex && (includeUnchanged || !message.used)) {
                message.narration.forEachSentence(consumer)
                message.used = true
            }
        }
        return stringBuilder.toString()
    }

    private class Message {

        var narration: Narration<*> = Narration.EMPTY
        var index: Int = -1
        var used: Boolean = false

        fun setNarration(index: Int, narration: Narration<*>): Message {
            if (this.narration != narration) {
                //FC.DEVLOG.info("Non-equal narrations! old:${printNarration(this.narration)}, new:${printNarration(narration)}")
                this.narration = narration
                this.used = false
            } else if (this.index + 1 != index) {
                this.used = false
            }

            this.index = index
            return this
        }

        override fun toString(): String {
            return "Message(narration=${printNarration(narration)}, index=$index, used=$used)"
        }
    }

    inner class MessageBuilder internal constructor(private val depth: Int) : NarrationMessageBuilder {

        override fun put(part: NarrationPart, narration: Narration<*>) {
            (this@ConfigScreenNarrator.narrations.computeIfAbsent(PartIndex(part, this.depth)) { _ -> Message() })
                .setNarration(this@ConfigScreenNarrator.currentMessageIndex, narration)//.also { FC.DEVLOG.info("Added message via ${if(MinecraftClient.getInstance().navigationType.isKeyboard) "keyboard" else "mouse"} navigation: $it") }
        }

        override fun put(part: NarrationPart, text: Text) {
            this.put(part, Narration.string(TextVisitFactory.removeFormattingCodes(text)))
        }

        override fun put(part: NarrationPart, vararg texts: Text) {
            val collector = StringBuilder()
            for (text in texts) {
                collector.append(TextVisitFactory.removeFormattingCodes(text))
                collector.append(" ")
            }
            put(part, Narration.string(collector.toString()))
        }

        override fun nextMessage(): NarrationMessageBuilder {
            return this@ConfigScreenNarrator.MessageBuilder(this.depth + 1)
        }
    }

    private data class PartIndex(val part: NarrationPart, val depth: Int)
}