/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.util

import com.mojang.brigadier.Message
import net.minecraft.client.gui.Font
import net.minecraft.client.resources.language.I18n
import net.minecraft.network.chat.CommonComponents
import net.minecraft.ChatFormatting
import net.minecraft.resources.Identifier
import net.minecraft.locale.Language
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FormattedText
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import net.minecraft.util.FormattedCharSequence
import net.minecraft.world.level.ChunkPos
import java.lang.StringBuilder
import java.util.*
import java.util.function.Supplier
import kotlin.collections.lastIndex

/**
 * Various text utilities and wrappers for making kotlin minecraft modding more text-expressive
 * @sample me.fzzyhmstrs.fzzy_config.examples.ExampleTexts.texts
 * @see me.fzzyhmstrs.fzzy_config.util.Translatable
 * @author fzzyhmstrs
 * @since 0.2.0
 */
object FcText {

    internal val regex = Regex("(?=\\p{Lu}\\p{Ll})")
    internal val EMPTY: Component = empty()

    /**
     * Wrapper method around Text.translatable. A backwards compatibility holdover from porting older versions
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun translatable(key: String, vararg args: Any): MutableComponent {
        return Component.translatable(key, *args)
    }
    /**
     * Wrapper method around Text.translatableWithFallback. A backwards compatibility holdover from porting older versions
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun translatableWithFallback(key: String, fallback: String?, vararg args: Any): MutableComponent {
        return Component.translatableWithFallback(key, fallback, *args)
    }
    /**
     * Wrapper method around Text.stringified. A backwards compatibility holdover from porting older versions
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun stringified(key: String, vararg args: Any): MutableComponent {
        return Component.translatableEscape(key, *args)
    }
    /**
     * Wrapper method around Text.literal. A backwards compatibility holdover from porting older versions
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun literal(text: String): MutableComponent {
        return Component.literal(text)
    }
    /**
     * Wrapper method around Text.empty. A backwards compatibility holdover from porting older versions
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun empty(): MutableComponent {
        return Component.empty()
    }
    /**
     * Appends multiple texts to a base text
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun appended(baseText: MutableComponent, vararg appended: Component): MutableComponent {
        appended.forEach {
            baseText.append(it)
        }
        return baseText
    }

    /**
     * Extension function for converting Identifiers into Texts in a kotlin style
     * @receiver [Identifier] to convert to text
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun Identifier.text(): Component {
        return Component.translationArg(this)
    }
    /**
     * Extension function for converting UUIDs into Texts in a kotlin style
     * @receiver [UUID] to convert to text
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun UUID.text(): Component {
        return Component.translationArg(this)
    }
    /**
     * Extension function for converting Dates into Texts in a kotlin style
     * @receiver [Date] to convert to text
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun Date.text(): Component {
        return Component.translationArg(this)
    }
    /**
     * Extension function for converting Messages into Texts in a kotlin style
     * @receiver [Message] to convert to text
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun Message.text(): Component {
        return Component.translationArg(this)
    }
    /**
     * Extension function for converting ChunkPos into Texts in a kotlin style
     * @receiver [ChunkPos] to convert to text
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun ChunkPos.text(): Component {
        return Component.translationArg(this)
    }

    /**
     * Extension function converts a string into a literal Text representing it
     * @receiver String - will be interpreted literally
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun String.lit(): MutableComponent {
        return literal(this)
    }
    /**
     * Extension function uses the receiver String as a translation key to convert it into Text
     * @receiver String - should be a translation key
     * @param args vararg Anything - the arguments to use in the translation. Should be valid translatable argument types (Primitive, string, or other Text instance)
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun String.translate(vararg args: Any): MutableComponent {
        return translatable(this, *args)
    }

    /**
     * Translates anything. If the thing is [Translatable], it will use the built-in translation, otherwise it will translate the fallback key
     * @receiver Anything, null or not. [Translatable] will provide its translation.
     * @param fallback String - translation key for the fallback translation
     * @return [MutableText] translation based on the receivers translation, or the fallback translation
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun Any?.translation(fallback: String): MutableComponent {
        return if(this is Translatable)
            if (this.hasTranslation()) {
                this.translation()
            } else {
                translatable(fallback).withStyle(ChatFormatting.ITALIC)
            }
        else
            translatable(fallback).withStyle(ChatFormatting.ITALIC)
    }
    /**
     * Translates anything. If the thing is [Translatable], it will use the built-in translation, otherwise it will use the fallback literally
     * @receiver Anything, null or not. [Translatable] will provide its translation.
     * @param literalFallback String - the fallback text, used literally, not as a translation key
     * @return [MutableText] translation based on the receivers translation, or the fallback
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun Any?.transLit(literalFallback: String = ""): MutableComponent {
        return if(this is Translatable)
            if (this.hasTranslation()) {
                this.translation()
            } else {
                literal(literalFallback).withStyle(ChatFormatting.ITALIC)
            }
        else if (literalFallback != "")
            literal(literalFallback).withStyle(ChatFormatting.ITALIC)
        else
            literal(this.toString())
    }
    /**
     * Translates anything. If the thing is [Translatable], it will use the built in translation, otherwise it will use the fallback literally
     * @receiver Anything, null or not. [Translatable] will provide its translation.
     * @param fallbackSupplier Supplier&lt;String&gt; - the fallback text supplier, used literally, not as a translation key
     * @return [MutableText] translation based on the receivers translation, or the fallback
     * @author fzzyhmstrs
     * @since 0.4.2
     */
    fun Any?.transSupplied(fallbackSupplier: Supplier<String>): MutableComponent {
        return if(this is Translatable) {
            if (this.hasTranslation()) {
                this.translation()
            } else {
                literal(fallbackSupplier.get()).withStyle(ChatFormatting.ITALIC)
            }
        } else {
            val fallback = fallbackSupplier.get()
            if (fallback != "")
                literal(fallback).withStyle(ChatFormatting.ITALIC)
            else
                empty()
        }
    }
    /**
     * Describes anything (In enchantment description style, or for tooltips, for example). If the thing is [Translatable], it will use the built in description, otherwise it will translate the fallback key
     * @receiver Anything, null or not. [Translatable] will provide its description.
     * @param fallback String - translation key for the fallback description
     * @return [Text] description based on the receivers description translation, or the fallback translation
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun Any?.description(fallback: String): Component {
        return if(this is Translatable)
            if (this.hasDescription()) {
                this.description()
            } else {
                translatable(fallback).withStyle(ChatFormatting.ITALIC)
            }
        else
            translatable(fallback).withStyle(ChatFormatting.ITALIC)
    }
    /**
     * Describes anything (In enchantment description style, or for tooltips, for example). If the thing is [Translatable], it will use the built-in description, otherwise it will use the fallback literally
     * @receiver Anything, null or not. [Translatable] will provide its description.
     * @param literalFallback String - the fallback text, used literally, not as a translation key
     * @return [Text] description based on the receivers description translation, or the fallback
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun Any?.descLit(literalFallback: String = ""): Component {
        return if(this is Translatable) {
            if (this.hasDescription()) {
                this.description()
            } else {
                literal(literalFallback).withStyle(ChatFormatting.ITALIC)
            }
        } else if(literalFallback != "")
            literal(literalFallback).withStyle(ChatFormatting.ITALIC)
        else
            empty()
    }
    /**
     * Describes anything (In enchantment description style, or for tooltips, for example). If the thing is [Translatable], it will use the built-in description, otherwise it will use the fallback literally
     * @receiver Anything, null or not. [Translatable] will provide its description.
     * @param fallbackSupplier Supplier&lt;String&gt; - the fallback text supplier, used literally, not as a translation key
     * @return [MutableText] description based on the receivers description translation, or the fallback
     * @author fzzyhmstrs
     * @since 0.4.2
     */
    fun Any?.descSupplied(fallbackSupplier: Supplier<String>): MutableComponent {
        return if(this is Translatable) {
            if (this.hasDescription()) {
                this.description()
            } else {
                literal(fallbackSupplier.get()).withStyle(ChatFormatting.ITALIC)
            }
        } else {
            val fallback = fallbackSupplier.get()
            if (fallback != "")
                literal(fallback).withStyle(ChatFormatting.ITALIC)
            else
                empty()
        }
    }

    internal fun Any?.prefix(fallback: String): Component? {
        if(this is Translatable) {
            if (this.hasPrefix()) {
                return this.prefix()
            }
        }
        return if (I18n.exists(fallback))
            translatable(fallback).withStyle(ChatFormatting.ITALIC)
        else
            null
    }
    internal fun Any?.prefixLit(literalFallback: String = ""): Component? {
        if(this is Translatable) {
            if (this.hasPrefix()) {
                return this.prefix(literalFallback)
            }
        }
        return if(literalFallback != "")
            literal(literalFallback).withStyle(ChatFormatting.ITALIC)
        else
            null
    }
    internal fun Any?.prefixSupplied(fallbackSupplier: Supplier<String>): MutableComponent? {
        if(this is Translatable) {
            if (this.hasPrefix()) {
                return this.prefix(fallbackSupplier.get())
            }
        }
        val fallback = fallbackSupplier.get()
        return if (fallback != "")
            literal(fallback).withStyle(ChatFormatting.ITALIC)
        else
            null
    }

    /**
     * Adds a Run Command click action to a [MutableText]
     * @param command String representation of the command *with* preceding `/`
     * @return The mutable text styled with a Run Command click event
     * @author fzzyhmstrs
     * @since 0.4.2
     */
    fun MutableComponent.command(command: String): MutableComponent {
        return this.withStyle { s -> s.withClickEvent(ClickEvent.RunCommand(command)) }
    }

    /**
     * Adds a Show Text hover action (a tooltip) to a [MutableText]
     * @param tooltip [Text] - the tooltip message to show on hover
     * @return The mutable text styled with a tooltip
     * @author fzzyhmstrs
     * @since 0.4.2
     */
    fun MutableComponent.tooltip(tooltip: Component): MutableComponent {
        return this.withStyle { s -> s.withHoverEvent(HoverEvent.ShowText(tooltip)) }
    }

    /**
     * Underlines the receiver text
     * @return this text with underline applied
     * @author fzzyhmstrs
     * @since 0.4.2
     */
    fun MutableComponent.underline(): MutableComponent {
        return this.withStyle(ChatFormatting.UNDERLINE)
    }
    /**
     * Bolds the receiver text
     * @return this text with bolding applied
     * @author fzzyhmstrs
     * @since 0.4.2
     */
    fun MutableComponent.bold(): MutableComponent {
        return this.withStyle(ChatFormatting.BOLD)
    }
    /**
     * Italicizes the receiver text
     * @return this text with italics applied
     * @author fzzyhmstrs
     * @since 0.4.2
     */
    fun MutableComponent.italic(): MutableComponent {
        return this.withStyle(ChatFormatting.ITALIC)
    }
    /**
     * Strikes through the receiver text
     * @return this text with strikethrough applied
     * @author fzzyhmstrs
     * @since 0.4.2
     */
    fun MutableComponent.strikethrough(): MutableComponent {
        return this.withStyle(ChatFormatting.STRIKETHROUGH)
    }
    /**
     * Applies an RGB color to the receiver text
     * @return this text with the specified color applied
     * @author fzzyhmstrs
     * @since 0.4.2
     */
    fun MutableComponent.colored(color: Int): MutableComponent {
        return this.withStyle { s -> s.withColor(color) }
    }

    /**
     * Whether this Text object represents an empty string. This can have any type of inner content, as long as it resolves to an empty string
     * @return The mutable text styled with a Run Command click event
     * @author fzzyhmstrs
     * @since 0.?.?
     */
    fun Component.isEmpty(): Boolean {
        return this.string.isEmpty()
    }

    /**
     * Whether this Text object does not represent an empty string. This can have any type of inner content, as long as it doesn't resolve to an empty string
     * @return The mutable text styled with a Run Command click event
     * @author fzzyhmstrs
     * @since 0.?.?
     */
    fun Component.isNotEmpty(): Boolean {
        return this.string.isNotEmpty()
    }

    /**
     * Concatenates the provided texts into one text with each piece separated by a line break
     * @param texts List of text to concatenate
     * @return The mutable text styled with a Run Command click event
     * @author fzzyhmstrs
     * @since 0.7.5
     */
    fun toLinebreakText(texts: List<Component>): MutableComponent {
        if (texts.isEmpty()) return empty()
        var text: MutableComponent? = null
        for (message in texts) {
            if (text == null) {
                text = message.copy()
            } else {
                text.append(literal("\n")).append(message)
            }
        }
        return text ?: empty()
    }

    /**
     * Concatenates the provided texts into one text with each piece separated by a line break
     * @param text Text to trim
     * @param width maximum allowed width
     * @param textRenderer [TextRenderer] text renderer instance to use
     * @return The input text trimmed if needed. If it needs trimming, it will be trimmed to fit ellipses at the end ("Trimmed tex...")
     * @author fzzyhmstrs
     * @since 0.7.5
     */
    fun trim(text: Component, width: Int, textRenderer: Font): FormattedCharSequence {
        val stringVisitable = textRenderer.substrByWidth(text, width - textRenderer.width(CommonComponents.ELLIPSIS))
        return Language.getInstance().getVisualOrder(FormattedText.composite(stringVisitable, CommonComponents.ELLIPSIS))
    }

    fun concat(str: String, str2: String): String {
        val builder = StringBuilder(str)
        builder.append(str2)
        return builder.toString()
    }

    fun concat(str: String, str2: String, str3: String): String {
        val builder = StringBuilder(str)
        builder.append(str2)
        builder.append(str3)
        return builder.toString()
    }

    fun concat(str: String, str2: String, str3: String, str4: String): String {
        val builder = StringBuilder(str)
        builder.append(str2)
        builder.append(str3)
        builder.append(str4)
        return builder.toString()
    }

    /**
     * Capitalizes a string and converts it to a literal text object
     * @receiver The string to capitalize
     * @return A [Text] object with the input string capitalized
     * @author fzzyhmstrs
     * @since 0.7.5
     */
    fun String.capital(): Component {
        return this.lowercase().replace('_', ' ').split(' ').joinToString(" ") { it.lowercase(); it.replaceFirstChar { c -> c.uppercase() } }.lit()
    }

    /**
     * Concatenates the provided texts into one text with each piece separated by a line break
     * @param texts List of text to concatenate
     * @param separator Text to concatenate between text fragments provided in [texts]
     * @return A single text object with the provided texts concatenated and separated by the separator
     * @author fzzyhmstrs
     * @since 0.7.5
     */
    fun joinToText(texts: List<Component>, separator: Component): Component {
        if (texts.isEmpty()) return empty()
        if (texts.size == 1) return texts[0]
        var t = texts[0].copy()
        for (i in 1..texts.lastIndex) {
            t = t.append(separator)
            t = t.append(texts[i])
        }
        return t
    }

    /**
     * Concatenates the provided texts into one text as a comma-separated "or" list ("a, b, c, or d")
     * @param list List of text to concatenate
     * @return Text object representing an "or list"
     * @author fzzyhmstrs
     * @since 0.7.5
     */
    fun orList(list: List<Component>): Component {
        return orList(list, 0)
    }


    private fun orList(list: List<Component>, currentIndex: Int): Component {
        if (list.isEmpty()) return empty()
        return when (currentIndex) {
            (list.size - 1) -> {
                list[currentIndex]
            }
            (list.size - 2) -> {
                translatable("fzzy_config.text.or_2", list[currentIndex], orList(list, currentIndex + 1))
            }
            else -> {
                translatable("fzzy_config.text.or_3", list[currentIndex], orList(list, currentIndex + 1))
            }
        }
    }

    /**
     * Concatenates the provided texts into one text as a comma-separated "and" list ("a, b, c, and d")
     * @param list List of text to concatenate
     * @return Text object representing an "and list"
     * @author fzzyhmstrs
     * @since 0.7.5
     */
    fun andList(list: List<Component>): Component {
        return andList(list, 0)
    }


    private fun andList(list: List<Component>, currentIndex: Int): Component {
        if (list.isEmpty()) return empty()
        return when (currentIndex) {
            (list.size - 1) -> {
                list[currentIndex]
            }
            (list.size - 2) -> {
                translatable("fzzy_config.text.and_2", list[currentIndex], andList(list, currentIndex + 1))
            }
            else -> {
                translatable("fzzy_config.text.or_3", list[currentIndex], andList(list, currentIndex + 1))
            }
        }
    }

    /**
     * Creates a string representation of a tooltip (or other list of ordered text), generally used to supply narration
     * @param tt List of [OrderedText] to convert
     * @return String representation of the text list, separated with periods
     * @author fzzyhmstrs
     * @since 0.7.5
     */
    fun createTooltipString(tt: List<FormattedCharSequence>): String {
        val builder = StringBuilder()
        for (tip in tt) {
            tip.accept { _, _, codepoint ->
                builder.appendCodePoint(codepoint)
                true
            }
            builder.append(". ")
        }
        return builder.toString()
    }
}