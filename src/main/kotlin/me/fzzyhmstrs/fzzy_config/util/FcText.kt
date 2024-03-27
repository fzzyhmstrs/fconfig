package me.fzzyhmstrs.fzzy_config.util

import com.mojang.brigadier.Message
import me.fzzyhmstrs.fzzy_config.api.Translatable
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.minecraft.util.math.ChunkPos
import java.util.*

object FcText {

    fun translatable(key: String, vararg args: Any): MutableText {
        return Text.translatable(key, *args)
    }

    fun translatableWithFallback(key: String, fallback: String?, vararg args: Any): MutableText {
        return Text.translatableWithFallback(key,fallback, *args)
    }

    fun stringified(key: String, vararg args: Any): MutableText {
        return Text.stringifiedTranslatable(key, *args)
    }

    fun literal(text: String): MutableText {
        return Text.literal(text)
    }

    fun empty(): MutableText {
        return Text.empty()
    }

    fun appended(baseText: MutableText, vararg appended: Text): MutableText {
        appended.forEach {
            baseText.append(it)
        }
        return baseText
    }

    fun Identifier.text(): Text{
        return Text.of(this)
    }

    fun UUID.text(): Text{
        return Text.of(this)
    }

    fun Date.text(): Text{
        return Text.of(this)
    }

    fun Message.text(): Text{
        return Text.of(this)
    }

    fun ChunkPos.text(): Text{
        return Text.of(this)
    }

    fun String.lit(): MutableText{
        return literal(this)
    }

    fun String.translate(vararg args: Any): MutableText{
        return translatable(this, args)
    }

    fun Any?.translation(fallback: String): MutableText {
        return if(this is Translatable)
            this.translation().takeIf { it.string != this.translationKey() } ?: translatable(fallback).formatted(Formatting.ITALIC)
        else
            translatable(fallback).formatted(Formatting.ITALIC)
    }
    fun Any?.transLit(literalFallback: String): MutableText {
        return if(this is Translatable)
            this.translation().takeIf { it.string != this.translationKey() } ?: literal(literalFallback).formatted(Formatting.ITALIC)
        else
            literal(literalFallback).formatted(Formatting.ITALIC)
    }
    fun Any?.description(fallback: String): Text {
        return if(this is Translatable)
            this.description().takeIf { it.string != this.descriptionKey() } ?: translatable(fallback).formatted(Formatting.ITALIC)
        else
            translatable(fallback).formatted(Formatting.ITALIC)
    }
    fun Any?.descLit(literalFallback: String): Text {
        return if(this is Translatable)
            this.description().takeIf { it.string != this.descriptionKey() } ?: literal(literalFallback).formatted(Formatting.ITALIC)
        else
            literal(literalFallback).formatted(Formatting.ITALIC)
    }
}