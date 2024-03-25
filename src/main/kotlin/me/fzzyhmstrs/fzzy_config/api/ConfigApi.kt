@file:Suppress("unused")

package me.fzzyhmstrs.fzzy_config.api

import me.fzzyhmstrs.fzzy_config.annotations.NonSync
import me.fzzyhmstrs.fzzy_config.annotations.TomlHeaderComment
import me.fzzyhmstrs.fzzy_config.annotations.Version
import me.fzzyhmstrs.fzzy_config.api.ConfigApi.deserializeFromToml
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.registry.SyncedConfigRegistry
import me.fzzyhmstrs.fzzy_config.validation.entry.EntrySerializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.peanuuutz.tomlkt.*
import java.io.File

/**
 * API for de/serialization, registration, and loading of config files.
 *
 * De/serialized from File, string, or raw TomlElement. File read is performed with validation and correction.
 *
 * @author fzzyhmstrs
 * @since 0.1.0
 */
object ConfigApi {

    /**
     * Registers a [Config] to the [SyncedConfigRegistry]. Use if you have custom initialization to perform.
     *
     * Configs registered this way still have to handle their own initialization. That is to say, they have to be instantiated and passed to the registry in a timely manner, otherwise they will not be loaded in time for CONFIGURATION stage syncing with clients.
     *
     * Loading with the Fabric [ModInitializer][net.fabricmc.api.ModInitializer] is a convenient and typical way to achieve this.
     * @param T the config type, any subclass of [Config]
     * @param config the config to register
     * @param registerType enum of [RegisterType] that defines which registries to register to. defaults to [RegisterType.BOTH]
     * @sample me.fzzyhmstrs.fzzy_config.examples.ConfigRegistration
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmStatic
    fun <T: Config> registerConfig(config: T, registerType: RegisterType = RegisterType.BOTH): T{
        return ConfigApiImpl.registerConfig(config, registerType)
    }

    /**
     * Creates and registers a Config.
     * Performs the entire creation, loading, validation, and registration process on a config class. Internally performs the two steps
     * 1) [readOrCreateAndValidate]
     * 2) [registerConfig]
     * @param T the config type, any subclass of [Config]
     * @param configClass supplier of T
     * @param registerType enum of [RegisterType] that defines which registries to register to. defaults to [RegisterType.BOTH]
     * @return loaded, validated, and registered instance of T
     * @sample me.fzzyhmstrs.fzzy_config.examples.ConfigRegistration
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmStatic
    fun <T: Config> registerAndLoadConfig(configClass: () -> T, registerType: RegisterType = RegisterType.BOTH): T{
        return ConfigApiImpl.registerAndLoadConfig(configClass, registerType)
    }

    /**
     * Reads in from File or Creates a new config class, and writes out any corrections, updates, or new content to File.
     *
     * Config Class and File generator with [Version] updating support, automatic validation and correction, and detailed error reporting. Use this to generate the actual config class instance to be used in-game. See the Example Config for typical usage case.
     *
     * @param T The config class type. Must be a subclass of [Config]
     * @param name String. The config name, will become the file name. In an identifier, would be the "path". Adds ".toml" to the name for reading/writing to file automatically.
     * @param folder String, optional. A base config folder name. If left out, will write to the main config directory (not recommended). In an Identifier, this would be the "namespace"
     * @param subfolder String, optional. A subfolder name if desired. By default, blank. The file will appear in the base "namespace" folder if no child is given.
     * @param configClass () -> T. A provider of instances of the config class itself. In Kotlin this can typically be written like `{ MyConfigClass() }`
     * @return An instance of the configClass passed to it, updated and validated or passed back as-is, depending on circumstances and errors encountered
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmStatic
    @Deprecated("Consider readOrCreateAndValidate(configClass) for consistent application of names")
    fun <T: Config> readOrCreateAndValidate(name: String, folder: String = "", subfolder: String = "", configClass: () -> T): T{
        return ConfigApiImpl.readOrCreateAndValidate(name, folder, subfolder, configClass)
    }

    /**
     * overload of [readOrCreateAndValidate] that automatically applies the name, folder, and subfolder from the config itself. Automatically adds ".toml" to the name for reading and writing.
     *
     * @param T type of config being created. Any subclass of [Config]
     * @param configClass supplier of T
     * @return An instance of the configClass passed to it, updated and validated or passed back as-is, depending on circumstances and errors encountered
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmStatic
    fun <T: Config> readOrCreateAndValidate(configClass: () -> T): T{
        return ConfigApiImpl.readOrCreateAndValidate(configClass)
    }

    /**
     * Saves a config to file without reading or updating.
     *
     * Performs a config save. Use after a Mod is updated via gui and/or command. Does not perform any validation or reading/deleting of old or redundant files. This is used automatically by FzzyConfig when a client updates client-sided settings or receives and accepts a forwarded setting, and when the server receives a server update from a valid client.
     * @param T the type of the config, any subclass of [Config]
     * @param name the name of the config. Needs to match to the naming used in [readOrCreateAndValidate]
     * @param folder the folder the config is stored in. Needs to match the folder used in [readOrCreateAndValidate]
     * @param subfolder the subfolder inside folder that the config is stored in. Needs to match the subfolder used in [readOrCreateAndValidate]
     * @param configClass instance of the config to save
     * @see [save]
     */
    @JvmStatic
    @Deprecated("save(configClass) is strongly recommended instead, for consistent application of names")
    fun <T : Config> saveManual(name: String, folder: String = "", subfolder: String = "", configClass: T) {
        ConfigApiImpl.save(name, folder, subfolder, configClass)
    }

    /**
     * Overload of [save] that automatically fills in name, folder, and subfolder from the config itself.
     * @param T subclass of [Config]
     * @param configClass instance of T to be saved
     * @sample [me.fzzyhmstrs.fzzy_config.examples.MyConfig.saveMe]
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmStatic
    fun <T : Config> save(configClass: T) {
        ConfigApiImpl.save(configClass)
    }

    /**
     * Opens a config GUI
     *
     * Configs to be opened must be registered to the [ClientConfigRegistry][me.fzzyhmstrs.fzzy_config.registry.ClientConfigRegistry] via [registerConfig] or [registerAndLoadConfig]
     * @param scope the scope of the config screen to be opened. To open a selection screen of every config in a mod namespace, pass that namespace.
     * @sample [me.fzzyhmstrs.fzzy_config.examples.ConfigGuiOpener]
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @Environment(EnvType.CLIENT)
    fun openScreen(scope: String){
        ConfigApiImpl.openScreen(scope)
    }

    /**
     * Serialize a config class to a TomlElement
     *
     * Custom serializer, powered by TomlKt. Serialization occurs in two ways
     * 1) [EntrySerializer] elements are serialized with their custom `serialize` method
     * 2) "Raw" properties and fields are serialized with the TomlKt by-class-type serialization.
     *
     * Will serialize the available TomlAnnotations, for use in proper formatting, comment generation, etc.
     * - [TomlHeaderComment] and [Version]: Will add top-of-file comments above the
     * - [TomlComment]: Adds a comment to the Toml file output. Accepts single line ("..") or multi-line ("""..""") comments
     * - [TomlBlockArray]: marks a list or other array object as a Block Array (Multi-line list). Default items per line is 1
     * - [TomlInline]: Marks that the annotated table or array element should be serialized as one line. Overrides TomlBlockArray
     * - [TomlMultilineString]: Marked string parses to file as a multi line string
     *
     * Should be called as a matched pair to [deserializeFromToml]. Ex: if ignoreNonSync is false on one end, it needs to be false on the other.
     *
     * @param T Type of the config to serialize. Can be any Non-Null type.
     * @param config the config instance to serialize from
     * @param errorBuilder the error list. error messages are appended to this for display after the serialization call
     * @param ignoreNonSync default true. If false, elements with the [NonSync] annotation will be skipped. Use true to serialize the entire config (ex: saving to file), use false for syncing (ex: initial sync server -> client)
     * @return Returns a [TomlElement] of the serialized config
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmStatic
    fun <T: Any> serializeToToml(config: T, errorBuilder: MutableList<String>, ignoreNonSync: Boolean = true): TomlElement{
        return ConfigApiImpl.serializeToToml(config, errorBuilder, ignoreNonSync)
    }

    /**
     * Serializes a config class to a string.
     *
     * Extension of [serializeToToml] that takes the additional step of encoding to string. Use to write to a file or packet.
     *
     * @param T Type of the config to serialize. Can be any Non-Null type.
     * @param config the config instance to serialize from
     * @param errorBuilder the error list. error messages are appended to this for display after the serialization call
     * @param ignoreNonSync default true. If false, elements with the [NonSync] annotation will be skipped. Use true to serialize the entire config (ex: saving to file), use false for syncing (ex: initial sync server -> client)
     * @return Returns a [TomlElement] of the serialized config
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmStatic
    fun <T: Any> serializeConfig(config: T, errorBuilder: MutableList<String>, ignoreNonSync: Boolean = true): String{
        return ConfigApiImpl.serializeConfig(config, errorBuilder, ignoreNonSync)
    }

    /**
     * Deserializes a config class from a TomlElement
     *
     * Custom deserializer, powered by TomlKt. Deserialization focuses on validation and building a useful error message. Deserialization happens in two ways
     * 1) [EntrySerializer] elements are deserialized with their custom `deserialize` method
     * 2) "Raw" properties and fields are deserialized with the TomlKt by-class-type deserialization.
     *
     * Configs are deserialized "in place". That is to say, the deserializer iterates over the relevant field and properties of a pre-instantiated "default" config class. Each relevant field/property is filled in with the results of deserializing from the TomlElement at the matching TomlTable key. If for some reason there is a critical error, the initial config passed in, with whatever deserialization was successfully completed, will be returned as a fallback.
     *
     * Should be called as a matched pair to [serializeToToml]. Ex: if ignoreNonSync is false on one end, it needs to be false on the other.
     *
     * @param T the config type. Can be any Non-Null type.
     * @param config the config pre-deserialization
     * @param toml the TomlElement to deserialize from. Needs to be a TomlTable
     * @param errorBuilder a mutableList of strings the original caller of deserialization can use to print a detailed error log
     * @param ignoreNonSync default true. If false, elements with the [NonSync] annotation will be skipped. Use true to deserialize the entire config (ex: loading from file), use false for syncing (ex: initial sync server -> client)
     * @return Returns a [ValidationResult] with the config included, and any error message if applicable
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmStatic
    fun <T: Any> deserializeFromToml(config: T, toml: TomlElement, errorBuilder: MutableList<String>, ignoreNonSync: Boolean = true): ValidationResult<T> {
        return ConfigApiImpl.deserializeFromToml(config, toml, errorBuilder, ignoreNonSync)
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
     * @return Returns a [Pair]: [ValidationResult] and [Int].The validation result includes the config and any applicable errors, the int deserializes the [Version] of the file
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmStatic
    fun <T: Any> deserializeConfig(config: T, string: String, errorBuilder: MutableList<String>, ignoreNonSync: Boolean = true): Pair<ValidationResult<T>,Int> {
        return ConfigApiImpl.deserializeConfig(config, string, errorBuilder, ignoreNonSync)
    }

    /**
     * Creates a config directory keyed off the standard Fabric Config Directory
     *
     * Used to create a directory in the config parent directory inside the .minecraft folder. If the directory can't be created, the right member of the returning Pair will be false.
     *
     * @param folder the base folder for created directories. Should be considered analogous to the 'namespace' for a mod using this lib
     * @param subfolder subfolders for specific configs. Will typically be blank
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