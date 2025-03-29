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
import java.lang.ref.SoftReference
import java.util.concurrent.ConcurrentHashMap
import java.util.function.UnaryOperator

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
     * translation key of this Translatable description. the "description" in-game, the descriptions Enchantment Descriptions adds to enchantment tooltips are a good example.
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun descriptionKey(): String
    /**
     * translation key of this Translatable inline prefix text. Unlike descriptions, which are usually displayed in-tooltips, prefixes are displayed inline in the config screen itself
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
     * @param fallback String, default null. Translation key fallback (can be a literal string too, translation will simply provide the literal once translation fails). If null, the class name will be split by uppercase characters and used (MyClassName -> My Class Name)
     * @return [MutableText] translation result
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun translation(fallback: String? = null): MutableText {
        return FcText.translatableWithFallback(translationKey(), fallback ?: this::class.java.simpleName.split(FcText.regex).joinToString(" ").trimStart())
    }
    /**
     * The translated [Text] description from the [descriptionKey]. Falls back to an empty string so no tooltip is rendered.
     * @param fallback String, default null. Translation key fallback (can be a literal string too, translation will simply provide the literal once translation fails). If null will return a blank description.
     * @return [MutableText] translation result
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun description(fallback: String? = null): MutableText {
        return FcText.translatableWithFallback(descriptionKey(), fallback ?: "")
    }
    /**
     * The translated [Text] description from the [descriptionKey]. Falls back to an empty string so no tooltip is rendered.
     * @param fallback String, default null. Translation key fallback (can be a literal string too, translation will simply provide the literal once translation fails). If null will return a blank prefix.
     * @return [MutableText] translation result
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    fun prefix(fallback: String? = null): MutableText {
        return FcText.translatableWithFallback(prefixKey(), fallback ?: "")
    }

    /**
     * The translated [Text] name from the [translationKey]. Falls back to the implementing classes Simple Name (non-translated). If no translation exists when called returns null.
     * @param fallback String, default null. Translation key fallback (can be a literal string too, translation will simply provide the literal once translation fails). If null, the class name will be split by uppercase characters and used (MyClassName -> My Class Name)
     * @return [MutableText] translation result or null if no translation is present
     * @author fzzyhmstrs
     * @since 0.6.8
     */
    fun translationOrNull(fallback: String? = null): MutableText? {
        return if (hasTranslation())
            translation(fallback)
        else
            null
    }
    /**
     * The translated [Text] description from the [descriptionKey]. Falls back to an empty string so no tooltip is rendered. If no translation exists when called returns null.
     * @param fallback String, default null. Translation key fallback (can be a literal string too, translation will simply provide the literal once translation fails). If null will return a blank description.
     * @return [MutableText] translation result or null if no translation is present
     * @author fzzyhmstrs
     * @since 0.6.8
     */
    fun descriptionOrNull(fallback: String? = null): MutableText? {
        return if(hasDescription()) description(fallback) else null
    }
    /**
     * The translated [Text] description from the [descriptionKey]. Falls back to an empty string so no tooltip is rendered. If no translation exists when called returns null.
     * @param fallback String, default null. Translation key fallback (can be a literal string too, translation will simply provide the literal once translation fails). If null will return a blank prefix.
     * @return [MutableText] translation result or null if no translation is present
     * @author fzzyhmstrs
     * @since 0.6.8
     */
    fun prefixOrNull(fallback: String? = null): MutableText? {
        return if (hasPrefix()) prefix(fallback) else null
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
     * @since 0.6.0, data class since 0.6.5, implements [Searcher.SearchContent] and [ResultProvider], and deprecated, since 0.6.8
     */
     @Deprecated("Replace with createResult, and use ResultProvider for typing. Constructor and impl will change in 0.7.0")
    data class Result(override val name: Text, override val desc: Text? = null, override val prefix: Text? = null): ResultProvider<Result>(), Searcher.SearchContent {

        @Deprecated("Use content, this is not used directly by Searcher as of 0.6.8. Scheduled for removal 0.7.0")
        override val texts: Result = this

        override val content = this

        override val skip = false

        companion object {
            val EMPTY = Result(FcText.empty(), null, null)
        }

        override fun map(nameMapper: UnaryOperator<Text>, descMapper: UnaryOperator<Text>, prefixMapper: UnaryOperator<Text>): Result {
            return Result(nameMapper.apply(name), desc?.let { descMapper.apply(it) }, prefix?.let { prefixMapper.apply(it) })
        }

        override fun mapName(nameMapper: UnaryOperator<Text>): Result {
            return Result(nameMapper.apply(name), desc, prefix)
        }

        override fun mapDesc(descMapper: UnaryOperator<Text>): Result {
            return Result(name, desc?.let { descMapper.apply(it) }, prefix)
        }

        override fun mapPrefix(prefixMapper: UnaryOperator<Text>): Result {
            return Result(name, desc, prefix?.let { prefixMapper.apply(it) })
        }
    }

    data class Name internal constructor(override val name: Text): ResultProvider<Name>() {

        override val desc: Text?
            get() = null

        override val prefix: Text?
            get() = null

        override fun map(nameMapper: UnaryOperator<Text>, descMapper: UnaryOperator<Text>, prefixMapper: UnaryOperator<Text>): Name {
            return Name(nameMapper.apply(name))
        }

        override fun mapName(nameMapper: UnaryOperator<Text>): Name {
            return Name(nameMapper.apply(name))
        }

        override fun mapDesc(descMapper: UnaryOperator<Text>): Name {
            return this
        }

        override fun mapPrefix(prefixMapper: UnaryOperator<Text>): Name {
            return this
        }
    }

    data class NameDesc internal constructor(override val name: Text, override val desc: Text): ResultProvider<NameDesc>() {

        override val prefix: Text?
            get() = null

        override fun map(nameMapper: UnaryOperator<Text>, descMapper: UnaryOperator<Text>, prefixMapper: UnaryOperator<Text>): NameDesc {
            return NameDesc(nameMapper.apply(name), descMapper.apply(desc))
        }

        override fun mapName(nameMapper: UnaryOperator<Text>): NameDesc {
            return NameDesc(nameMapper.apply(name), desc)
        }

        override fun mapDesc(descMapper: UnaryOperator<Text>): NameDesc {
            return NameDesc(name, descMapper.apply(desc))
        }

        override fun mapPrefix(prefixMapper: UnaryOperator<Text>): NameDesc {
            return this
        }
    }

    data class NamePrefix internal constructor(override val name: Text, override val prefix: Text): ResultProvider<NamePrefix>() {

        override val desc: Text?
            get() = null

        override fun map(nameMapper: UnaryOperator<Text>, descMapper: UnaryOperator<Text>, prefixMapper: UnaryOperator<Text>): NamePrefix {
            return NamePrefix(nameMapper.apply(name), prefixMapper.apply(prefix))
        }

        override fun mapName(nameMapper: UnaryOperator<Text>): NamePrefix {
            return NamePrefix(nameMapper.apply(name), prefix)
        }

        override fun mapDesc(descMapper: UnaryOperator<Text>): NamePrefix {
            return this
        }

        override fun mapPrefix(prefixMapper: UnaryOperator<Text>): NamePrefix {
            return NamePrefix(name, prefixMapper.apply(prefix))
        }
    }

    private data object Empty: ResultProvider<Empty>() {

        override val name: Text = FcText.empty()
        override val desc: Text? = null
        override val prefix: Text? = null

        override fun map(nameMapper: UnaryOperator<Text>, descMapper: UnaryOperator<Text>, prefixMapper: UnaryOperator<Text>): Empty {
            return this
        }

        override fun mapName(nameMapper: UnaryOperator<Text>): Empty {
            return this
        }

        override fun mapDesc(descMapper: UnaryOperator<Text>): Empty {
            return this
        }

        override fun mapPrefix(prefixMapper: UnaryOperator<Text>): Empty {
            return this
        }

    }

    /**
     * Abstract representation of a translation result. Implementations of this class may or may not have all three translation components
     * @author fzzyhmstrs
     * @since 0.6.8, will replace Result itself in 0.7.0
     */
    abstract class ResultProvider<T>: Searcher.SearchContent {
        abstract val name: Text
        abstract val desc: Text?
        abstract val prefix: Text?

        abstract fun map(nameMapper: UnaryOperator<Text>, descMapper: UnaryOperator<Text>, prefixMapper: UnaryOperator<Text>): T
        abstract fun mapName(nameMapper: UnaryOperator<Text>): T
        abstract fun mapDesc(descMapper: UnaryOperator<Text>): T
        abstract fun mapPrefix(prefixMapper: UnaryOperator<Text>): T

        override val content: ResultProvider<*>
            get() = this

        override val skip: Boolean = false
    }

    /**
     * Provides utilities for creating and caching translation results
     * @author fzzyhmstrs
     * @since 0.6.8
     */
    companion object Utils {

        private var cache: SoftReference<ConcurrentHashMap<String, Result>> = SoftReference(ConcurrentHashMap())

        internal fun invalidate() {
            cache = SoftReference(ConcurrentHashMap())
        }

        val EMPTY: ResultProvider<*> =  Empty

        /**
         * Retrieves a cached result, if any exists. If the cache has been invalidated this will return null
         * @param scope String representation of the object needing translation
         * @return [Result], nullable. Null if the cache doesn't contain a result or if it has been invalidated.
         * @author fzzyhmstrs
         * @since 0.6.8
         */
        @JvmStatic
        fun getScopedResult(scope: String): Result? {
            val m = cache.get() ?: return null //memory demands have wiped the cache, it will need to be rebuilt

            return m[scope]
        }

        /**
         * Caches the provided result and passes it through.
         * @param scope String representation of the object needing translation
         * @param result [Result] input result to cache
         * @return The input result after caching
         * @see createScopedResult
         * @author fzzyhmstrs
         * @since 0.6.8
         */
        @JvmStatic
        fun cacheScopedResult(scope: String, result: Result): Result {
            val m = cache.get()
            if (m == null) { //rebuild cache
                val m2 = ConcurrentHashMap<String, Result>()
                cache = SoftReference(m2)
                m2[scope] = result
                return result
            }
            m[scope] = result
            return result
        }

        /**
         * Creates a [Result], caches it, and passes the newly created result through.
         * @param scope String representation of the object needing translation
         * @param name [Text] the title of the element, such as "Particle Count"
         * @param desc [Text], nullable. the tooltip description. Null means no description is present.
         * @param prefix [Text], nullable. the inline prefix text of a config entry. Null means no prefix.
         * @return The created result after caching
         * @author fzzyhmstrs
         * @since 0.6.8
         */
        @JvmOverloads
        @JvmStatic
        fun createScopedResult(scope: String, name: Text, desc: Text? = null, prefix: Text? = null): Result {
            return cacheScopedResult(scope, createResult(name, desc, prefix))
        }

        /**
         * Creates a [Result] without caching it.
         * @param name [Text] the title of the element, such as "Particle Count"
         * @param desc [Text], nullable. the tooltip description. Null means no description is present.
         * @param prefix [Text], nullable. the inline prefix text of a config entry. Null means no prefix.
         * @return The created result
         * @author fzzyhmstrs
         * @since 0.6.8
         */
        @JvmOverloads
        @JvmStatic
        fun createResult(name: Text, desc: Text? = null, prefix: Text? = null): Result {
            return Result(name, desc, prefix)
        }
    }
}