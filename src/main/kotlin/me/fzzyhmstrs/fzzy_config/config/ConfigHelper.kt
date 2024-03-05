@file:Suppress("unused")

package me.fzzyhmstrs.fzzy_config.config

import kotlinx.serialization.encodeToString
import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.annotations.NonSync
import me.fzzyhmstrs.fzzy_config.annotations.TomlHeaderComment
import me.fzzyhmstrs.fzzy_config.annotations.Version
import me.fzzyhmstrs.fzzy_config.config.ConfigHelper.deserializeFromToml
import me.fzzyhmstrs.fzzy_config.impl.ConfigHelperImpl
import me.fzzyhmstrs.fzzy_config.interfaces.DirtyMarkable
import me.fzzyhmstrs.fzzy_config.interfaces.DirtySerializable
import me.fzzyhmstrs.fzzy_config.interfaces.FzzySerializable
import net.fabricmc.loader.api.FabricLoader
import net.peanuuutz.tomlkt.*
import java.io.File
import java.lang.reflect.Modifier
import java.lang.reflect.Modifier.isTransient
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

/**
 * Helper object for de/serialization of config files.
 *
 * De/serialized from File, string, or raw TomlElement. File read is performed with validation and correction.
 *
 * @author fzzyhmstrs
 * @since 0.1.0
 */
object ConfigHelper {

    /**
    * Registers a [Config] to the [SyncedConfigRegistry]. Use if you have custom initialization to perform.
    * 
    * Configs registered this way still have to handle their own initialization. That is to say, they have to be instantiated and passed to the registry in a timely manner, otherwise they will not be loaded in time for CONFIGURATION stage syncing with clients.
    *
    * Loading with the Fabric [ModInitializer] is a convenient and typical way to achieve this.
    * @param T the config type, any subclass of [Config]
    * @param config the config to register
    * @author fzzyhmstrs
    * @since 0.2.0
    */
    @JvmStatic
    fun <T: Config> registerConfig(config: T): T{
        SyncedConfigRegistry.register(Identifier(config.folder, config.name), config)
        return config
    }

    /**
    * Creates and registers a Config.
    * Performs the entire creation, loading, validation, and registration process on a config class. Internally performs the two steps
    * 1) [readOrCreateAndValidate]
    * 2) [registerConfig]
    * @param T the config type, any subclass of [Config]
    * @param configClass supplier of T
    * @return loaded, validated, and registered instance of T
    * @author fzzyhmstrs
    * @since 0.2.0
    */
    @JvmStatic
    fun <T:Config> registerAndLoadConfig(configClass: () -> T): T{
        return registerConfig(readOrCreateAndValidate(configClass))
    }

    /**
     * Reads in from File or Creates a new config class, and writes out any corrections, updates, or new content to File.
     *
     * Config Class and File generator with [Version] updating support, automatic validation and correction, and detailed error reporting. Use this to generate the actual config class instance to be used in-game. See the Example Config for typical usage case.
     *
     * @param T The config class type. Must be a subclass of [Config]
     * @param name String. The config name, will become the file name. In an identifier, would be the "path"
     * @param folder String, optional. A base config folder name. If left out, will write to the main config directory (not recommended). In an Identifier, this would be the "namespace"
     * @param subfolder String, optional. A subfolder name if desired. By default, blank. The file will appear in the base "namespace" folder if no child is given.
     * @param configClass () -> T. A provider of instances of the config class itself. In Kotlin this can typically be written like `{ MyConfigClass() }`
     * @return An instance of the configClass passed to it, updated and validated or passed back as-is, depending on circumstances and errors encountered
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmStatic
    fun <T: Config> readOrCreateAndValidate(name: String, folder: String = "", subfolder: String = "", configClass: () -> T): T{
        //wrap entire method in a try-catch. don't need to have config oopsies causing a hard crash, just fall back
        try {
            //create our directory, or bail if we can't for some reason
            val (dir,dirCreated) = makeDir(folder, subfolder)
            if (!dirCreated) {
                FC.LOGGER.error("Failed to create directory [${if(subfolder.isNotEmpty())"./$folder/$subfolder" else "./$folder"}]. Using default config for [$name].")
                return configClass()
            }
            //create our file
            val f = File(dir, name)
            if (f.exists()) {
                val fErrorsIn = mutableListOf<String>()
                val str = f.readLines().joinToString("\n")
                val classInstance = configClass()
                val classVersion = ConfigHelperImpl.getVersion(classInstance::class)
                val (readConfigResult, readVersion) = deserializeConfig(classInstance, str, fErrorsIn)
                val readConfig = readConfigResult.get()
                val needsUpdating = classVersion > readVersion
                if (readConfigResult.isError()){
                    readConfigResult.writeWarning(fErrorsIn)
                    val fErrorsOut = mutableListOf<String>()
                    if (needsUpdating){
                        readConfig.update(readVersion)
                    }
                    val correctedConfig = serializeConfig(readConfig,fErrorsOut)
                    if (fErrorsOut.isNotEmpty()){
                        val fErrorsOutResult = ValidationResult.error(true, "Critical error(s) encountered while re-serializing corrected Config Class! Output may not be complete.")
                        fErrorsOutResult.writeError(fErrorsOut)
                    }
                    f.writeText(correctedConfig)
                }
                if (needsUpdating){
                    readConfig.update(readVersion)
                    val fErrorsOut = mutableListOf<String>()
                    val updatedConfig = serializeConfig(readConfig,fErrorsOut)
                    if (fErrorsOut.isNotEmpty()){
                        val fErrorsOutResult = ValidationResult.error(true, "Critical error(s) encountered while re-serializing updated Config Class! Output may not be complete.")
                        fErrorsOutResult.writeError(fErrorsOut)
                    }
                    f.writeText(updatedConfig)
                }
                return readConfig
            } else if (!f.createNewFile()){
                FC.LOGGER.error("Couldn't create new file for config [$name]. Using default config.")
                return configClass()
            } else {
                val classInstance = configClass()
                val fErrorsOut = mutableListOf<String>()
                val serializedConfig = serializeConfig(classInstance,fErrorsOut)
                if (fErrorsOut.isNotEmpty()){
                    val fErrorsOutResult = ValidationResult.error(true, "Critical error(s) encountered while re-serializing corrected Config Class! Output may not be complete.")
                    fErrorsOutResult.writeError(fErrorsOut)
                }
                f.writeText(serializedConfig)
                return classInstance
            }
        } catch (e: Exception){
            FC.LOGGER.error("Critical error encountered while reading or creating [$name]. Using default config.")
            e.printStackTrace()
            return configClass()
        }
    }

    /**
     * overload of [readOrCreateAndValidate] that automatically applies the name, folder, and subfolder from the config itself.
     *
     * @param T type of config being created. Any subclass of [Config]
     * @param configClass supplier of T
     * @return An instance of the configClass passed to it, updated and validated or passed back as-is, depending on circumstances and errors encountered
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmStatic
    fun <T: Config> readOrCreateAndValidate(configClass: () -> T): T{
        val tempInstance = configClass()
        return readOrCreateAndValidate(tempInstance.name,tempInstance.folder,tempInstance.subfolder, configClass)
    }

    /**
     * Saves a config to file without reading or updating.
     *
     * Performs a config save. Use after a Mod is updated via gui and/or command. Does not perform any validation or reading/deleting of old or redundant files.
     */
    @JvmStatic
    fun <T : Config> save(name: String, folder: String = "", subfolder: String = "", configClass: T) {
        try {
            val (dir,dirCreated) = makeDir(folder, subfolder)
            if (!dirCreated) {
                return
            }
            val f = File(dir, name)
            if (f.exists()) {
                val fErrorsOut = mutableListOf<String>()
                val str = serializeConfig(configClass, fErrorsOut)
                if (fErrorsOut.isNotEmpty()){
                    val fErrorsOutResult = ValidationResult.error(true, "Critical error(s) encountered while saving updated Config Class! Output may not be complete.")
                    fErrorsOutResult.writeError(fErrorsOut)
                }
                f.writeText(str)
            } else if (!f.createNewFile()) {
                FC.LOGGER.error("Failed to open config file ($name), config not saved.")
            } else {
                val fErrorsOut = mutableListOf<String>()
                val str = serializeConfig(configClass, fErrorsOut)
                if (fErrorsOut.isNotEmpty()){
                    val fErrorsOutResult = ValidationResult.error(true, "Critical error(s) encountered while saving new Config Class! Output may not be complete.")
                    fErrorsOutResult.writeError(fErrorsOut)
                }
                f.writeText(str)
            }
        } catch (e: Exception) {
            FC.LOGGER.error("Failed to save config file $name!")
            e.printStackTrace()
        }
    }

    /**
     * Overload of [save] that automatically fills in name, folder, and subfolder from the config itself.
     */
    @JvmStatic
    fun <T : Config> save(configClass: T) {
        save(configClass.name,configClass.folder,configClass.subfolder, configClass)
    }

    /**
     * Serialize a config class to a TomlElement
     *
     * Custom serializer, powered by TomlKt. Serialization occurs in two ways
     * 1) [FzzySerializable] elements are serialized with their custom `serialize` method
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
        //used to build a TOML table piece by piece
        val toml = TomlTableBuilder()
        val version = ConfigHelperImpl.getVersion(config::class)
        val headerAnnotations = ConfigHelperImpl.tomlHeaderAnnotations(config::class).toMutableList()
        headerAnnotations.add(TomlHeaderComment("Don't change this! Version used to track needed updates."))
        toml.element("version", TomlLiteral(version),headerAnnotations.map { TomlComment(it.text) })
        try {
            //java fields are ordered in declared order, apparently not so for Kotlin properties. use these first to get ordering. skip Transient
            val fields = config::class.java.declaredFields.filter { !isTransient(it.modifiers) }
            //generate an index map, so I can order the properties based on name
            val orderById = fields.withIndex().associate { it.value.name to it.index }
            //kotlin member properties filtered by [field map contains it && if NonSync matters, it isn't NonSync]. NonSync does not matter by default
            for (it in config.javaClass.kotlin.memberProperties.filter {
                orderById.containsKey(it.name)
                && if (ignoreNonSync) true else !ConfigHelperImpl.isNonSync(it)
            }.sortedBy { orderById[it.name] }) {
                //has to be a public mutable property. private and protected and vals another way to have serialization ignore
                if (it is KMutableProperty<*> && it.visibility == KVisibility.PUBLIC) {
                    //get the actual [thing] from the property
                    val propVal = it.get(config)
                    //things name
                    val name = it.name
                    //serialize the element. FzzySerializable elements will have a set serialization method
                    val el = if (propVal is FzzySerializable) {
                        try {
                            propVal.serialize(errorBuilder, ignoreNonSync)
                        } catch (e: Exception) {
                            errorBuilder.add("Problem encountered with serialization of [$name]: ${e.localizedMessage}")
                            TomlNull
                        }
                        //fallback is to use by-type TOML serialization
                    } else if (propVal != null) {
                        try {
                            ConfigHelperImpl.encodeToTomlElement(propVal, it.returnType) ?: TomlNull
                        } catch (e: Exception) {
                            errorBuilder.add("Problem encountered with raw data during serialization of [$name]: ${e.localizedMessage}")
                            TomlNull
                        }
                        //TomlNull for properties with Null state (improper state, no config values should be nullable)
                    } else {
                        errorBuilder.add("Property [$name] was null during serialization!")
                        TomlNull
                    }
                    //scrape all the TomlAnnotations associated
                    val tomlAnnotations = ConfigHelperImpl.tomlAnnotations(it)
                    //add the element to the TomlTable, with annotations
                    toml.element(name, el, tomlAnnotations)
                }
            }
        } catch (e: Exception){
            errorBuilder.add("Critical error encountered while serializing config!: ${e.localizedMessage}")
            return toml.build()
        }
        //serialize the TomlTable to its string representation
        return toml.build()
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
        return Toml.encodeToString(serializeToToml(config,errorBuilder,ignoreNonSync))
    }

    /**
     * serializes `dirty` elements in a Config for syncing with a server or secondary clients.
     *
     * FzzyConfig has a system for marking changes made as `dirty`, and only resynchronizing elements that were actually changed. This method is used internally by [DirtySerializable]
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
    fun <T: Any> serializeDirtyToToml(config: T, errorBuilder: MutableList<String>, ignoreNonSync: Boolean = true): TomlElement{
        //used to build a TOML table piece by piece
        val toml = TomlTableBuilder()
        try {
            //kotlin member properties filtered by [field map contains it && if NonSync matters, it isn't NonSync]. NonSync does not matter by default
            for (it in config.javaClass.kotlin.memberProperties.filter {
                !isTransient(it.javaField?.modifiers ?: Modifier.TRANSIENT)
                && if (ignoreNonSync) true else !ConfigHelperImpl.isNonSync(it)
                && it.visibility == KVisibility.PUBLIC
            }) {
                //has to be a public mutable property. private and protected and vals another way to have serialization ignore
                if (it is KMutableProperty<*> && it.visibility == KVisibility.PUBLIC) {
                    //get the actual [thing] from the property
                    val propVal = it.get(config)
                    //thing needs to be markable as `dirty`
                    if (propVal !is DirtyMarkable) continue
                    //not dirty? ignore
                    if (!propVal.isDirty()) continue
                    //things name
                    val name = it.name
                    val el = when (propVal) {
                        is DirtySerializable -> { //DirtySerializable gets priority.
                            try {
                                propVal.serializeDirty(errorBuilder, ignoreNonSync)
                            } catch (e: Exception) {
                                errorBuilder.add("Problem encountered with serialization of [$name]: ${e.localizedMessage}")
                                TomlNull
                            }
                        }
                        is FzzySerializable -> { // next a plain FzzySerializable will get entirely serialized
                            try {
                                propVal.serialize(errorBuilder, ignoreNonSync)
                            } catch (e: Exception) {
                                errorBuilder.add("Problem encountered with serialization of [$name]: ${e.localizedMessage}")
                                TomlNull
                            }
                        }
                        else -> {
                            errorBuilder.add("Element marked as Dirty, but not serializable: [$name]")
                            continue
                        }
                    }
                    //scrape all the TomlAnnotations associated
                    val tomlAnnotations = ConfigHelperImpl.tomlAnnotations(it)
                    //add the element to the TomlTable, with annotations
                    toml.element(name, el, tomlAnnotations)
                }
            }
        } catch (e: Exception){
            errorBuilder.add("Critical error encountered while serializing config!: ${e.localizedMessage}")
            return toml.build()
        }
        //serialize the TomlTable to its string representation
        return toml.build()
    }

    @JvmStatic
    fun <T: Any> serializeDirty(config: T, errorBuilder: MutableList<String>, ignoreNonSync: Boolean = true): String{
        return Toml.encodeToString(serializeDirtyToToml(config,errorBuilder,ignoreNonSync))
    }


    /**
     * Deserializes a config class from a TomlElement
     *
     * Custom deserializer, powered by TomlKt. Deserialization focuses on validation and building a useful error message. Deserialization happens in two ways
     * 1) [FzzySerializable] elements are deserialized with their custom `deserialize` method
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
        val inboundErrorSize = errorBuilder.size
        if (toml !is TomlTable) {
            errorBuilder.add("TomlElement passed not a TomlTable! Using default Config")
            return ValidationResult.error(config,"Improper TOML format passed to deserializeFromToml")
        }
        try {
            val fields = config::class.java.declaredFields.filter { !isTransient(it.modifiers) }
            val orderById = fields.withIndex().associate { it.value.name to it.index }
            for (it in config.javaClass.kotlin.memberProperties.filter {
                orderById.containsKey(it.name)
                && if (ignoreNonSync) true else !ConfigHelperImpl.isNonSync(it)
            }.sortedBy { orderById[it.name] }) {
                if (it is KMutableProperty<*> && it.visibility == KVisibility.PUBLIC) {
                    val propVal = it.get(config)
                    val name = it.name
                    val tomlElement = if (toml.containsKey(name)) {
                        toml[name]
                    } else {
                        errorBuilder.add("Key [$name] not found in TOML file.")
                        continue
                    }
                    if (tomlElement == null || tomlElement is TomlNull) {
                        errorBuilder.add("TomlElement [$name] was null!.")
                        continue
                    }
                    if (propVal is FzzySerializable) {
                        val result = propVal.deserialize(tomlElement,errorBuilder, name, ignoreNonSync)
                        if (result.isError()) {
                            errorBuilder.add(result.getError())
                        }
                    } else {
                        try {
                            it.setter.call(
                                config,
                                ConfigHelperImpl.decodeFromTomlElement(tomlElement, it.returnType).also { println(it) })
                        } catch (e: Exception) {
                            errorBuilder.add("Error deserializing raw field [$name]: ${e.localizedMessage}")
                        }
                    }
                }
            }
        } catch (e: Exception){
            errorBuilder.add("Critical error encountered while deserializing")
        }
        return if (inboundErrorSize == errorBuilder.size) {
            ValidationResult.success(config)
        } else {
            ValidationResult.error(config, "Errors found while deserializing Config ${config.javaClass.canonicalName}!")
        }
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
        val toml = try {
            Toml.parseToTomlTable(string)
        } catch (e:Exception){
            return  Pair(ValidationResult.error(config, "Config ${config.javaClass.canonicalName} is corrupted or improperly formatted for parsing"),0)
        }
        val version = if(toml.containsKey("version")){
            try {
                toml["version"]?.asTomlLiteral()?.toInt() ?: 0
            }catch (e: Exception){
                0
            }
        } else {
            0
        }
        return Pair(deserializeFromToml(config, toml, errorBuilder, ignoreNonSync), version)
    }

    /**
     * Deserializes received dirty-only config from [TomlElement].
     *
     * FzzyConfig has a system for marking changes made as `dirty`, and only resynchronizing elements that were actually changed. This method is used internally by [DirtySerializable]
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
    fun <T: Any> deserializeDirtyFromToml(config: T, toml: TomlElement, errorBuilder: MutableList<String>, ignoreNonSync: Boolean = false): ValidationResult<T> {
        val inboundErrorSize = errorBuilder.size
        if (toml !is TomlTable) {
            errorBuilder.add("TomlElement passed not a TomlTable! Using default Config")
            return ValidationResult.error(config,"Improper TOML format passed to deserializeDirtyFromToml")
        }
        try {
            val propMap = config.javaClass.kotlin.memberProperties.filter {
                !isTransient(it.javaField?.modifiers ?: Modifier.TRANSIENT)
                        && toml.containsKey(it.name)
                        && if (ignoreNonSync) true else !ConfigHelperImpl.isNonSync(it)
                        && it is KMutableProperty<*>
                        && it.visibility == KVisibility.PUBLIC
            }.associateBy { it.name }
            for (key in toml.keys){
                if (!propMap.containsKey(key)){
                    errorBuilder.add("TomlTable sent for deserialization includes key not present in receiver class!: [$key]")
                }
            }
            for ((key, tomlElement) in toml.entries) {
                val it = propMap[key]
                if (it == null || it !is KMutableProperty<*>){
                    errorBuilder.add("Immutable or mismatched/missing property found in sync of Dirty Data from key [$key]: ignoreNonSync: $ignoreNonSync")
                    continue
                }
                val propVal = it.get(config)
                val name = it.name
                if (propVal is DirtySerializable) {
                    val result = propVal.deserializeDirty(tomlElement,errorBuilder, name, ignoreNonSync)
                    if (result.isError()) {
                        errorBuilder.add(result.getError())
                    }
                } else if (propVal is DirtyMarkable && propVal is FzzySerializable) {
                    val result = propVal.deserialize(tomlElement,errorBuilder, name, ignoreNonSync)
                    if (result.isError()) {
                        errorBuilder.add(result.getError())
                    }
                } else {
                    errorBuilder.add("Property [$name] wasn't DirtySerializable or FzzySerializable")
                }
            }
        } catch (e: Exception){
            errorBuilder.add("Critical error encountered while deserializing")
        }
        return if (inboundErrorSize == errorBuilder.size) {
            ValidationResult.success(config)
        } else {
            ValidationResult.error(config, "Errors found while deserializing Config ${config.javaClass.canonicalName}!")
        }
    }

    /**
     * Deserializes the updated portions of a config from a string.
     *
     * Extension of [deserializeDirtyFromToml] that deserializes directly from a string. Use to read from a file or packet.
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
    fun <T: Any> deserializeDirty(config: T, string: String, errorBuilder: MutableList<String>, ignoreNonSync: Boolean = false): ValidationResult<T> {
        val toml = try {
            Toml.parseToTomlTable(string)
        } catch (e:Exception){
            return ValidationResult.error(
                config,
                "Config ${config.javaClass.canonicalName} is corrupted or improperly formatted for parsing"
            )
        }
        return deserializeDirtyFromToml(config, toml, errorBuilder, ignoreNonSync)
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
        val dir = if (subfolder != ""){
            File(File(FabricLoader.getInstance().configDir.toFile(), folder), subfolder)
        } else {
            if (folder != "") {
                File(FabricLoader.getInstance().configDir.toFile(), folder)
            } else {
                FabricLoader.getInstance().configDir.toFile()
            }
        }
        if (!dir.exists() && !dir.mkdirs()) {
            FC.LOGGER.error("Could not create directory.")
            return Pair(dir,false)
        }
        return Pair(dir,true)
    }

}
