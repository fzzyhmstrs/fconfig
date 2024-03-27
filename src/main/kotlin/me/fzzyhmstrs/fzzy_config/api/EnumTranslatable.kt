package me.fzzyhmstrs.fzzy_config.api

import me.fzzyhmstrs.fzzy_config.util.FcText
import net.minecraft.text.MutableText
import net.minecraft.text.Text

/**
 * SubInterface of [Translatable] for use with Enums
 *
 * If a non-Enum extends this, the game will crash
 *
 * The [translationKey] will be in the form `"<prefix>.CONSTANT"`. Example: `TestEnum.TEST`
 *
 * The [descriptionKey] will be in the form `"<prefix>.CONSTANT.desc"`. Example: `TestEnum.TEST.desc`
 * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedShorthands.TestEnum]
 * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedShorthands.lang]
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
     * Override of [translationKey][me.fzzyhmstrs.fzzy_config.api.Translatable.translationKey] that utilized the [prefix] and enum constant name
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    override fun translationKey(): String {
        return "${prefix()}.${(this as Enum<*>).name}"
    }
    /**
     * Override of [descriptionKey][me.fzzyhmstrs.fzzy_config.api.Translatable.descriptionKey] that utilized the [prefix] and enum constant name
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    override fun descriptionKey(): String {
        return "${prefix()}.${(this as Enum<*>).name}.desc"
    }

    /**
     * Override of [translation][me.fzzyhmstrs.fzzy_config.api.Translatable.translation] that falls back to the enum constant name. Example `"TEST"`
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    override fun translation(): MutableText{
        return FcText.translatableWithFallback(translationKey(),(this as Enum<*>).name)
    }
}