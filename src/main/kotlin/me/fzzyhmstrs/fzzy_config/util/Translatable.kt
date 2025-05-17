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

import me.fzzyhmstrs.fzzy_config.annotations.Comment
import me.fzzyhmstrs.fzzy_config.annotations.Translation
import me.fzzyhmstrs.fzzy_config.util.FcText.transSupplied
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import net.minecraft.client.resource.language.I18n
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Language
import net.peanuuutz.tomlkt.TomlComment
import java.lang.ref.SoftReference
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Supplier
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

    object Impls {

        internal fun getText(thing: Any, scope: String, fieldName: String, annotations: List<Annotation>, globalAnnotations: List<Annotation>, fallback: String = fieldName): Result {
            val cachedText = getScopedResult(scope)
            if (cachedText != null) return cachedText
            for (annotation in annotations) {
                if (annotation is Translation) {
                    for (ga in globalAnnotations) {
                        if (ga is Translation) {
                            return if (ga.negate) {
                                createScopedResult(thing, annotations, fallback, scope)
                            } else {
                                createKeyedScopedResult(thing, scope, fieldName, ga, annotations, fallback)
                            }
                        }
                    }
                    if (annotation.negate) {
                        return createScopedResult(thing, annotations, fallback, scope)
                    }
                    return createKeyedScopedResult(thing, scope, fieldName, annotation, annotations, fallback)
                }
            }
            for (annotation in globalAnnotations) {
                if (annotation is Translation && !annotation.negate) {
                    return createKeyedScopedResult(thing, scope, fieldName, annotation, annotations, fallback)
                }
            }
            return createScopedResult(thing, annotations, fallback, scope)
        }

        private fun createScopedResult(thing: Any, annotations: List<Annotation>, fallback: String, scope: String): Result {
            val n = thing.transSupplied { getNameFallback(annotations, fallback) }
            val d = thing.descGet { getDescFallback(annotations) }
            val p = thing.prefixGet { getPrefixFallback(annotations) }
            return Utils.createScopedResult(scope, n, d, p)
        }

        private fun createKeyedScopedResult(thing: Any, scope: String, fieldName: String, annotation: Translation, annotations: List<Annotation>, fallback: String): Result {
            val bl = fieldName.isNotEmpty()
            val keyN = if(bl) FcText.concat(annotation.prefix, PERIOD, fieldName) else annotation.prefix
            val keyD = if(bl) FcText.concat(annotation.prefix, PERIOD, fieldName, DESC) else FcText.concat(annotation.prefix, DESC)
            val keyP = if(bl) FcText.concat(annotation.prefix, PERIOD, fieldName, PREFIX) else FcText.concat(annotation.prefix, PREFIX)
            val n = if (I18n.hasTranslation(keyN)) keyN.translate() else thing.transSupplied { getNameFallback(annotations, fallback) }
            val d = if (I18n.hasTranslation(keyD)) keyD.translate() else thing.descGet { getDescFallback(annotations) }
            val p = if (I18n.hasTranslation(keyP)) keyP.translate() else thing.prefixGet { getPrefixFallback(annotations) }
            return Utils.createScopedResult(scope, n, d, p)
        }

        private fun Any?.descGet(fallbackSupplier: Supplier<String>): MutableText? {
            if(this is Translatable) {
                if (this.hasDescription()) {
                    return this.description()
                }
            }
            val fallback = fallbackSupplier.get()
            return if (fallback != "")
                FcText.literal(fallback).formatted(Formatting.ITALIC)
            else
                null
        }

        private fun Any?.prefixGet(fallbackSupplier: Supplier<String>): MutableText? {
            if(this is Translatable) {
                if (this.hasPrefix()) {
                    return this.prefix()
                }
            }
            val fallback = fallbackSupplier.get()
            return if (fallback != "")
                FcText.literal(fallback).formatted(Formatting.ITALIC)
            else
                null
        }

        private const val SPACER = ". "
        private const val PERIOD = "."
        internal const val DESC = ".desc"
        internal const val PREFIX = ".prefix"

        private fun getNameFallback(annotations: List<Annotation>, fallback: String): String {
            return annotations.filterIsInstance<Name>().let { l ->
                l.firstOrNull { a -> a.lang == "en_us" }?.value ?: l.firstOrNull()?.value
            } ?: fallback.replace('_', ' ').split(FcText.regex).joinToString(" ") { it.lowercase(); it.replaceFirstChar { c -> c.uppercase() } }
        }

        private fun getDescFallback(annotations: List<Annotation>): String {
            val comment = mutableListOf<String>()
            for (annotation in annotations) {
                if (annotation is TomlComment) {
                    comment.add(annotation.text)
                } else if(annotation is Comment) {
                    comment.add(annotation.value)
                } else if(annotation is Desc && annotation.lang == "en_us") {
                    comment.add(annotation.value)
                }
                Language.getInstance()
            }
            return if (comment.isEmpty()) "" else comment.joinToString(separator = SPACER, postfix = PERIOD)
        }

        private fun getPrefixFallback(annotations: List<Annotation>): String {
            val comment = mutableListOf<String>()
            for (annotation in annotations) {
                if(annotation is Prefix && annotation.lang == "en_us") {
                    comment.add(annotation.value)
                }
            }
            return if (comment.isEmpty()) "" else comment.joinToString(separator = SPACER, postfix = PERIOD)
        }
    }

    private data class Full(override val name: Text, override val desc: Text? = null, override val prefix: Text? = null): Result(), Searcher.SearchContent {

        override val content = this

        override val skip = false

        override fun map(nameMapper: UnaryOperator<Text>, descMapper: UnaryOperator<Text>, prefixMapper: UnaryOperator<Text>): Result {
            return Full(nameMapper.apply(name), desc?.let { descMapper.apply(it) }, prefix?.let { prefixMapper.apply(it) })
        }

        override fun mapName(nameMapper: UnaryOperator<Text>): Result {
            return Full(nameMapper.apply(name), desc, prefix)
        }

        override fun mapDesc(descMapper: UnaryOperator<Text>): Result {
            return Full(name, desc?.let { descMapper.apply(it) }, prefix)
        }

        override fun mapPrefix(prefixMapper: UnaryOperator<Text>): Result {
            return Full(name, desc, prefix?.let { prefixMapper.apply(it) })
        }
    }

    private data class Named(override val name: Text): Result() {

        override val desc: Text?
            get() = null

        override val prefix: Text?
            get() = null

        override fun map(nameMapper: UnaryOperator<Text>, descMapper: UnaryOperator<Text>, prefixMapper: UnaryOperator<Text>): Result {
            return Named(nameMapper.apply(name))
        }

        override fun mapName(nameMapper: UnaryOperator<Text>): Result {
            return Named(nameMapper.apply(name))
        }

        override fun mapDesc(descMapper: UnaryOperator<Text>): Result {
            return this
        }

        override fun mapPrefix(prefixMapper: UnaryOperator<Text>): Result {
            return this
        }
    }

    private data class NameDesc(override val name: Text, override val desc: Text): Result() {

        override val prefix: Text?
            get() = null

        override fun map(nameMapper: UnaryOperator<Text>, descMapper: UnaryOperator<Text>, prefixMapper: UnaryOperator<Text>): Result {
            return NameDesc(nameMapper.apply(name), descMapper.apply(desc))
        }

        override fun mapName(nameMapper: UnaryOperator<Text>): Result {
            return NameDesc(nameMapper.apply(name), desc)
        }

        override fun mapDesc(descMapper: UnaryOperator<Text>): Result {
            return NameDesc(name, descMapper.apply(desc))
        }

        override fun mapPrefix(prefixMapper: UnaryOperator<Text>): Result {
            return this
        }
    }

    private data class NamePrefix(override val name: Text, override val prefix: Text): Result() {

        override val desc: Text?
            get() = null

        override fun map(nameMapper: UnaryOperator<Text>, descMapper: UnaryOperator<Text>, prefixMapper: UnaryOperator<Text>): Result {
            return NamePrefix(nameMapper.apply(name), prefixMapper.apply(prefix))
        }

        override fun mapName(nameMapper: UnaryOperator<Text>): Result {
            return NamePrefix(nameMapper.apply(name), prefix)
        }

        override fun mapDesc(descMapper: UnaryOperator<Text>): Result {
            return this
        }

        override fun mapPrefix(prefixMapper: UnaryOperator<Text>): Result {
            return NamePrefix(name, prefixMapper.apply(prefix))
        }
    }

    private data object Empty: Result() {

        override val name: Text = FcText.empty()
        override val desc: Text? = null
        override val prefix: Text? = null

        override fun map(nameMapper: UnaryOperator<Text>, descMapper: UnaryOperator<Text>, prefixMapper: UnaryOperator<Text>): Result = this
        override fun mapName(nameMapper: UnaryOperator<Text>): Result = this
        override fun mapDesc(descMapper: UnaryOperator<Text>): Result = this
        override fun mapPrefix(prefixMapper: UnaryOperator<Text>): Result = this
    }

    /**
     * Abstract representation of a translation result. Implementations of this class may or may not have all three translation components
     * @author fzzyhmstrs
     * @since 0.6.8, will replace Result itself in 0.7.0
     */
    abstract class Result: Searcher.SearchContent {
        abstract val name: Text
        abstract val desc: Text?
        abstract val prefix: Text?

        abstract fun map(nameMapper: UnaryOperator<Text>, descMapper: UnaryOperator<Text>, prefixMapper: UnaryOperator<Text>): Result
        abstract fun mapName(nameMapper: UnaryOperator<Text>): Result
        abstract fun mapDesc(descMapper: UnaryOperator<Text>): Result
        abstract fun mapPrefix(prefixMapper: UnaryOperator<Text>): Result

        override val content: Result
            get() = this

        override val skip: Boolean
            get() = false
    }

    /**
     * TODO()
     * @author fzzyhmstrs
     * @since 0.7.0
     */
    @Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.CLASS)
    @Repeatable
    annotation class Name(val value: String, val lang: String = "en_us")

    /**
     * Description string for datagen or as a "fallback" description implementation.
     *
     * If there is no [Comment] or [TomlComment] annotation and there is an en_us Desc attached, it will be used as the toml comment. Conversely, [Comment] or [TomlComment] will also be utilized for en_us datagen if available and no Desc is provided.
     *
     * This annotation is repeatable, so can be used to provide lang values for any number of languages.
     * @param value the description string
     * @param lang Default "en_us" the lang key applicable to this desc
     * @author fzzyhmstrs
     * @since 0.7.0
     */
    @Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.CLASS)
    @Repeatable
    annotation class Desc(val value: String, val lang: String = "en_us")

    /**
     * TODO()
     * @author fzzyhmstrs
     * @since 0.7.0
     */
    @Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.CLASS)
    @Repeatable
    annotation class Prefix(val value: String, val lang: String = "en_us")

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

        /**
         * An empty translation result with null description, null prefix, and empty text name.
         * @author fzzyhmstrs
         * @since 0.6.8
         */
        val EMPTY: Result =  Empty

        /**
         * Retrieves a cached result, if any exists. If the cache has been invalidated this will return null
         * @param scope String representation of the object needing translation
         * @return [Full], nullable. Null if the cache doesn't contain a result or if it has been invalidated.
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
         * @param result [Full] input result to cache
         * @return The input result provider after caching
         * @see createScopedResult
         * @author fzzyhmstrs
         * @since 0.6.8, returns ResultProvider 0.7.0
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
         * Creates a [Full], caches it, and passes the newly created result through.
         * @param scope String representation of the object needing translation
         * @param name [Text] the title of the element, such as "Particle Count"
         * @param desc [Text], nullable. the tooltip description. Null means no description is present.
         * @param prefix [Text], nullable. the inline prefix text of a config entry. Null means no prefix.
         * @return The created result after caching
         * @author fzzyhmstrs
         * @since 0.6.8, returns ResultProvider 0.7.0
         */
        @JvmOverloads
        @JvmStatic
        fun createScopedResult(scope: String, name: Text, desc: Text? = null, prefix: Text? = null): Result {
            return cacheScopedResult(scope, createResult(name, desc, prefix))
        }

        /**
         * Creates a [Full] without caching it.
         * @param name [Text] the title of the element, such as "Particle Count"
         * @param desc [Text], nullable. the tooltip description. Null means no description is present.
         * @param prefix [Text], nullable. the inline prefix text of a config entry. Null means no prefix.
         * @return The created result
         * @author fzzyhmstrs
         * @since 0.6.8, returns ResultProvider 0.7.0
         */
        @JvmOverloads
        @JvmStatic
        fun createResult(name: Text, desc: Text? = null, prefix: Text? = null): Result {
            return if (desc == null) {
                if (prefix == null) {
                    Named(name)
                } else {
                    NamePrefix(name, prefix)
                }
            } else if (prefix == null) {
                NameDesc(name, desc)
            } else {
                Full(name, desc, prefix)
            }
        }
    }
}