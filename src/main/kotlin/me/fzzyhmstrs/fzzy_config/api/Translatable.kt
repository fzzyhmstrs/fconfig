package me.fzzyhmstrs.fzzy_config.api

import me.fzzyhmstrs.fzzy_config.util.FcText
import net.minecraft.text.MutableText
import net.minecraft.text.Text

/**
 * Classes that implement [Translatable] can be automatically utilized by many FzzyConfig systems for generating translatable text in-game
 * @sample [me.fzzyhmstrs.fzzy_config.examples.ExampleTranslatable]
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
    fun translation(): MutableText{
        return FcText.translatableWithFallback(translationKey(),this::class.java.simpleName)
    }
    /**
     * The translated [Text] description from the [descriptionKey]. Falls back to an empty string so no tooltip is rendered.
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun description(): MutableText{
        return FcText.translatableWithFallback(descriptionKey(),"")
    }
}