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
 * @sample me.fzzyhmstrs.fzzy_config.examples.texts
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
        return Text.translatableWithFallback(key,fallback, *args)
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
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun Identifier.text(): Text{
        return Text.of(this)
    }
    /**
     * Extension function for converting UUIDs into Texts in a kotlin style
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun UUID.text(): Text{
        return Text.of(this)
    }
    /**
     * Extension function for converting Dates into Texts in a kotlin style
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun Date.text(): Text{
        return Text.of(this)
    }
    /**
     * Extension function for converting Messages into Texts in a kotlin style
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun Message.text(): Text{
        return Text.of(this)
    }
    /**
     * Extension function for converting ChunkPos into Texts in a kotlin style
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun ChunkPos.text(): Text{
        return Text.of(this)
    }

    /**
     * Extension function converts a string into a literal Text representing it
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun String.lit(): MutableText{
        return literal(this)
    }
    /**
     * Extension function uses the receiver String as a translation key to convert it into Text
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun String.translate(vararg args: Any): MutableText{
        return translatable(this, *args)
    }

    /**
     * Translates anything. If the thing is [Translatable], it will use the built in translation, otherwise it will translate the fallback key
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun Any?.translation(fallback: String): MutableText {
        return if(this is Translatable)
            this.translation().takeIf { it.string != this.translationKey() } ?: translatable(fallback).formatted(Formatting.ITALIC)
        else
            translatable(fallback).formatted(Formatting.ITALIC)
    }
    /**
     * Translates anything. If the thing is [Translatable], it will use the built in translation, otherwise it will use the fallback literally
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun Any?.transLit(literalFallback: String = ""): MutableText {
        return if(this is Translatable)
            this.translation().takeIf { it.string != this.translationKey() } ?: literal(literalFallback).formatted(Formatting.ITALIC)
        else if (literalFallback != "")
            literal(literalFallback).formatted(Formatting.ITALIC)
        else
            literal(this.toString())
    }
    /**
     * Describes anything (In enchantment description style, or for tooltips, for example). If the thing is [Translatable], it will use the built in description, otherwise it will translate the fallback key
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun Any?.description(fallback: String): Text {
        return if(this is Translatable)
            this.description().takeIf { it.string != this.descriptionKey() } ?: translatable(fallback).formatted(Formatting.ITALIC)
        else
            translatable(fallback).formatted(Formatting.ITALIC)
    }
    /**
     * Describes anything (In enchantment description style, or for tooltips, for example). If the thing is [Translatable], it will use the built in description, otherwise it will use the fallback literally
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun Any?.descLit(literalFallback: String = ""): Text {
        return if(this is Translatable)
            this.description().takeIf { it.string != this.descriptionKey() } ?: literal(literalFallback).formatted(Formatting.ITALIC)
        else if(literalFallback != "")
            literal(literalFallback).formatted(Formatting.ITALIC)
        else
            empty()
    }
}
