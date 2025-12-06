/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.util

import me.fzzyhmstrs.fzzy_config.annotations.Comment
import me.fzzyhmstrs.fzzy_config.util.platform.Registrar
import me.fzzyhmstrs.fzzy_config.util.platform.RegistryBuilder
import net.minecraft.registry.Registry
import net.peanuuutz.tomlkt.TomlComment
import org.jetbrains.annotations.ApiStatus
import org.slf4j.Logger
import java.io.File
import java.util.*
import java.util.function.BiConsumer

/**
 * API for abstraction of simple ModLoader requests
 * @author fzzyhmstrs
 * @since 0.5.0
 */
interface PlatformApi {

    /**
     * Whether the game includes a logical client or not. This will be true both for singleplayer games and the client side of a multiplayer game.
     * @return true if a logical client is present (so you can access client code like MinecraftClient), false if the environment is a dedicated server.
     * @author fzzyhmstrs
     * @since 0.5.0
     */
    fun isClient(): Boolean

    /**
     * The config directory
     * @return [File] representing the standard config directory inside the game folder.
     * @author fzzyhmstrs
     * @since 0.5.0
     */
    fun configDir(): File

    /**
     * The root game directory
     * @return [File] representing the path of the root game directory.
     * @author fzzyhmstrs
     * @since 0.5.0
     */
    fun gameDir(): File

    /**
     * Returns whether another mod is loaded based on their registered mod_id.
     * @return true if the mod is present, false otherwise
     * @author fzzyhmstrs
     * @since 0.5.0
     */
    fun isModLoaded(mod: String): Boolean

    /**
     * Whether the current JVM environment is a development environment
     * @author fzzyhmstrs
     * @since 0.5.9
     */
    fun isDev(): Boolean

    /**
     * Returns a logger that checks if the current environment [isDev] before doing any logging
     * @param name String id of the delegate logger. This will be created with LoggerFactory.getLogger
     * @return [Logger] instance with builtin dev checks
     * @author fzzyhmstrs
     * @since 0.5.9
     */
    fun devLogger(name: String): Logger

    /**
     * Creates a [Registrar] wrapper for registering objects in a platform-agnostic way.
     *
     * Note the use of T: Any for this particular version. In 0.8.0 I will update all methods across versions to this. This is for successful build in 1.21.11 neo.
     * @param namespace String namespace to register objects under
     * @param registry [Registry] registry to wrap
     * @return [Registrar] platform-agnostic wrapper that registers objects
     * @author fzzyhmstrs
     * @since 0.5.9, no longer experimental 0.7.0
     */
    fun <T: Any> createRegistrar(namespace: String, registry: Registry<T>): Registrar<T>

    /**
     * Creates a [RegistryBuilder] for creating registries in a platform-agnostic way, along with providing other registry utilities.
     *
     * [See the Wiki](https://moddedmc.wiki/en/project/fzzy-config/docs/features/Registrar-System) for more details and examples.
     * @param namespace String namespace to register objects under
     * @return [RegistryBuilder] platform-agnostic wrapper for creating registries
     * @author fzzyhmstrs
     * @since 0.7.4
     */
    fun <T> createRegistryBuilder(namespace: String): RegistryBuilder

    /**
     * Applies a set of translations for the provided registry instance to the provided [builder]. Uses [Translatable.Name], [Translatable.Desc], and [Translatable.Prefix] annotations to power the generation. [TomlComment] and [Comment] can be used to provide en_us description lang.
     *
     * This translation only works using an INSTANCE pattern. Kotlin objects, for example. In Java, you will have to define your items, blocks, etc. as member fields in the class and create a static INSTANCE that is referenced wherever.
     * @param T Non-null registry type
     * @param obj [T] instance to scrape
     * @param prefix String prefix applicable to the registry type. For example, an items registry will use "item". This will create keys like "item.namespace.path"
     * @param lang The applicable lang code to generate for, e.g. "en_us" or "es_mx". The builder will look for annotations with matching codes to apply.
     * @param logWarnings If true, Fzzy Config will log warnings for every missing name, description, and prefix; if false only missing names will be logged.
     * @param builder [BiConsumer]&lt;String, String&gt; that accepts new lang entries. For fabric lang generation this could be `TranslationBuilder::add`
     * @author fzzyhmstrs
     * @since 0.7.0
     */
    fun <T: Any> buildRegistryTranslations(obj: T, prefix: String, lang: String, logWarnings: Boolean, builder: BiConsumer<String, String>)

    /**
     * Tests the version of the provided mod (or minecraft)
     * @param id String - the modid to test the version of. Use "minecraft" to test MCs loaded version
     * @param version String representation of the version to test. Example "0.6.3". The loaders may have individual quirks when it comes to parsing the result
     * @return [Optional]&lt;Int&gt; - The resulting version comparison. If the comparison isn't valid for some reason (or the id isn't loaded) the optional will be empty, otherwise will be the result of a Comparator check (negative = loaded version less than the requested version, 0 = versions functionally equal, positive = loaded version is higher).
     * @author fzzyhmstrs
     * @since 0.6.3
     */
    fun testVersion(id: String, version: String): Optional<Int>
}