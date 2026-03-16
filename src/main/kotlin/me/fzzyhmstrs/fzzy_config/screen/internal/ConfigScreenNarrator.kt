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
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.narration.NarrationThunk
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.gui.narration.NarratedElementType
import net.minecraft.network.chat.Component
import net.minecraft.util.StringDecomposer
import java.util.function.Consumer

internal class ConfigScreenNarrator(vararg narrateOnceStrings: String) {

    private val narrateOnce: Set<String> = narrateOnceStrings.map { it.lowercase() }.toSet()
    private val narrations: MutableMap<PartIndex, Message> = Maps.newTreeMap(
        Comparator.comparing { partIndex: PartIndex -> partIndex.part }
            .thenComparing { partIndex: PartIndex -> partIndex.depth })

    private var currentMessageIndex: Int = 0
    private var seenOnce: MutableSet<String> = hashSetOf()

    private companion object {

        private fun printNarration(narration: NarrationThunk<*>): String {
            val builder = StringBuilder()
            narration.getText(builder::append)
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
    fun buildNarrations(builderConsumer: Consumer<NarrationElementOutput>) {
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
            private val seen: MutableSet<String> = hashSetOf()

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
                message.narration.getText(consumer)
                message.used = true
            }
        }
        return stringBuilder.toString()
    }

    @Environment(EnvType.CLIENT)
    private class Message {

        var narration: NarrationThunk<*> = NarrationThunk.EMPTY
        var index: Int = -1
        var used: Boolean = false

        fun setNarration(index: Int, narration: NarrationThunk<*>): Message {
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

    @Environment(EnvType.CLIENT)
    inner class MessageBuilder internal constructor(private val depth: Int) : NarrationElementOutput {

        override fun add(part: NarratedElementType, narration: NarrationThunk<*>) {
            (this@ConfigScreenNarrator.narrations.computeIfAbsent(PartIndex(part, this.depth)) { _ -> Message() })
                .setNarration(this@ConfigScreenNarrator.currentMessageIndex, narration)//.also { FC.DEVLOG.info("Added message via ${if(MinecraftClient.getInstance().navigationType.isKeyboard) "keyboard" else "mouse"} navigation: $it") }
        }

        override fun add(part: NarratedElementType, text: Component) {
            this.add(part, NarrationThunk.from(StringDecomposer.getPlainText(text)))
        }

        override fun add(part: NarratedElementType, vararg texts: Component) {
            val collector = StringBuilder()
            for (text in texts) {
                collector.append(StringDecomposer.getPlainText(text))
                collector.append(" ")
            }
            add(part, NarrationThunk.from(collector.toString()))
        }

        override fun nest(): NarrationElementOutput {
            return this@ConfigScreenNarrator.MessageBuilder(this.depth + 1)
        }
    }

    private data class PartIndex(val part: NarratedElementType, val depth: Int)
}