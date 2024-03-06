package me.fzzyhmstrs.fzzy_config.api

import me.fzzyhmstrs.fzzy_config.util.FcText
import net.minecraft.text.Text

interface StringTranslatable {
    fun translationKey(): String
    fun descriptionKey(): String
    fun translation(): Text{
        return FcText.translatable(translationKey())
    }
    fun description(): Text{
        return FcText.translatable(descriptionKey())
    }
}