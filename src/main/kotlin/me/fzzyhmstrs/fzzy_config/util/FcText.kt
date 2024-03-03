package me.fzzyhmstrs.fzzy_config.util

import com.mojang.brigadier.Message
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.ChunkPos
import java.util.*

object FcText {

    fun translatable(key: String, vararg args: Any): MutableText {
        return Text.translatable(key, *args)
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
}