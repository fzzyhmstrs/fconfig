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
 * @since 0.2.0, added prefixes 0.6.0
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
     * translation key of this Translatable's inline prefix text. Unlike descriptions, which are usually displayed in-tooltips, prefixes are displayed inline in the config screen itself
     *
     * Both descriptions and prefixes are narrated like "hints" (tooltips), so usage of either and/or both is equivalent for narration except that prefixes are narrated before descriptions.
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    fun prefixKey(): String {
        return ""
    }

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
     * The translated [Text] description from the [descriptionKey]. Falls back to an empty string so no tooltip is rendered.
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    fun prefix(fallback: String? = null): MutableText {
        return FcText.translatableWithFallback(prefixKey(), fallback ?: "")
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
    /**
     * Whether this Translatable has a valid prefix
     * @return Boolean - If there is a valid prefix.
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    fun hasPrefix(): Boolean {
        return I18n.hasTranslation(prefixKey())
    }

    /**
     * A translation result from a [Translatable] instance. This is generated internally, but is passed into many builder methods for config GUIs. Think of it, as the name implies, as the result of Fzzy Config generating a translation set for the relevant element.
     * @param name [Text] the title of the element, such as "Particle Count"
     * @param desc [Text], nullable. the tooltip description. Null means no description is present.
     * @param prefix [Text], nullable. the inline prefix text of a config entry. Null means no prefix.
     * @author fzzyhmstrs
     * @since 0.6.0, data class since 0.6.5
     */
    data class Result(val name: Text, val desc: Text? = null, val prefix: Text? = null) {

        companion object {
            val EMPTY = Result(FcText.empty(), null, null)
        }
    }
}