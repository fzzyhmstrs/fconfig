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

import net.minecraft.text.MutableText

/**
 * SubInterface of [Translatable] for use with Enums
 *
 * If a non-Enum extends this, the game will crash
 *
 * The default [translationKey] will be in the form `"<prefix>.CONSTANT"`. Example: `TestEnum.TEST`
 *
 * The default [descriptionKey] will be in the form `"<prefix>.CONSTANT.desc"`. Example: `TestEnum.TEST.desc`
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@JvmDefaultWithCompatibility
interface EnumTranslatable: Translatable {
    /**
     * Defines the prefix of the translation/description key
     *
     * Typical implementation would be a translation key version of your config identifier. Example: `"my.config"`
     *
     * Defaults to the classes Simple Name. Example: `"TestEnum"`
     * @return String the lang file prefix before the enum constant name
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun prefix(): String {
        return this::class.java.simpleName
    }
    /**
     * Override of [translationKey][me.fzzyhmstrs.fzzy_config.util.Translatable.translationKey] that utilized the [prefix] and enum constant name
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    override fun translationKey(): String {
        return "${prefix()}.${(this as Enum<*>).name}"
    }
    /**
     * Override of [descriptionKey][me.fzzyhmstrs.fzzy_config.util.Translatable.descriptionKey] that utilized the [prefix] and enum constant name
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    override fun descriptionKey(): String {
        return "${prefix()}.${(this as Enum<*>).name}.desc"
    }

    /**
     * Override of [translation][me.fzzyhmstrs.fzzy_config.util.Translatable.translation] that falls back to the enum constant name. Example `"TEST"`
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    override fun translation(fallback: String?): MutableText{
        return FcText.translatableWithFallback(translationKey(),fallback ?: (this as Enum<*>).name)
    }
}