/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.util.platform.impl

import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.util.PlatformApi
import me.fzzyhmstrs.fzzy_config.util.Translatable
import me.fzzyhmstrs.fzzy_config.util.platform.Registrar
import me.fzzyhmstrs.fzzy_config.util.platform.RegistrySupplier
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier
import net.minecraft.util.Util
import net.neoforged.fml.ModList
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import org.slf4j.Logger
import java.io.File
import java.util.*
import java.util.function.BiConsumer
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

internal object PlatformApiImpl: PlatformApi {

    override fun isClient(): Boolean {
        return PlatformUtils.isClient()
    }

    override fun configDir(): File {
        return PlatformUtils.configDir()
    }

    override fun gameDir(): File {
        return PlatformUtils.gameDir()
    }

    override fun isModLoaded(mod: String): Boolean {
        return PlatformUtils.isModLoaded(mod)
    }

    override fun isDev(): Boolean {
        return PlatformUtils.isDev()
    }

    override fun devLogger(name: String): Logger {
        return DevLogger(name)
    }

    override fun <T: Any> createRegistrar(namespace: String, registry: Registry<T>): Registrar<T> {
        return RegistrarImpl(namespace, registry)
    }

    override fun testVersion(id: String, version: String): Optional<Int> {
        return try {
            ModList.get().getModContainerById(id).map { container ->
                val v = DefaultArtifactVersion(version)
                container.modInfo.version.compareTo(v)
            }
        } catch (e: Throwable) {
            FC.DEVLOG.error("Critical error encountered parsing version in PlatformApi", e)
            Optional.empty()
        }
    }

    override fun <T : Any> buildRegistryTranslations(obj: T, prefix: String, lang: String, logWarnings: Boolean, builder: BiConsumer<String, String>) {
        buildRegistryTranslations(obj, prefix, lang, builder, logWarnings)
    }

    private val regSupplierClass = RegistrySupplier::class.java
    private val identifierClass = Identifier::class.java

    private fun <T: Any> buildRegistryTranslations(obj: T, prefix: String, lang: String, builder: BiConsumer<String, String>, logWarnings: Boolean = true) {

        try {
            val clazz = obj.javaClass.kotlin
            val orderById = clazz.java.declaredFields.withIndex().associate { it.value.name to it.index }.toMutableMap()
            val props = clazz.memberProperties.sortedBy { orderById[it.name] }

            for (prop in props) {
                try {
                    val propField = prop.javaField ?: continue
                    if (!(regSupplierClass.isAssignableFrom(propField.type) || identifierClass.isAssignableFrom(propField.type))) continue
                    val propVal = prop.get(obj)
                    val id = if (propVal is RegistrySupplier<*>) {
                        propVal.getId()
                    } else if (propVal is Identifier) {
                        propVal
                    } else {
                        continue
                    }

                    val annotations = prop.annotations
                    val key = Util.createTranslationKey(prefix, id)
                    annotations.filterIsInstance<Translatable.Name>().firstOrNull { it.lang == lang }.also {
                        if (it == null) FC.LOGGER.error("  No $lang name entry for $key")
                    }?.apply {
                        builder.accept(key, value)
                    }
                    annotations.filterIsInstance<Translatable.Desc>().firstOrNull { it.lang == lang }.also {
                        if (it == null && logWarnings) FC.LOGGER.warn("  No $lang description entry for $key")
                    }?.apply {
                        builder.accept("$key.desc", value)
                    }

                } catch (e: Exception) {
                    FC.LOGGER.error("Critical error building registry translation for ${prop.name} in $obj", e)
                }
            }
        } catch (e: Exception) {
            FC.LOGGER.error("Exception while building registry translations for $obj", e)
        }
    }
}