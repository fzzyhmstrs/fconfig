@file:Suppress("unused", "DeprecatedCallableAddReplaceWith")

/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.api

import me.fzzyhmstrs.fzzy_config.api.ConfigApi.readOrCreateAndValidate
import me.fzzyhmstrs.fzzy_config.api.ConfigApi.registerAndLoadConfig
import me.fzzyhmstrs.fzzy_config.api.ConfigApi.registerConfig
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.networking.api.NetworkApi
import me.fzzyhmstrs.fzzy_config.networking.impl.NetworkApiImpl
import me.fzzyhmstrs.fzzy_config.util.PlatformApi
import me.fzzyhmstrs.fzzy_config.util.PlatformApiImpl
import net.peanuuutz.tomlkt.*
import java.util.function.Supplier

/**
 * API for management of config files, with better compile-time friendliness for Java-only builds.
 *
 * Configs can de/serialized from File, string, or raw TomlElement. File read is performed with validation and correction.
 * @author fzzyhmstrs
 * @since 0.1.0
 */
object ConfigApiJava {


    /**
     * Registers a [Config] to registries. Does NOT load or validate it from file. Use this if you have custom initialization to perform, otherwise use [registerAndLoadConfig] for full initialization functionality.
     *
     * Configs registered this way still have to handle their own initialization. That is to say, they have to be instantiated and passed to the registry in a timely manner, otherwise they will not be loaded in time for CONFIGURATION stage syncing with clients. Loading with the Fabric [ModInitializer][net.fabricmc.api.ModInitializer] is a convenient and typical way to achieve this.
     *
     * Depending on the RegistryType(s) picked, the config will have different functionalities:
     * - SYNC: Will be registered to the SyncedConfigRegistry. Configs will be automatically synchronized and saved to clients during the CONFIGURATION stage, and also during any datapack reloads. Configs will NOT have client-side GUIs with this selection.
     * - CLIENT: Will be registered to the ClientConfigRegistry. Configs will have GUI Screens automatically generated for in-game configuration, and screens will be automatically registered with ModMenu and Catalogue. Clients will not sync between servers and clients, and players don't need any special permissions to edit entries in a CLIENT config.
     * - BOTH: Will be registered to both registries (default functionality). Updates made in the client-side GUI by Server Operators with the correct permissions will be automatically propagated to the server and out to any clients currently connected.
     * @param T the config type, any subclass of [Config]
     * @param config the config to register
     * @param configClass A Supplier of config class instances.
     * @param registerType enum of [RegisterType] that defines which registries to register to. defaults to [RegisterType.BOTH]
     * @sample me.fzzyhmstrs.fzzy_config.examples.ConfigRegistration.registration
     * @author fzzyhmstrs
     * @since 0.3.2
     */
    @JvmStatic
    @JvmOverloads
    @Deprecated("Consider registerAndLoadConfig() instead, to perform automatic loading, registering, and validating in one step.")
    fun <T: Config> registerConfig(config: T, configClass: Supplier<T>, registerType: RegisterType = RegisterType.BOTH): T {
        return ConfigApi.registerConfig(config, configClass, registerType)
    }

    /**
     * Creates and registers a Config. Use this over [registerConfig] and [readOrCreateAndValidate] if possible.
     *
     * Performs the entire creation, loading, validation, and registration process on a config class. Internally performs the two steps
     * 1) [readOrCreateAndValidate]
     * 2) [registerConfig]
     * @param T the config type, any subclass of [Config]
     * @param configClass Supplier of config class instances
     * @param registerType enum of [RegisterType] that defines which registries to register to. defaults to [RegisterType.BOTH]
     * @return loaded, validated, and registered instance of T
     * @sample me.fzzyhmstrs.fzzy_config.examples.ConfigRegistration.registration
     * @author fzzyhmstrs
     * @since 0.3.2
     */
    @JvmStatic
    @JvmOverloads
    fun <T: Config> registerAndLoadConfig(configClass: Supplier<T>, registerType: RegisterType = RegisterType.BOTH): T {
        return ConfigApi.registerAndLoadConfig(configClass, registerType)
    }

    /**
     * Opens a config GUI. Does nothing on the server (But is not marked with @Environment, allowing for safe inclusion anywhere in code)
     *
     * In order for a screen to exist, Configs must be registered to the [ClientConfigRegistry][me.fzzyhmstrs.fzzy_config.registry.ClientConfigRegistry] via [registerConfig] or [registerAndLoadConfig]
     *
     * FzzyConfig automatically registered Config GUIs with ModMenu and Catalogue, so you do not strictly need to implement any custom screen opening functionality unless desired.
     * @param scope the scope of the config screen to be opened. This is the "translation key" of the config(s) you want to open. For example, `"my_mod:my_config" > "my_mod.my_config"` To open a selection screen of every config from a mod, pass the namespace: `"my_mod"`.
     * @sample [me.fzzyhmstrs.fzzy_config.examples.ConfigGuiOpener.exampleScreenOpening]
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun openScreen(scope: String) {
        ConfigApiImpl.openScreen(scope)
    }

    /**
     * Provides an instance of the [NetworkApi] for usage of the built-in cross-loader networking API
     * @author fzzyhmstrs
     * @since 0.4.4
     */
    @JvmStatic
    fun network(): NetworkApi {
        return NetworkApiImpl
    }

    /**
     * Provides an instance of the [PlatformApi] for usage of the built-in cross-loader utilities
     * @author fzzyhmstrs
     * @since 0.4.4
     */
    @JvmStatic
    fun platform(): PlatformApi {
        return PlatformApiImpl
    }

}