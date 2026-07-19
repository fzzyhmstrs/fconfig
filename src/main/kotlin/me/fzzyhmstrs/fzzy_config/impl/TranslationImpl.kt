/*
 * Copyright (c) 2026 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.impl

import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.annotations.Comment
import me.fzzyhmstrs.fzzy_config.annotations.Translation
import me.fzzyhmstrs.fzzy_config.cast
import me.fzzyhmstrs.fzzy_config.entry.EntryDelegate
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl.configClass
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl.configSectionClass
import me.fzzyhmstrs.fzzy_config.nullCast
import me.fzzyhmstrs.fzzy_config.util.Translatable
import me.fzzyhmstrs.fzzy_config.util.Walkable
import net.minecraft.util.Identifier
import net.peanuuutz.tomlkt.TomlComment
import java.lang.reflect.Modifier
import java.lang.reflect.Modifier.isTransient
import java.util.function.BiConsumer
import kotlin.jvm.kotlin
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KParameter
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

internal object TranslationImpl {

    private val walkableClass = Walkable::class
    private val entryDelegateClass = EntryDelegate::class

    internal fun <T: Any> buildTranslations(jclazz: Class<T>, id: Identifier, lang: String, builder: BiConsumer<String, String>, logWarnings: Boolean = true) {
        buildTranslations(jclazz.kotlin, id, lang, builder, logWarnings)
    }

    internal fun <T: Any> buildTranslations(clazz: KClass<T>, id: Identifier, lang: String, builder: BiConsumer<String, String>, logWarnings: Boolean = true) {
        buildTranslations(clazz, id.toTranslationKey(), lang, builder, logWarnings)
    }

    private fun buildTranslations(clazz: KClass<*>, prefix: String, lang: String, builder: BiConsumer<String, String>, logWarnings: Boolean, keyComposer: (String, String) -> String = { a, b -> "$a.$b" }) {

        try {
            val orderById =
                clazz.java.declaredFields.filter { !isTransient(it.modifiers) }.withIndex().associate { it.value.name to it.index }.toMutableMap()
            for (sup in clazz.allSuperclasses) {
                if (sup == configClass) continue //ignore Config itself, as that has state we don't need
                if (sup == configSectionClass) continue //ignore ConfigSection itself, as that has state we don't need
                orderById.putAll(sup.java.declaredFields.filter { !isTransient(it.modifiers) }.withIndex().associate { it.value.name to it.index })
            }

            val props = clazz.memberProperties.filter {
                it is KMutableProperty<*> && !isTransient(it.javaField?.modifiers ?: Modifier.TRANSIENT)
            }.sortedBy { orderById[it.name] }

            FC.LOGGER.info("Building $lang entries for ${clazz.simpleName} @ $prefix")

            //base config lang itself
            val clazzAnnotations = clazz.annotations
            val clazzPrefix = clazzPrefix(prefix, clazzAnnotations)
            if (configClass.java.isAssignableFrom(clazz.java))
                applyTranslation(clazzPrefix, clazzAnnotations, lang, builder, logWarnings)

            for (prop in props) {
                try {
                    val name = prop.name
                    val annotations = prop.annotations
                    val propPrefix = getPrefix(prefix, annotations, clazzAnnotations)
                    val key = keyComposer(propPrefix, name)
                    applyTranslation(key, annotations, lang, builder, logWarnings)
                    val propClass = prop.javaField?.type
                    if (propClass != null && (configSectionClass.java.isAssignableFrom(propClass) || walkableClass.java.isAssignableFrom(propClass))) {
                        //burrow into sections and walkables
                        buildTranslations(propClass.kotlin, key, lang, builder, logWarnings)
                    } else if (propClass != null && entryDelegateClass.java.isAssignableFrom(propClass)) {
                        try {
                            val instance = clazz.constructors.singleOrNull { it.parameters.all(KParameter::isOptional) }?.callBy(emptyMap())
                            if (instance == null) {
                                FC.LOGGER.error("Delegate validation [$key] found in config without empty constructor. Fzzy Config won't be able to apply translations")
                                continue
                            }
                            val thing = prop.cast<KMutableProperty1<Any, *>>().get(instance) as? EntryDelegate
                            thing?.delegateClass()?.let {
                                buildTranslations(it, key, lang, builder, logWarnings)
                            }
                        } catch (e: Exception) {
                            FC.LOGGER.error("Exception while building translations for delegate validation [$key]", e)
                        }
                    }
                } catch (e: Exception) {
                    FC.LOGGER.error("Critical error building translation for ${prop.name} in ${clazz.simpleName}", e)
                }
            }
        } catch (e: Exception) {
            FC.LOGGER.error("Exception while building translations for ${clazz.simpleName}", e)
        }
    }

    private fun applyTranslation(key: String, annotations: List<Annotation>, lang: String, builder: BiConsumer<String, String>, logWarnings: Boolean) {
        annotations.filterIsInstance<Translatable.Name>().firstOrNull { it.lang == lang }.also {
            if (it == null) FC.LOGGER.error("  No $lang name entry for $key")
        }?.apply {
            builder.accept(key, value)
        }
        annotations.filterIsInstance<Translatable.Desc>().firstOrNull { it.lang == lang }.also {
            if (it == null) {
                val comment = annotations.firstNotNullOfOrNull { a -> a.nullCast<Comment>() }
                if (comment != null && lang == "en_us") {
                    builder.accept("$key.desc", comment.value)
                } else {
                    val tomlComment = annotations.firstNotNullOfOrNull { a -> a.nullCast<TomlComment>() }
                    if (tomlComment != null && lang == "en_us") {
                        builder.accept("$key.desc", tomlComment.text)
                    } else if (logWarnings) {
                        FC.LOGGER.warn("  No $lang description entry for $key")
                    }
                }
            }
        }?.apply {
            builder.accept("$key.desc", value)
        }
        annotations.filterIsInstance<Translatable.Prefix>().firstOrNull { it.lang == lang }.also {
            if (it == null && logWarnings) FC.LOGGER.warn("  No $lang prefix entry for $key")
        }?.apply {
            builder.accept("$key.prefix", value)
        }
    }

    private fun getPrefix(basePrefix: String, annotations: List<Annotation>, globalAnnotations: List<Annotation>): String {
        for (annotation in annotations) {
            if (annotation is Translation) {
                for (ga in globalAnnotations) {
                    if (ga is Translation) {
                        return if (ga.negate) {
                            basePrefix
                        } else {
                            annotation.prefix
                        }
                    }
                }
                return if (annotation.negate) {
                    basePrefix
                } else {
                    annotation.prefix
                }
            }
        }
        for (ga in globalAnnotations) {
            if (ga is Translation && !ga.negate) {
                return ga.prefix
            }
        }
        return basePrefix
    }

    private fun clazzPrefix(basePrefix: String, globalAnnotations: List<Annotation>): String {
        for (ga in globalAnnotations) {
            if (ga is Translation && !ga.negate) {
                return ga.prefix
            }
        }
        return basePrefix
    }
}