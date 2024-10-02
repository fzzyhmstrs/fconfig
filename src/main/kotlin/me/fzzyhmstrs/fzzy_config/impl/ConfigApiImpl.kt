/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.impl

import blue.endless.jankson.Jankson
import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.serializer
import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.FCC
import me.fzzyhmstrs.fzzy_config.annotations.*
import me.fzzyhmstrs.fzzy_config.api.RegisterType
import me.fzzyhmstrs.fzzy_config.cast
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.config.ConfigAction
import me.fzzyhmstrs.fzzy_config.config.ConfigContext
import me.fzzyhmstrs.fzzy_config.config.ConfigContext.Keys.ACTIONS
import me.fzzyhmstrs.fzzy_config.config.ConfigContext.Keys.RESTART_RECORDS
import me.fzzyhmstrs.fzzy_config.config.ConfigContext.Keys.VERSIONS
import me.fzzyhmstrs.fzzy_config.config.ConfigSection
import me.fzzyhmstrs.fzzy_config.entry.Entry
import me.fzzyhmstrs.fzzy_config.entry.EntryDeserializer
import me.fzzyhmstrs.fzzy_config.entry.EntrySerializer
import me.fzzyhmstrs.fzzy_config.registry.SyncedConfigRegistry
import me.fzzyhmstrs.fzzy_config.updates.BasicValidationProvider
import me.fzzyhmstrs.fzzy_config.updates.UpdateManager
import me.fzzyhmstrs.fzzy_config.util.PlatformUtils
import me.fzzyhmstrs.fzzy_config.util.TomlOps
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.wrap
import me.fzzyhmstrs.fzzy_config.util.Walkable
import me.fzzyhmstrs.fzzy_config.validation.number.*
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.registry.BuiltinRegistries
import net.minecraft.registry.RegistryWrapper.WrapperLookup
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.MathHelper
import net.peanuuutz.tomlkt.*
import org.apache.logging.log4j.core.jmx.Server
import java.io.File
import java.lang.reflect.Modifier
import java.lang.reflect.Modifier.isTransient
import java.util.function.Supplier
import kotlin.experimental.and
import kotlin.math.min
import kotlin.experimental.or
import kotlin.reflect.*
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.superclasses
import kotlin.reflect.jvm.javaField

internal object ConfigApiImpl {

    private val isClient by lazy {
        PlatformUtils.isClient()
    }

    private var wrapperLookup: WrapperLookup? = null

    internal fun invalidateLookup() {
        this.wrapperLookup = null
    }

    internal fun getWrapperLookup(): WrapperLookup {
        return wrapperLookup ?: BuiltinRegistries.createWrapperLookup().also { wrapperLookup = it }
    }

    internal const val CHECK_NON_SYNC: Byte = 0
    internal const val IGNORE_NON_SYNC: Byte = 1
    internal const val CHECK_ACTIONS: Byte = 2
    internal const val IGNORE_NON_SYNC_AND_CHECK_RESTART: Byte = 3
    internal const val IGNORE_VISIBILITY: Byte = 4
    internal const val RECORD_RESTARTS: Byte = 8
    internal const val CHECK_ACTIONS_AND_RECORD_RESTARTS: Byte = 10

    internal fun openScreen(scope: String) {
        if (isClient)
            ConfigApiImplClient.openScreen(scope)
    }
    internal fun openRestartScreen() {
        if (isClient)
            FCC.openRestartScreen()
    }

    ///////////////////// Registration ///////////////////////////////////////////////////

    internal fun <T: Config> registerConfig(config: T, configClass: () -> T, registerType: RegisterType): T {
        return when(registerType) {
            RegisterType.BOTH -> registerBoth(config, configClass)
            RegisterType.SERVER -> registerSynced(config)
            RegisterType.CLIENT -> registerClient(config, configClass)
        }
    }

    private fun <T: Config> registerBoth(config: T, configClass: () -> T): T {
        SyncedConfigRegistry.registerConfig(config)
        return registerClient(config, configClass)
    }
    private fun <T: Config> registerSynced(config: T): T {
        SyncedConfigRegistry.registerConfig(config)
        return config
    }
    private fun <T: Config> registerClient(config: T, configClass: () -> T): T {
        if(isClient)
            ConfigApiImplClient.registerConfig(config, configClass())
        return config
    }

    internal fun <T: Config> registerAndLoadConfig(configClass: () -> T, registerType: RegisterType): T {
        return when(registerType) {
            RegisterType.BOTH -> registerAndLoadBoth(configClass)
            RegisterType.SERVER -> registerAndLoadSynced(configClass)
            RegisterType.CLIENT -> registerAndLoadClient(configClass)
        }
    }
    private fun <T: Config> registerAndLoadBoth(configClass: () -> T): T {
        return registerBoth(readOrCreateAndValidate(configClass), configClass)
    }
    private fun <T: Config> registerAndLoadSynced(configClass: () -> T): T {
        return registerSynced(readOrCreateAndValidate(configClass))
    }
    private fun <T: Config> registerAndLoadClient(configClass: () -> T): T {
        return registerClient(readOrCreateAndValidate(configClass), configClass)
    }

    ///////////////// Flags //////////////////////////////////////////////////////////////

    //////////////// Read, Create, Save //////////////////////////////////////////////////

    internal fun <T: Config> readOrCreateAndValidate(name: String, folder: String = "", subfolder: String = "", configClass: () -> T): T {
        //wrap entire method in a try-catch. don't need to have config problems causing a hard crash, just fall back
        try {
            //create our directory, or bail if we can't for some reason
            val (dir, dirCreated) = makeDir(folder, subfolder)
            if (!dirCreated) {
                FC.LOGGER.error("Failed to create directory [${if(subfolder.isNotEmpty())"./$folder/$subfolder" else "./$folder"}]. Using default config for [$name].")
                return configClass()
            }
            //create our file
            val f = File(dir, "$name.toml")
            if (f.exists()) {
                val fErrorsIn = mutableListOf<String>()
                val str = f.readLines().joinToString("\n")
                val classInstance = configClass()
                val classVersion = getVersion(classInstance::class)
                val readConfigResult = deserializeConfig(classInstance, str, fErrorsIn)
                val readVersion = readConfigResult.get().getInt(VERSIONS)
                val readConfig = readConfigResult.get().config
                val needsUpdating = classVersion > readVersion
                if (readConfigResult.isError()) {
                    readConfigResult.writeWarning(fErrorsIn)
                    val fErrorsOut = mutableListOf<String>()
                    if (needsUpdating) {
                        readConfig.update(readVersion)
                    }
                    val correctedConfig = serializeConfig(readConfig, fErrorsOut)
                    if (fErrorsOut.isNotEmpty()) {
                        val fErrorsOutResult = ValidationResult.error(
                            true,
                            "Critical error(s) encountered while re-serializing corrected Config Class! Output may not be complete."
                        )
                        fErrorsOutResult.writeError(fErrorsOut)
                    }
                    f.writeText(correctedConfig)
                } else if (needsUpdating) {
                    readConfig.update(readVersion)
                    val fErrorsOut = mutableListOf<String>()
                    val updatedConfig = serializeConfig(readConfig, fErrorsOut)
                    if (fErrorsOut.isNotEmpty()) {
                        val fErrorsOutResult = ValidationResult.error(
                            true,
                            "Critical error(s) encountered while re-serializing updated Config Class! Output may not be complete."
                        )
                        fErrorsOutResult.writeError(fErrorsOut)
                    }
                    f.writeText(updatedConfig)
                }
                return readConfig
            } else if (!f.createNewFile()) {
                FC.LOGGER.error("Couldn't create new file for config [$name]. Using default config.")
                return configClass()
            } else {
                val classInstance = configClass()
                val oldFilePair = getCompat(classInstance::class)
                val oldFile = oldFilePair.first
                if (oldFile != null && oldFile.exists()) {
                    if (oldFilePair.second) {
                        val str = oldFile.readLines().joinToString("\n")
                        val errorBuilder = mutableListOf<String>()
                        val convertedConfigResult = deserializeConfig(classInstance, str, errorBuilder)
                        if (convertedConfigResult.isError()) {
                            convertedConfigResult.writeWarning(errorBuilder)
                        }
                    } else {
                        try {
                            val builder = Jankson.builder().build()
                            val jsonObject = builder.load(oldFile)
                            val jsonString = jsonObject.toJson(false, false)
                            val tomlElement = JsonOps.INSTANCE.convertTo(TomlOps.INSTANCE, JsonParser.parseString(jsonString))
                            val errorBuilder = mutableListOf<String>()
                            val result = deserializeFromToml(classInstance, tomlElement, errorBuilder)
                            if (result.isError())
                                ValidationResult.error("", "Error(s) encountered while deserializing old config file into new format").writeWarning(errorBuilder)
                        } catch (e: Exception) {
                            FC.LOGGER.error("Error encountered while converting old config file")
                            e.printStackTrace()
                        }
                    }
                    oldFile.delete()
                }
                val fErrorsOut = mutableListOf<String>()
                val serializedConfig = serializeConfig(classInstance, fErrorsOut)
                if (fErrorsOut.isNotEmpty()) {
                    val fErrorsOutResult = ValidationResult.error(true, "Critical error(s) encountered while re-serializing corrected Config Class! Output may not be complete.")
                    fErrorsOutResult.writeError(fErrorsOut)
                }
                f.writeText(serializedConfig)
                return classInstance
            }
        } catch (e: Exception) {
            FC.LOGGER.error("Critical error encountered while reading or creating [$name]. Using default config.")
            e.printStackTrace()
            return configClass()
        }
    }

    internal fun <T: Config> readOrCreateAndValidate(configClass: () -> T): T {
        val tempInstance = configClass()
        return readOrCreateAndValidate(tempInstance.name, tempInstance.folder, tempInstance.subfolder, configClass)
    }

    internal fun <T : Config> save(name: String, folder: String = "", subfolder: String = "", configClass: T) {
        try {
            val (dir, dirCreated) = makeDir(folder, subfolder)
            if (!dirCreated) {
                return
            }
            val f = File(dir, "$name.toml")
            if (f.exists()) {
                val fErrorsOut = mutableListOf<String>()
                val str = serializeConfig(configClass, fErrorsOut)
                if (fErrorsOut.isNotEmpty()) {
                    val fErrorsOutResult = ValidationResult.error(
                        true,
                        "Critical error(s) encountered while saving updated Config Class! Output may not be complete."
                    )
                    fErrorsOutResult.writeError(fErrorsOut)
                }
                f.writeText(str)
            } else if (!f.createNewFile()) {
                FC.LOGGER.error("Failed to open config file ($name), config not saved.")
            } else {
                val fErrorsOut = mutableListOf<String>()
                val str = serializeConfig(configClass, fErrorsOut)
                if (fErrorsOut.isNotEmpty()) {
                    val fErrorsOutResult = ValidationResult.error(
                        true,
                        "Critical error(s) encountered while saving new Config Class! Output may not be complete."
                    )
                    fErrorsOutResult.writeError(fErrorsOut)
                }
                f.writeText(str)
            }
        } catch (e: Exception) {
            FC.LOGGER.error("Failed to save config file $name!")
            e.printStackTrace()
        }
    }

    internal fun <T : Config> save(configClass: T) {
        save(configClass.name, configClass.folder, configClass.subfolder, configClass)
    }

    ///////////////// END Read, Create, Save /////////////////////////////////////////////

    ///////////////// Serialize //////////////////////////////////////////////////////////

    internal fun <T: Any> serializeToToml(config: T, errorBuilder: MutableList<String>, flags: Byte = IGNORE_NON_SYNC): TomlTable {
        //used to build a TOML table piece by piece
        val toml = TomlTableBuilder()
        try {
            if (config is Config) {
                val version = getVersion(config::class)
                val headerAnnotations = tomlHeaderAnnotations(config::class).toMutableList()
                headerAnnotations.add(TomlHeaderComment("Don't change this! Version used to track needed updates."))
                toml.element("version", TomlLiteral(version), headerAnnotations.map { TomlComment(it.text) })
            }
            val ignoreVisibility = isIgnoreVisibility(config::class) || ignoreVisibility(flags)
            //java fields are ordered in declared order, apparently not so for Kotlin properties. use these first to get ordering. skip Transient
            val fields = config::class.java.declaredFields.filter { !isTransient(it.modifiers) }.toMutableList()
            for (sup in config::class.allSuperclasses) {
                if (sup == Config::class) continue //ignore Config itself, as that has state we don't need
                fields.addAll(sup.java.declaredFields.filter { !isTransient(it.modifiers) })
            }
            //generate an index map, so I can order the properties based on name
            val orderById = fields.withIndex().associate { it.value.name to it.index }
            //kotlin member properties filtered by [field map contains it && if NonSync matters, it isn't NonSync]. NonSync does not matter by default
            for (prop in config.javaClass.kotlin.memberProperties.filter {
                if (ignoreNonSync(flags)) true else !isNonSync(it)
                && if(ignoreVisibility) (it.javaField?.trySetAccessible() == true) else it.visibility == KVisibility.PUBLIC
            }.sortedBy { orderById[it.name] }) {
                //has to be a public mutable property. private and protected and val another way to have serialization ignore
                if (prop !is KMutableProperty<*>) continue
                //get the actual [thing] from the property
                if(ignoreVisibility) (prop.javaField?.trySetAccessible())
                val propVal = prop.get(config)
                if (propVal is ConfigAction) continue
                //if(ignoreVisibility) (prop.javaField?.trySetAccessible())
                //things name
                val name = prop.name
                //serialize the element. EntrySerializer elements will have a set serialization method
                val el = if (propVal is EntrySerializer<*>) { //is EntrySerializer
                    try {
                        propVal.serializeEntry(null, errorBuilder, flags)
                    } catch (e: Exception) {
                        errorBuilder.add("Problem encountered with serialization of [$name]: ${e.localizedMessage}")
                        TomlNull
                    }
                    //fallback is to use by-type TOML serialization
                } else if (propVal != null) {
                    val basicValidation = UpdateManager.basicValidationStrategy(propVal, prop.returnType, prop.annotations)
                    if (basicValidation != null) {
                        basicValidation.trySerialize(propVal, errorBuilder, flags) ?: TomlNull
                    } else {
                        try {
                            encodeToTomlElement(propVal, prop.returnType) ?: TomlNull
                        } catch (e: Exception) {
                            errorBuilder.add("Problem encountered with raw data during serialization of [$name]: ${e.localizedMessage}")
                            TomlNull
                        }
                    }
                    //TomlNull for properties with Null state (improper state, no config values should be nullable)
                } else {
                    errorBuilder.add("Property [$name] was null during serialization!")
                    TomlNull
                }
                //scrape all the TomlAnnotations associated
                val tomlAnnotations = tomlAnnotations(prop)
                //add the element to the TomlTable, with annotations
                toml.element(name, el, tomlAnnotations)

            }
        } catch (e: Exception) {
            errorBuilder.add("Critical error encountered while serializing config!: ${e.localizedMessage}")
            return toml.build()
        }
        //serialize the TomlTable to its string representation
        return toml.build()
    }

    internal fun <T: Any> serializeConfig(config: T, errorBuilder: MutableList<String>, flags: Byte = IGNORE_NON_SYNC): String {
        return Toml.encodeToString(serializeToToml(config, errorBuilder, flags))
    }

    private fun <T: Config, M> serializeUpdateToToml(config: T, manager: M, errorBuilder: MutableList<String>, flags: Byte = CHECK_NON_SYNC): TomlTable where M: UpdateManager, M: BasicValidationProvider {
        val toml = TomlTableBuilder()
        try {
            walk(config, config.getId().toTranslationKey(), flags) { _, _, str, v, prop, annotations, _, _ ->
                if(manager.hasUpdate(str)) {
                    if(v is EntrySerializer<*>) {
                        toml.element(str, v.serializeEntry(null, errorBuilder, flags))
                    } else if (v != null) {
                        val basicValidation = manager.basicValidationStrategy(v, prop.returnType, annotations)
                        if (basicValidation != null) {
                            val el = basicValidation.trySerialize(v, errorBuilder, flags)
                            if (el != null)
                                toml.element(str, el)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            errorBuilder.add("Critical error encountered while serializing config update!: ${e.localizedMessage}")
            return toml.build()
        }
        return toml.build()
    }

    internal fun <T: Config, M> serializeUpdate(config: T, manager: M, errorBuilder: MutableList<String>, flags: Byte = CHECK_NON_SYNC): String where M: UpdateManager, M: BasicValidationProvider {
        return Toml.encodeToString(serializeUpdateToToml(config, manager, errorBuilder, flags))
    }

    internal fun serializeEntry(entry: Entry<*, *>, errorBuilder: MutableList<String>, flags: Byte = IGNORE_NON_SYNC): String {
        val toml = TomlTableBuilder()
        toml.element("entry", entry.serializeEntry(null, errorBuilder, flags))
        return Toml.encodeToString(toml.build())
    }

    ///////////////// END Serialize //////////////////////////////////////////////////////

    ///////////////// Deserialize ////////////////////////////////////////////////////////

    internal fun <T: Any> deserializeFromToml(config: T, toml: TomlElement, errorBuilder: MutableList<String>, flags: Byte = IGNORE_NON_SYNC): ValidationResult<ConfigContext<T>> {
        val inboundErrorSize = errorBuilder.size
        val restartNeeded: MutableSet<Action> = mutableSetOf()
        val restartRecords: MutableSet<String> = mutableSetOf()
        if (toml !is TomlTable) {
            errorBuilder.add("TomlElement passed not a TomlTable! Using default Config")
            return ValidationResult.error(ConfigContext(config).withContext(ACTIONS, mutableSetOf<Action>()), "Improper TOML format passed to deserializeFromToml")
        }
        try {
            val checkActions = checkActions(flags)
            val recordRestarts = if (!checkActions) false else recordRestarts(flags)
            val globalAction = getAction(config::class.annotations)
            val ignoreVisibility = isIgnoreVisibility(config::class) || ignoreVisibility(flags)

            val fields = config::class.java.declaredFields.filter { !isTransient(it.modifiers) }.toMutableList()
            for (sup in config::class.allSuperclasses) {
                if (sup == Config::class) continue
                fields.addAll(sup.java.declaredFields.filter { !isTransient(it.modifiers) })
            }

            println(fields.map { it.name })
            val orderById = fields.withIndex().associate { it.value.name to it.index }
            for (prop in config.javaClass.kotlin.memberProperties.filter {
                if (ignoreNonSync(flags)) true else !isNonSync(it)
                && if(ignoreVisibility) (it.javaField?.trySetAccessible() == true) else it.visibility == KVisibility.PUBLIC
            }.sortedBy { orderById[it.name] }) {
                if (prop !is KMutableProperty<*>) continue
                if(ignoreVisibility) (prop.javaField?.trySetAccessible())
                val propVal = prop.get(config)
                if (propVal is ConfigAction) continue
                val name = prop.name
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
                if (propVal is EntryDeserializer<*>) { //is EntryDeserializer
                    val action = requiredAction(prop.annotations, globalAction)
                    val result = if(checkActions && propVal is Supplier<*> && action != null) {
                        val before = propVal.get()
                        propVal.deserializeEntry(tomlElement, errorBuilder, name, flags).also { r ->
                            if(r.get() != before) {
                                restartNeeded.add(action)
                                if(recordRestarts && action.restartPrompt) {
                                    restartRecords.add(((config as? Config)?.getId()?.toTranslationKey() ?: "") + "." + name)
                                }
                            }
                        }
                    } else {
                        propVal.deserializeEntry(tomlElement, errorBuilder, name, flags)
                    }
                    if (result.isError()) {
                        errorBuilder.add(result.getError())
                    }
                } else {
                    val basicValidation = UpdateManager.basicValidationStrategy(propVal, prop.returnType, prop.annotations)
                    if (basicValidation != null) {
                        @Suppress("DEPRECATION")
                        val thing = basicValidation.deserializeEntry(tomlElement, errorBuilder, name, flags)
                        try {
                            val action = requiredAction(prop.annotations, globalAction)
                            if(checkActions && action != null)
                                if (propVal != thing.get()) {
                                    restartNeeded.add(action)
                                    if(recordRestarts && action.restartPrompt) {
                                        restartRecords.add(((config as? Config)?.getId()?.toTranslationKey() ?: "") + "." + name)
                                    }
                                }
                            if(ignoreVisibility) (prop.javaField?.trySetAccessible())
                            prop.setter.call(config, thing.get())
                        } catch(e: Exception) {
                            errorBuilder.add("Error deserializing basic validation [$name]: ${e.localizedMessage}")
                        }
                    } else {
                        try {
                            val action = requiredAction(prop.annotations, globalAction)
                            if(checkActions && action != null) {
                                if(ignoreVisibility) (prop.javaField?.trySetAccessible())
                                prop.setter.call(config, validateNumber(decodeFromTomlElement(tomlElement, prop.returnType), prop).also{
                                    if (propVal != it) {
                                        restartNeeded.add(action)
                                        if(recordRestarts && action.restartPrompt) {
                                            restartRecords.add(((config as? Config)?.getId()?.toTranslationKey() ?: "") + "." + name)
                                        }
                                    }
                                })
                            } else {
                                if(ignoreVisibility) (prop.javaField?.trySetAccessible())
                                prop.setter.call(config, validateNumber(decodeFromTomlElement(tomlElement, prop.returnType), prop))
                            }
                        } catch (e: Exception) {
                            errorBuilder.add("Error deserializing raw field [$name]: ${e.localizedMessage}")
                        }
                    }
                }

            }
        } catch (e: Exception) {
            errorBuilder.add("Critical error encountered while deserializing")
        }
        return ValidationResult.predicated(ConfigContext(config).withContext(ACTIONS, restartNeeded).withContext(RESTART_RECORDS, restartRecords), errorBuilder.size <= inboundErrorSize, "Errors found while deserializing Config ${config.javaClass.canonicalName}!")
    }

    internal fun <T: Any> deserializeConfig(config: T, string: String, errorBuilder: MutableList<String>, flags: Byte = IGNORE_NON_SYNC): ValidationResult<ConfigContext<T>> {
        val toml = try {
            Toml.parseToTomlTable(string)
        } catch (e:Exception) {
            return  ValidationResult.error(ConfigContext(config), "Config ${config.javaClass.canonicalName} is corrupted or improperly formatted for parsing")
        }
        val version = if(toml.containsKey("version")) {
            try {
                toml["version"]?.asTomlLiteral()?.toInt() ?: 0
            }catch (e: Exception) {
                -1 //error state, pass back non-valid version number
            }
        } else {
            -1 //error state, pass back non-valid version number
        }
        return deserializeFromToml(config, toml, errorBuilder, flags).let { it.wrap(it.get().withContext(VERSIONS ,version)) }
    }


    private fun <T: Any> deserializeUpdateFromToml(config: T, toml: TomlElement, errorBuilder: MutableList<String>, flags: Byte = CHECK_NON_SYNC): ValidationResult<ConfigContext<T>> {
        val inboundErrorSize = errorBuilder.size
        val actionsNeeded: MutableSet<Action> = mutableSetOf()
        val restartRecords: MutableSet<String> = mutableSetOf()
        try {
            val checkActions = checkActions(flags)
            val recordRestarts = if (!checkActions) false else recordRestarts(flags)
            val globalAction = getAction(config::class.annotations)

            if (toml !is TomlTable) {
                errorBuilder.add("TomlElement passed not a TomlTable! Using default Config")
                return ValidationResult.error(ConfigContext(config), "Improper TOML format passed to deserializeDirtyFromToml")
            }
            walk(config, (config as? Config)?.getId()?.toTranslationKey() ?: "", flags) { _, _, str, v, prop, annotations, _, _ -> toml[str]?.let {
                if(v is EntryDeserializer<*>) {
                    val action = requiredAction(prop.annotations, globalAction)
                    if(checkActions && v is Supplier<*> && action != null) {
                        val before = v.get()
                        v.deserializeEntry(it, errorBuilder, str, flags).also { r ->
                            if(r.get() != before) {
                                actionsNeeded.add(action)
                                if(recordRestarts && action.restartPrompt) {
                                    restartRecords.add(str)
                                }
                            }
                        }
                    } else {
                        v.deserializeEntry(it, errorBuilder, str, flags)
                    }
                } else if (v != null) {
                    val basicValidation = UpdateManager.basicValidationStrategy(v, prop.returnType, annotations)
                    if (basicValidation != null) {
                        @Suppress("DEPRECATION")
                        val thing = basicValidation.deserializeEntry(it, errorBuilder, str, flags)
                        try {
                            val action = requiredAction(prop.annotations, globalAction)
                            if(checkActions && action != null)
                                if (v != thing.get()) {
                                    actionsNeeded.add(action)
                                    if(recordRestarts && action.restartPrompt) {
                                        restartRecords.add(str)
                                    }
                                }
                            prop.setter.call(config, thing.get()) //change?
                        } catch(e: Exception) {
                            errorBuilder.add("Error deserializing basic validation [$str]: ${e.localizedMessage}")
                        }
                    }
                }
            }
            }
        } catch(e: Exception) {
            errorBuilder.add("Critical error encountered while deserializing update")
        }
        return ValidationResult.predicated(ConfigContext(config).withContext(ACTIONS, actionsNeeded).withContext(RESTART_RECORDS, restartRecords), errorBuilder.size <= inboundErrorSize, "Errors found while deserializing Config ${config.javaClass.canonicalName}!")
    }

    internal fun <T: Any> deserializeUpdate(config: T, string: String, errorBuilder: MutableList<String>, flags: Byte = CHECK_NON_SYNC): ValidationResult<ConfigContext<T>> {
        val toml = try {
            Toml.parseToTomlTable(string)
        } catch (e:Exception) {
            return ValidationResult.error(ConfigContext(config), "Config ${config.javaClass.canonicalName} is corrupted or improperly formatted for parsing")
        }
        return deserializeUpdateFromToml(config, toml, errorBuilder, flags)
    }

    internal fun <T> deserializeEntry(entry: Entry<T, *>, string: String, scope: String, errorBuilder: MutableList<String>, flags: Byte = CHECK_NON_SYNC): ValidationResult<out T?> {
        val toml = try {
            Toml.parseToTomlTable(string)
        } catch (e:Exception) {
            return ValidationResult.error(null, "Toml $string isn't properly formatted to be deserialized")
        }
        val element = toml["entry"] ?: return ValidationResult.error(null, "Toml $string doesn't contain needed 'entry' key")
        return entry.deserializeEntry(element, errorBuilder, scope, flags)
    }

    ///////////////////////////////////////////////

    internal fun <T: Any> generatePermissionsReport(player: ServerPlayerEntity, config: T, flags: Byte = CHECK_NON_SYNC): MutableMap<String, Boolean> {
        val map: MutableMap<String, Boolean> = mutableMapOf()

        walk(config, (config as? Config)?.getId()?.toTranslationKey() ?: "", flags) { _, _, key, _, _, annotations, _, _ ->
            annotations.firstOrNull { it is WithCustomPerms }?.cast<WithCustomPerms>()?.let {
                for (group in it.perms) {
                    if (PlatformUtils.hasPermission(player, group)) {
                        map[key] = true
                        break
                    }
                }
                if (it.fallback >= 0) {
                    if (player.hasPermissionLevel(it.fallback))
                        map[key] = true
                }
                map.putIfAbsent(key, false)
            }
        }

        return map
    }

    internal fun validatePermissions(player: ServerPlayerEntity, id: String, config: Config, configString: String): ValidationResult<List<String>> {
        val toml = try {
            Toml.parseToTomlTable(configString)
        } catch (e:Exception) {
            return ValidationResult.error(listOf(), "Update for $id is corrupted or improperly formatted for parsing")
        }
        val list: MutableList<String> = mutableListOf()
        val playerPermLevel = getPlayerPermissionLevel(player)

        try {
            walk(config, id, CHECK_NON_SYNC) { _, _, str, _, _, annotations, _, _ ->
                if(toml.containsKey(str)) {
                    if(!hasNeededPermLevel(player, playerPermLevel, config, annotations)) {
                        list.add(str)
                    }
                }
            }
        } catch(e: Throwable) {
            FC.LOGGER.error("Critical exception encountered while validating update permissions. Defaulting to rejection of the update")
            e.printStackTrace()
            return ValidationResult.error(listOf(e.message ?: ""), "Critical exception encountered while validating update permissions. Defaulting to rejection of the update")
        }
        return ValidationResult.predicated(list, list.isEmpty(), "Access Violations Found!")
    }

    fun isConfigAdmin(player: ServerPlayerEntity, config: Config): Boolean {
        val annotation = config::class.annotations.firstOrNull{ it is AdminAccess }?.cast<WithCustomPerms>()
        if (annotation == null) {
            return player.hasPermissionLevel(3)
        }
        for (perm in annotation.perms) {
            if(PlatformUtils.hasPermission(player, perm)) {
                return true
            }
        }
        if (annotation.fallback >= 0) {
            return player.hasPermissionLevel(annotation.fallback)
        }
        return player.hasPermissionLevel(3)
    }

    private fun getPlayerPermissionLevel(player: PlayerEntity): Int {
        var i = 0
        while(player.hasPermissionLevel(i)) {
            i++
        }
        return i - 1
    }

    internal fun makeDir(folder: String, subfolder: String): Pair<File, Boolean> {
        val dir = if (subfolder != "") {
            File(File(PlatformUtils.configDir(), folder), subfolder)
        } else {
            if (folder != "") {
                File(PlatformUtils.configDir(), folder)
            } else {
                PlatformUtils.configDir()
            }
        }
        if (!dir.exists() && !dir.mkdirs()) {
            FC.LOGGER.error("Could not create directory ./$folder/$subfolder")
            return Pair(dir, false)
        }
        return Pair(dir, true)
    }

    private fun encodeToTomlElement(a: Any, clazz: KType): TomlElement? {
        return try {
            val strategy = Toml.serializersModule.serializer(clazz)
            Toml. encodeToTomlElement(strategy, a)
        } catch (e: Exception) {
            null
        }
    }

    private fun decodeFromTomlElement(element: TomlElement, clazz: KType): Any? {
        return try {
            val strategy = Toml.serializersModule.serializer(clazz) as? KSerializer<*> ?: return null
            Toml.decodeFromTomlElement(strategy, element)
        } catch (e: Exception) {
            null
        }
    }

    ///////////////// END Deserialize ////////////////////////////////////////////////////

    ///////////////// Reflection /////////////////////////////////////////////////////////

    private fun isIgnoreVisibility(clazz: KClass<*>): Boolean {
        return clazz.annotations.firstOrNull { (it is IgnoreVisibility) }?.let { true } ?: false
    }
    private fun isNonSync(property: KProperty<*>): Boolean {
        return isNonSync(property.annotations)
    }
    internal fun isNonSync(annotations: List<Annotation>): Boolean {
        return annotations.firstOrNull { (it is NonSync) }?.let { true } ?: false
    }

    private fun getAction(annotations: List<Annotation>): Action? {
        return (annotations.firstOrNull { (it is RequiresAction) } as? RequiresAction ?: annotations.firstOrNull { (it is RequiresRestart) }?.let { RequiresAction(Action.RESTART) })?.action
    }

    private fun requiredAction(settingAnnotations: List<Annotation>, classAction: Action?): Action? {
        val settingAction = getAction(settingAnnotations)
        if (settingAction == null && classAction == null) return null
        if (settingAction == null) return classAction
        if (classAction == null) return settingAction
        return if (settingAction.isPriority(classAction)) {
            settingAction
        } else {
            classAction
        }
    }

    internal fun getActions(thing: Any, flags: Byte): Set<Action> {
        val classAction = getAction(thing::class.annotations)
        val propActions: MutableSet<Action> = mutableSetOf()
        walk(thing, "", flags) { _, _, _, _, _, annotations, _, _ ->
            val action = requiredAction(annotations, classAction)
            if (action != null) {
                propActions.add(action)
            }
        }
        return propActions
    }

    internal fun requiredAction(settingAnnotations: List<Annotation>, classAnnotations: List<Annotation>): Action? {
        val settingAction = getAction(settingAnnotations)
        val classAction = getAction(classAnnotations)
        if (settingAction == null && classAction == null) return null
        if (settingAction == null) return classAction
        if (classAction == null) return settingAction
        return if (settingAction.isPriority(classAction)) {
            settingAction
        } else {
            classAction
        }
    }
    internal fun tomlAnnotations(property: KAnnotatedElement): List<Annotation> {
        return property.annotations.map { mapJvmAnnotations(it) }.filter { it is TomlComment || it is TomlInline || it is TomlBlockArray || it is TomlMultilineString || it is TomlLiteralString || it is TomlInteger }
    }
    private fun mapJvmAnnotations(input: Annotation): Annotation {
        return when(input) {
            is Comment -> TomlComment(input.value)
            is Inline -> TomlInline()
            is BlockArray -> TomlBlockArray(input.itemsPerLine)
            is MultilineString -> TomlMultilineString()
            is LiteralString -> TomlLiteralString()
            is Integer -> TomlInteger(input.base, input.group)
            else -> input
        }
    }
    private fun <T: Any> tomlHeaderAnnotations(field: KClass<T>): List<TomlHeaderComment> {
        return field.annotations.mapNotNull { it as? TomlHeaderComment }
    }
    private fun getVersion(clazz: KClass<*>): Int {
        val version = clazz.findAnnotation<Version>()
        return version?.version ?: 0
    }
    private fun getCompat(clazz: KClass<*>): Pair<File?, Boolean> {
        val version = clazz.findAnnotation<ConvertFrom>() ?: return Pair(null, false)
        val dir = makeDir(version.folder, version.subfolder)
        if (!dir.second) return Pair(null, false)
        if (!validFileTypes(version.fileName)) return Pair(null, false)
        return Pair(File(dir.first, version.fileName), version.fileName.endsWith(".toml"))
    }
    private fun validFileTypes(fileName: String): Boolean {
        return fileName.endsWith(".json") || fileName.endsWith(".json5") || fileName.endsWith(".jsonc") || fileName.endsWith(".toml")
    }

    private fun validateNumber(thing: Any?, property: KProperty<*>): Any? {
        if (thing !is Number) return thing
        when (thing) {
            is Int -> {
                val restriction = property.findAnnotation<ValidatedInt.Restrict>() ?: return thing
                return MathHelper.clamp(thing, restriction.min, restriction.max)
            }
            is Byte -> {
                val restriction = property.findAnnotation<ValidatedByte.Restrict>() ?: return thing
                return if (thing < restriction.min) restriction.min else if (thing > restriction.max) restriction.max else thing
            }
            is Short -> {
                val restriction = property.findAnnotation<ValidatedShort.Restrict>() ?: return thing
                return if (thing < restriction.min) restriction.min else if (thing > restriction.max) restriction.max else thing
            }
            is Long -> {
                val restriction = property.findAnnotation<ValidatedLong.Restrict>() ?: return thing
                return if (thing < restriction.min) { restriction.min } else min(thing, restriction.max)
            }
            is Double -> {
                val restriction = property.findAnnotation<ValidatedDouble.Restrict>() ?: return thing
                return MathHelper.clamp(thing, restriction.min, restriction.max)
            }
            is Float -> {
                val restriction = property.findAnnotation<ValidatedFloat.Restrict>() ?: return thing
                return MathHelper.clamp(thing, restriction.min, restriction.max)
            }
            else -> return thing
        }
    }

    private fun hasNeededPermLevel(player: ServerPlayerEntity, playerPermLevel: Int, config: Config, annotations: List<Annotation>): Boolean {
        if (player.server.isSingleplayer) return true
        // 1. NonSync wins over everything, even whole config annotations
        if (ConfigApiImpl.isNonSync(annotations)) return true
        val configAnnotations = config::class.annotations
        // 2. whole-config ClientModifiable
        for (annotation in configAnnotations) {
            if (annotation is ClientModifiable)
                return true
        }
        // 3. per-setting ClientModifiable
        for (annotation in annotations) {
            if (annotation is ClientModifiable)
                return true
        }
        for (annotation in annotations) {
            //4. per-setting WithCustomPerms
            if (annotation is WithCustomPerms) {
               for (perm in annotation.perms) {
                    if(PlatformUtils.hasPermission(player, perm)) {
                        return true
                    }
                }
                if (annotation.fallback >= 0) {
                    return playerPermLevel >= annotation.fallback
                }
            }
            //5. per-setting WithPerms
            if (annotation is WithPerms)
                return playerPermLevel >= annotation.opLevel
        }
        for (annotation in configAnnotations) {
            //6. whole-config WithCustomPerms
            if (annotation is WithCustomPerms) {
                for (perm in annotation.perms) {
                    if(PlatformUtils.hasPermission(player, perm)) {
                        return true
                    }
                }
                if (annotation.fallback >= 0) {
                    return playerPermLevel >= annotation.fallback
                }
            }
            //7. whole-config WithPerms
            if (annotation is WithPerms)
                return playerPermLevel >= annotation.opLevel
        }
        //8. fallback to default vanilla permission level
        return playerPermLevel >= config.defaultPermLevel()
    }

    ///////////////// END Annotations ////////////////////////////////////////////////////

    ///////////////// Flags //////////////////////////////////////////////////////////////

    private fun ignoreNonSync(flags: Byte): Boolean {
        return flags and 1.toByte() == 1.toByte()
    }

    private fun checkActions(flags: Byte): Boolean {
        return flags and 2.toByte() == 2.toByte()
    }

    private fun ignoreVisibility(flags: Byte): Boolean {
        return flags and 4.toByte() == 4.toByte()
    }

    private fun recordRestarts(flags: Byte): Boolean {
        return flags and 8.toByte() == 8.toByte()
    }

    ///////////////// END Flags ///////////////////////////////////////////////////////////

    ///////////////// Printing ////////////////////////////////////////////////////////////

    internal fun printChangeHistory(history: List<String>, id: String, player: PlayerEntity? = null) {
        FC.LOGGER.info("$$$$$$$$$$$$$$$$$$$$$$$$$$")
        FC.LOGGER.info("Completed updates for configs: [$id]")
        if (player != null)
            FC.LOGGER.info("Updates made by: ${player.name.string}")
        FC.LOGGER.info("-------------------------")
        for (str in history)
            FC.LOGGER.info("  $str")
        FC.LOGGER.info("$$$$$$$$$$$$$$$$$$$$$$$$$$")
    }

    ///////////////// END Printing ///////////////////////////////////////////////////////

    ///////////////// Walking ////////////////////////////////////////////////////////////

    internal fun<W: Any> walk(walkable: W, prefix: String, flags: Byte, walkAction: WalkAction) {
        try {
            // check for IgnoreVisiblity
            val ignoreVisibility = isIgnoreVisibility(walkable::class) || ignoreVisibility(flags)
            val orderById = walkable::class.java.declaredFields.filter {
                !isTransient(it.modifiers)
            }.withIndex().associate {
                it.value.name to it.index
            }
            val globalAnnotations = walkable::class.annotations
            val walkCallback = WalkCallback(walkable)
            for (property in walkable.javaClass.kotlin.memberProperties
                .filter {
                    it is KMutableProperty<*>
                            && (if (ignoreNonSync(flags)) true else !isNonSync(it))
                            && if (ignoreVisibility) (it.javaField?.trySetAccessible() == true) else it.visibility == KVisibility.PUBLIC
                }.sortedBy {
                    orderById[it.name]
                }
            ) {
                try {
                    val newPrefix = prefix + "." + property.name
                    val propVal = property.get(walkable)
                    walkAction.act(
                        walkable,
                        prefix,
                        newPrefix,
                        propVal,
                        property as KMutableProperty<*>,
                        property.annotations,
                        globalAnnotations,
                        walkCallback
                    )
                    if (walkCallback.isCancelled())
                        break
                    if (walkCallback.isContinued())
                        continue
                    if (propVal is Walkable) {
                        val newFlags = if (ignoreVisibility) flags or IGNORE_VISIBILITY else flags
                        walk(propVal, newPrefix, newFlags, walkAction)
                    }
                } catch (e: Exception) {
                    FC.LOGGER.error("Critical exception caught while walking $prefix")
                    e.printStackTrace()
                    // continue without borking
                }
            }
        } catch (e: Exception) {
            FC.LOGGER.error("Critical exception encountered while Walking through ${walkable::class.simpleName}")
            e.printStackTrace()
        }
    }

    internal fun<W: Any> drill(walkable: W, target: String, delimiter: Char, flags: Byte, walkAction: WalkAction) {
        try {
            // check for IgnoreVisiblity
            val ignoreVisibility = isIgnoreVisibility(walkable::class) || ignoreVisibility(flags)
            //generate an index map, so I can order the properties based on name
            val props = walkable.javaClass.kotlin.memberProperties.filter {
                !isTransient(it.javaField?.modifiers ?: Modifier.TRANSIENT)
                        && it is KMutableProperty<*>
                        && (if (ignoreNonSync(flags)) true else !isNonSync(it))
                        && if (ignoreVisibility) (it.javaField?.trySetAccessible() == true) else it.visibility == KVisibility.PUBLIC
            }.associateBy { it.name }
            val globalAnnotations = walkable::class.annotations
            val callback = WalkCallback(walkable)
            val propTry = props[target]
            if (propTry != null) {
                return try {
                    val propVal = propTry.get(walkable)
                    walkAction.act(
                        walkable,
                        "",
                        "",
                        propVal,
                        propTry as KMutableProperty<*>,
                        propTry.annotations,
                        globalAnnotations,
                        callback
                    )
                } catch (e: Exception) {
                    FC.LOGGER.error("Critical exception caught while drill to $target")
                    e.printStackTrace()
                    // continue without borking
                }
            }
            for ((string, property) in props) {
                if (target.startsWith(string)) {
                    try {
                        val propVal = property.get(walkable)
                        if (propVal is Walkable) {
                            val newFlags = if (ignoreVisibility) flags or IGNORE_VISIBILITY else flags
                            drill(propVal, target.substringAfter(delimiter), delimiter, newFlags, walkAction)
                        } else {
                            break
                        }
                    } catch (e: Exception) {
                        FC.LOGGER.error("Critical exception caught while drill to $target")
                        e.printStackTrace()
                        // continue without borking
                    }
                }
            }
        } catch (e: Exception) {
            FC.LOGGER.error("Critical exception encountered while Drilling into ${walkable::class.simpleName}")
            e.printStackTrace()
        }
    }

    internal fun interface WalkAction {
        fun act(walkable: Any,
                oldPrefix: String,
                newPrefix: String,
                element: Any?,
                elementProp: KMutableProperty<*>,
                annotations: List<Annotation>,
                globalAnnotations: List<Annotation>,
                walkCallback: WalkCallback)
    }

    internal class WalkCallback (val walkable: Any) {
        private var cancelled = false
        private var continued = false
        fun isCancelled(): Boolean {
            return cancelled
        }
        fun cancel() {
            this.cancelled = true
        }
        fun isContinued(): Boolean {
            val bl = continued
            continued = false
            return bl
        }
        fun cont() {
            continued = true
        }
    }
}