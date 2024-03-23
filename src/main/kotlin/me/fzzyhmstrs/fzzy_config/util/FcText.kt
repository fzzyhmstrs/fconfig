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

    fun String.literal(): MutableText{
        return literal(this)
    }

    fun String.translatable(vararg args: Any): MutableText{
        return FcText.translatable(this, args)
    }

    fun Any?.translation(fallback: String): Text {
        return if(this is Translatable)
            this.translation().takeIf { it.string != this.translationKey() } ?: translatable(fallback).formatted(Formatting.DARK_RED)
        else
            translatable(fallback).formatted(Formatting.DARK_RED)
    }
    fun Any?.description(fallback: String): Text {
        return if(this is Translatable)
            this.description().takeIf { it.string != this.descriptionKey() } ?: translatable(fallback).formatted(Formatting.DARK_RED)
        else
            translatable(fallback).formatted(Formatting.DARK_RED)
    }
}