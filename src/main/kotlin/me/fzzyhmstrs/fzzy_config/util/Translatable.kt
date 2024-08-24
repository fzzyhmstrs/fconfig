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

import net.minecraft.client.resource.language.I18n
import net.minecraft.text.MutableText
import net.minecraft.text.Text

/**
 * Classes that implement [Translatable] can be automatically utilized by many FzzyConfig systems for generating translatable text in-game
 * @sample me.fzzyhmstrs.fzzy_config.examples.TranslatableExample.translatable
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@JvmDefaultWithCompatibility
interface Translatable {
    /**
     * translation key of this Translatable. the "name" in-game
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun translationKey(): String
    /**
     * translation key of this Translatable's description. the "description" in-game, the descriptions Enchantment Descriptions adds to enchantment tooltips are a good example.
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun descriptionKey(): String
    /**
     * The translated [Text] name from the [translationKey]. Falls back to the implementing classes Simple Name (non-translated)
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun translation(fallback: String? = null): MutableText {
        return FcText.translatableWithFallback(translationKey(), fallback ?: this::class.java.simpleName.split(FcText.regex).joinToString(" ").trimStart())
    }
    /**
     * The translated [Text] description from the [descriptionKey]. Falls back to an empty string so no tooltip is rendered.
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun description(fallback: String? = null): MutableText {
        return FcText.translatableWithFallback(descriptionKey(), fallback ?: "")
    }

    /**
     * Whether this Translatable has a valid translation
     * @return Boolean - If there is a valid translation.
     * @author fzzyhmstrs
     * @since 0.2.8
     */
    fun hasTranslation(): Boolean {
        return I18n.hasTranslation(translationKey())
    }
    /**
     * Whether this Translatable has a valid description
     * @return Boolean - If there is a valid description.
     * @author fzzyhmstrs
     * @since 0.2.8
     */
    fun hasDescription(): Boolean {
        return I18n.hasTranslation(descriptionKey())
    }
}