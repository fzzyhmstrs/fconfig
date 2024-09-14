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
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.minecraft.util.math.ChunkPos
import java.util.*

/**
 * Various text utilities and wrappers for making kotlin minecraft modding more text-expressive
 * @sample me.fzzyhmstrs.fzzy_config.examples.ExampleTexts.texts
 * @see me.fzzyhmstrs.fzzy_config.util.Translatable
 * @author fzzyhmstrs
 * @since 0.2.0
 */
object FcText {

    internal val regex = Regex("(?=\\p{Lu})")

    /**
     * Wrapper method around Text.translatable. A backwards compatibility holdover from porting older versions
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun translatable(key: String, vararg args: Any): MutableText {
        return Text.translatable(key, *args)
    }
    /**
     * Wrapper method around Text.translatableWithFallback. A backwards compatibility holdover from porting older versions
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun translatableWithFallback(key: String, fallback: String?, vararg args: Any): MutableText {
        return Text.translatableWithFallback(key, fallback, *args)
    }
    /**
     * Wrapper method around Text.stringified. A backwards compatibility holdover from porting older versions
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun stringified(key: String, vararg args: Any): MutableText {
        return Text.stringifiedTranslatable(key, *args)
    }
    /**
     * Wrapper method around Text.literal. A backwards compatibility holdover from porting older versions
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun literal(text: String): MutableText {
        return Text.literal(text)
    }
    /**
     * Wrapper method around Text.empty. A backwards compatibility holdover from porting older versions
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun empty(): MutableText {
        return Text.empty()
    }
    /**
     * Appends multiple texts to a base text
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun appended(baseText: MutableText, vararg appended: Text): MutableText {
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
    fun Identifier.text(): Text {
        return Text.of(this)
    }
    /**
     * Extension function for converting UUIDs into Texts in a kotlin style
     * @receiver [UUID] to convert to text
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun UUID.text(): Text {
        return Text.of(this)
    }
    /**
     * Extension function for converting Dates into Texts in a kotlin style
     * @receiver [Date] to convert to text
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun Date.text(): Text {
        return Text.of(this)
    }
    /**
     * Extension function for converting Messages into Texts in a kotlin style
     * @receiver [Message] to convert to text
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun Message.text(): Text {
        return Text.of(this)
    }
    /**
     * Extension function for converting ChunkPos into Texts in a kotlin style
     * @receiver [ChunkPos] to convert to text
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun ChunkPos.text(): Text {
        return Text.of(this)
    }

    /**
     * Extension function converts a string into a literal Text representing it
     * @receiver String - will be interpreted literally
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun String.lit(): MutableText {
        return literal(this)
    }
    /**
     * Extension function uses the receiver String as a translation key to convert it into Text
     * @receiver String - should be a translation key
     * @param args vararg Anything - the arguments to use in the translation. Should be valid translatable argument types (Primitive, string, or other Text instance)
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun String.translate(vararg args: Any): MutableText {
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
    fun Any?.translation(fallback: String): MutableText {
        return if(this is Translatable)
            this.translation().takeIf { this.hasTranslation() } ?: translatable(fallback).formatted(Formatting.ITALIC)
        else
            translatable(fallback).formatted(Formatting.ITALIC)
    }
    /**
     * Translates anything. If the thing is [Translatable], it will use the built in translation, otherwise it will use the fallback literally
     * @receiver Anything, null or not. [Translatable] will provide its translation.
     * @param literalFallback String - the fallback text, used literally, not as a translation key
     * @return [MutableText] translation based on the receivers translation, or the fallback
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun Any?.transLit(literalFallback: String = ""): MutableText {
        return if(this is Translatable)
            this.translation().takeIf { this.hasTranslation() } ?: literal(literalFallback).formatted(Formatting.ITALIC)
        else if (literalFallback != "")
            literal(literalFallback).formatted(Formatting.ITALIC)
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
    fun Any?.transSupplied(fallbackSupplier: Supplier<String>): MutableText {
        return if(this is Translatable) {
            this.translation().takeIf { this.hasTranslation() } ?: literal(fallbackSupplier.get()).formatted(Formatting.ITALIC)
        } else {
            val fallback = fallbackSupplier.get()
            if (fallback != "") 
                literal(fallback).formatted(Formatting.ITALIC)
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
    fun Any?.description(fallback: String): Text {
        return if(this is Translatable)
            this.description().takeIf { this.hasDescription() } ?: translatable(fallback).formatted(Formatting.ITALIC)
        else
            translatable(fallback).formatted(Formatting.ITALIC)
    }
    /**
     * Describes anything (In enchantment description style, or for tooltips, for example). If the thing is [Translatable], it will use the built-in description, otherwise it will use the fallback literally
     * @receiver Anything, null or not. [Translatable] will provide its description.
     * @param literalFallback String - the fallback text, used literally, not as a translation key
     * @return [Text] description based on the receivers description translation, or the fallback
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun Any?.descLit(literalFallback: String = ""): Text {
        return if(this is Translatable) {
            this.description().takeIf { this.hasDescription() } ?: literal(literalFallback).formatted(Formatting.ITALIC)
        } else if(literalFallback != "")
            literal(literalFallback).formatted(Formatting.ITALIC)
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
    fun Any?.descSupplied(fallbackSupplier: Supplier<String>): MutableText {
        return if(this is Translatable) {
            this.description().takeIf { this.hasDescription() } ?: literal(fallbackSupplier.get()).formatted(Formatting.ITALIC)
        } else {
            val fallback = fallbackSupplier.get()
            if (fallback != "") 
                literal(fallback).formatted(Formatting.ITALIC)
            else
                empty()
        }
    }
}
