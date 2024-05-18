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

import me.fzzyhmstrs.fzzy_config.annotations.NonSync
import me.fzzyhmstrs.fzzy_config.annotations.TomlHeaderComment
import me.fzzyhmstrs.fzzy_config.annotations.Version
import me.fzzyhmstrs.fzzy_config.api.ConfigApi.deserializeFromToml
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.config.ConfigContext
import me.fzzyhmstrs.fzzy_config.entry.EntrySerializer
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import net.peanuuutz.tomlkt.*
import java.io.File
import java.util.function.Supplier

/**
 * API for management of config files.
 *
 * Configs can de/serialized from File, string, or raw TomlElement. File read is performed with validation and correction.
 * @author fzzyhmstrs
 * @since 0.1.0
 */
object ConfigApi {

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
     * @param configClass A Function0 of config class instances.
     * @param registerType enum of [RegisterType] that defines which registries to register to. defaults to [RegisterType.BOTH]
     * @sample me.fzzyhmstrs.fzzy_config.examples.ConfigRegistration.registration
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmStatic
    @JvmOverloads
    @Deprecated("Consider registerAndLoadConfig() instead, to perform automatic loading, registering, and validating in one step.")
    fun <T: Config> registerConfig(config: T,configClass: () -> T, registerType: RegisterType = RegisterType.BOTH): T{
        return ConfigApiImpl.registerConfig(config, configClass, registerType)
    }

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
    fun <T: Config> registerConfig(config: T,configClass: Supplier<T>, registerType: RegisterType = RegisterType.BOTH): T{
        return ConfigApiImpl.registerConfig(config, { configClass.get() }, registerType)
    }

    /**
     * Creates and registers a Config. Use this over [registerConfig] and [readOrCreateAndValidate] if possible.
     *
     * Performs the entire creation, loading, validation, and registration process on a config class. Internally performs the two steps
     * 1) [readOrCreateAndValidate]
     * 2) [registerConfig]
     * @param T the config type, any subclass of [Config]
     * @param configClass Function0 of config class instances
     * @param registerType enum of [RegisterType] that defines which registries to register to. defaults to [RegisterType.BOTH]
     * @return loaded, validated, and registered instance of T
     * @sample me.fzzyhmstrs.fzzy_config.examples.ConfigRegistration.registration
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmStatic
    @JvmOverloads
    fun <T: Config> registerAndLoadConfig(configClass: () -> T, registerType: RegisterType = RegisterType.BOTH): T{
        return ConfigApiImpl.registerAndLoadConfig(configClass, registerType)
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
    fun <T: Config> registerAndLoadConfig(configClass: Supplier<T>, registerType: RegisterType = RegisterType.BOTH): T{
        return ConfigApiImpl.registerAndLoadConfig({ configClass.get() }, registerType)
    }

    /**
     * Reads a config from File or Creates a new config class; writes out any corrections, updates, or new content to File. Automatically adds ".toml" to the name for reading and writing.
     *
     * Includes [Version] updating support, automatic validation and correction, and detailed error reporting. Use this to generate the actual config class instance to be used in-game, if you have other custom initialization to perform, otherwise see [registerAndLoadConfig]. See the Example Config for typical usage case.
     * @param T The config class type. Must be a subclass of [Config]
     * @param name String. The config name, will become the file name. In an identifier, would be the "path". Adds ".toml" to the name for reading/writing to file automatically.
     * @param folder String, optional. A base config folder name. If left out, will write to the main config directory (not recommended). In an Identifier, this would be the "namespace"
     * @param subfolder String, optional. A subfolder name if desired. By default, blank. The file will appear in the base "namespace" folder if no child is given.
     * @param configClass Function0. A provider of instances of the config class itself. In Kotlin this can typically be written like `{ MyConfigClass() }`
     * @return An instance of the configClass passed to it, updated and validated or passed back as-is, depending on circumstances and errors encountered
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmStatic
    @JvmOverloads
    @Deprecated("Consider registerAndLoadConfig() instead, or readOrCreateAndValidate(configClass) for consistent application of names")
    fun <T: Config> readOrCreateAndValidate(name: String, folder: String = "", subfolder: String = "", configClass: () -> T): T{
        return ConfigApiImpl.readOrCreateAndValidate(name, folder, subfolder, configClass)
    }

    /**
     * Reads a config from File or Creates a new config class; writes out any corrections, updates, or new content to File. Automatically adds ".toml" to the name for reading and writing.
     *
     * Includes [Version] updating support, automatic validation and correction, and detailed error reporting. Use this to generate the actual config class instance to be used in-game, if you have other custom initialization to perform, otherwise see [registerAndLoadConfig]. See the Example Config for typical usage case.
     * @param T The config class type. Must be a subclass of [Config]
     * @param name String. The config name, will become the file name. In an identifier, would be the "path". Adds ".toml" to the name for reading/writing to file automatically.
     * @param folder String, optional. A base config folder name. If left out, will write to the main config directory (not recommended). In an Identifier, this would be the "namespace"
     * @param subfolder String, optional. A subfolder name if desired. By default, blank. The file will appear in the base "namespace" folder if no child is given.
     * @param configClass Supplier. A provider of instances of the config class itself.
     * @return An instance of the configClass passed to it, updated and validated or passed back as-is, depending on circumstances and errors encountered
     * @author fzzyhmstrs
     * @since 0.3.2
     */
    @JvmStatic
    @JvmOverloads
    @Deprecated("Consider registerAndLoadConfig() instead, or readOrCreateAndValidate(configClass) for consistent application of names")
    fun <T: Config> readOrCreateAndValidate(name: String, folder: String = "", subfolder: String = "", configClass: Supplier<T>): T{
        return ConfigApiImpl.readOrCreateAndValidate(name, folder, subfolder) { configClass.get() }
    }

    /**
     * overload of [readOrCreateAndValidate] that automatically applies the name, folder, and subfolder from the config itself. Automatically adds ".toml" to the name for reading and writing.
     * @param T type of config being created. Any subclass of [Config]
     * @param configClass Function0 of T
     * @return An instance of the configClass passed to it, updated and validated or passed back as-is, depending on circumstances and errors encountered
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmStatic
    fun <T: Config> readOrCreateAndValidate(configClass: () -> T): T{
        return ConfigApiImpl.readOrCreateAndValidate(configClass)
    }

    /**
     * overload of [readOrCreateAndValidate] that automatically applies the name, folder, and subfolder from the config itself. Automatically adds ".toml" to the name for reading and writing.
     * @param T type of config being created. Any subclass of [Config]
     * @param configClass Supplier of T
     * @return An instance of the configClass passed to it, updated and validated or passed back as-is, depending on circumstances and errors encountered
     * @author fzzyhmstrs
     * @since 0.3.2
     */
    @JvmStatic
    fun <T: Config> readOrCreateAndValidate(configClass: Supplier<T>): T{
        return ConfigApiImpl.readOrCreateAndValidate { configClass.get() }
    }

    /**
     * Saves a config to file without reading or updating.
     *
     * Performs a config save. Use after a config is updated in some way in-game. Does not perform any validation or reading/deleting of old or redundant files. This is used automatically by FzzyConfig when a client updates client-sided settings, receives an update from the server, or receives and accepts a forwarded setting; and also on the server when the server receives an update from a valid client.
     * @param T the type of the config, any subclass of [Config]
     * @param name the name of the config. Needs to match to the naming used in [readOrCreateAndValidate]
     * @param folder the folder the config is stored in. Needs to match the folder used in [readOrCreateAndValidate]
     * @param subfolder the subfolder inside folder that the config is stored in. Needs to match the subfolder used in [readOrCreateAndValidate]
     * @param configClass instance of the config to save
     * @see [save]
     */
    @JvmStatic
    @JvmOverloads
    @Deprecated("Consider save(configClass) for consistent application of names")
    fun <T : Config> saveManual(name: String, folder: String = "", subfolder: String = "", configClass: T) {
        ConfigApiImpl.save(name, folder, subfolder, configClass)
    }

    /**
     * Overload of [saveManual] that automatically fills in name, folder, and subfolder from the config itself.
     *
     * Performs a config save. Use after a config is updated in some way in-game. Does not perform any validation or reading/deleting of old or redundant files. This is used automatically by FzzyConfig when a client updates client-sided settings, receives an update from the server, or receives and accepts a forwarded setting; and also on the server when the server receives an update from a valid client.
     * @param T subclass of [Config]
     * @param configClass instance of T to be saved
     * @sample me.fzzyhmstrs.fzzy_config.examples.MyConfig.saveMe
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmStatic
    fun <T : Config> save(configClass: T) {
        ConfigApiImpl.save(configClass)
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
     * Serialize a config class to a TomlElement
     *
     * Custom serializer, powered by TomlKt. Serialization occurs in two ways
     * 1) [EntrySerializer] elements are serialized with their custom `serializeEntry` method
     * 2) "Raw" properties and fields are serialized with the TomlKt by-class-type serialization.
     *
     * Will serialize the available TomlAnnotations, for use in proper formatting, comment generation, etc. Note that if you register the config on the client side, TOML formatting may not be critical, as the user will generally edit the config in-game.
     * - [TomlHeaderComment] and [Version]: Will add top-of-file comments above the
     * - [TomlComment]: Adds a comment to the Toml file output. Accepts single line ("..") or multi-line ("""..""") comments
     * - [TomlBlockArray]: marks a list or other array object as a Block Array (Multi-line list). Default items per line is 1
     * - [TomlInline]: Marks that the annotated table or array element should be serialized as one line. Overrides TomlBlockArray
     * - [TomlMultilineString]: Marked string parses to file as a multi line string
     *
     * Should be called as a matched pair to [deserializeFromToml]. Ex: if `ignoreNonSync` is false on one end, it needs to be false on the other.
     * @param T Type of the config to serialize. Can be any Non-Null type.
     * @param config the config instance to serialize from
     * @param errorBuilder the error list. error messages are appended to this for display after the serialization call
     * @param ignoreNonSync default true. If false, elements with the [NonSync] annotation will be skipped. Use true to serialize the entire config (ex: saving to file), use false for syncing (ex: initial sync server -> client)
     * @return Returns a [TomlElement] of the serialized config
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmStatic
    @JvmOverloads
    fun <T: Any> serializeToToml(config: T, errorBuilder: MutableList<String>, flags: Byte = 1): TomlElement{
        return ConfigApiImpl.serializeToToml(config, errorBuilder, flags)
    }

    /**
     * Serializes a config class to a string.
     *
     * Extension of [serializeToToml] that takes the additional step of encoding to string. Use to write to a file or packet.
     * @param T Type of the config to serialize. Can be any Non-Null type.
     * @param config the config instance to serialize from
     * @param errorBuilder the error list. error messages are appended to this for display after the serialization call
     * @param ignoreNonSync default true. If false, elements with the [NonSync] annotation will be skipped. Use true to serialize the entire config (ex: saving to file), use false for syncing (ex: initial sync server -> client)
     * @return Returns a [TomlElement] of the serialized config
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmStatic
    @JvmOverloads
    fun <T: Any> serializeConfig(config: T, errorBuilder: MutableList<String>, flags: Byte = 1): String{
        return ConfigApiImpl.serializeConfig(config, errorBuilder, flags)
    }

    /**
     * Deserializes a config class from a TomlElement
     *
     * Custom deserializer, powered by TomlKt. Deserialization focuses on validation and building a useful error message. Deserialization happens in two ways
     * 1) [EntrySerializer] elements are deserialized with their custom `deserializeEntry` method
     * 2) "Raw" properties and fields are deserialized with the TomlKt by-class-type deserialization.
     *
     * Configs are deserialized "in place". That is to say, the deserializer iterates over the relevant fields and properties of a pre-instantiated "default" config class. Each relevant field/property is filled in with the results of deserializing from the TomlElement at the matching TomlTable key. If for some reason there is a critical error, the initial config passed in, with whatever deserialization was successfully completed, will be returned as a fallback.
     *
     * Should be called as a matched pair to [serializeToToml]. Ex: if `ignoreNonSync` is false on one end, it needs to be false on the other.
     * @param T the config type. Can be any Non-Null type.
     * @param config the config pre-deserialization
     * @param toml the TomlElement to deserialize from. Needs to be a TomlTable
     * @param errorBuilder a mutableList of strings the original caller of deserialization can use to print a detailed error log
     * @param ignoreNonSync default true. If false, elements with the [NonSync] annotation will be skipped. Use true to deserialize the entire config (ex: loading from file), use false for syncing (ex: initial sync server -> client)
     * @return Returns a [ValidationResult] of [ConfigContext] and applicable error, containing the config and any flag information
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmStatic
    @JvmOverloads
    fun <T: Any> deserializeFromToml(config: T, toml: TomlElement, errorBuilder: MutableList<String>, flags: Byte = 1): ValidationResult<ConfigContext<T>> {
        return ConfigApiImpl.deserializeFromToml(config, toml, errorBuilder, flags)
    }

    /**
     * Deserializes a config from a string.
     *
     * Extension of [deserializeFromToml] that deserializes directly from a string. Use to read from a file or packet.
     *
     * @param T the config type. can be Any non-null type.
     * @param config the config pre-deserialization
     * @param string the string to deserialize from. Needs to be valid Toml.
     * @param errorBuilder a mutableList of strings the original caller of deserialization can use to print a detailed error log
     * @param ignoreNonSync default true. If false, elements with the [NonSync] annotation will be skipped. Use true to deserialize the entire config (ex: loading from file), use false for syncing (ex: initial sync server -> client)
     * @return Returns [ValidationResult] of [ConfigContext]. The validation result includes the config and any applicable errors, and any flag information
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmStatic
    @JvmOverloads
    fun <T: Any> deserializeConfig(config: T, string: String, errorBuilder: MutableList<String>, flags: Byte = 1): ValidationResult<ConfigContext<T>> {
        return ConfigApiImpl.deserializeConfig(config, string, errorBuilder, flags)
    }

    /**
     * Creates a config directory keyed off the standard Fabric Config Directory
     *
     * Used to create a directory in the config parent directory inside the .minecraft folder. If the directory can't be created, the right/second member of the returned Pair will be false.
     * @param folder the base folder for created directories. Should be considered analogous to the 'namespace' for a mod using this lib
     * @param subfolder sub-folders for specific configs. Will typically be blank
     * @return A Pair<File, Boolean>, with a [File] instance and whether the directory could be successfully created
     * @author fzzyhmstrs
     * @since 0.1.0
     */
    @JvmStatic
    @Suppress("MemberVisibilityCanBePrivate")
    fun makeDir(folder: String, subfolder: String): Pair<File,Boolean>{
        return ConfigApiImpl.makeDir(folder, subfolder)
    }

}