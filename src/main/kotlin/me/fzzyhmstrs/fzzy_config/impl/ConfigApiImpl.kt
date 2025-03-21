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
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.serializer
import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.FCC
import me.fzzyhmstrs.fzzy_config.annotations.*
import me.fzzyhmstrs.fzzy_config.api.FileType
import me.fzzyhmstrs.fzzy_config.api.RegisterType
import me.fzzyhmstrs.fzzy_config.cast
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.config.ConfigContext
import me.fzzyhmstrs.fzzy_config.config.ConfigContext.Keys.ACTIONS
import me.fzzyhmstrs.fzzy_config.config.ConfigContext.Keys.RESTART_RECORDS
import me.fzzyhmstrs.fzzy_config.config.ConfigContext.Keys.VERSIONS
import me.fzzyhmstrs.fzzy_config.config.ConfigSection
import me.fzzyhmstrs.fzzy_config.entry.*
import me.fzzyhmstrs.fzzy_config.registry.SyncedConfigRegistry
import me.fzzyhmstrs.fzzy_config.result.impl.ResultApiImpl
import me.fzzyhmstrs.fzzy_config.updates.BasicValidationProvider
import me.fzzyhmstrs.fzzy_config.updates.UpdateManager
import me.fzzyhmstrs.fzzy_config.util.TomlOps
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.report
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.wrap
import me.fzzyhmstrs.fzzy_config.util.Walkable
import me.fzzyhmstrs.fzzy_config.util.platform.impl.PlatformUtils
import me.fzzyhmstrs.fzzy_config.validation.number.*
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.registry.BuiltinRegistries
import net.minecraft.registry.RegistryWrapper.WrapperLookup
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier
import net.minecraft.util.math.MathHelper
import net.peanuuutz.tomlkt.*
import java.io.BufferedReader
import java.io.File
import java.io.Reader
import java.lang.reflect.Modifier
import java.lang.reflect.Modifier.isTransient
import java.util.*
import java.util.function.Supplier
import kotlin.experimental.and
import kotlin.math.min
import kotlin.experimental.or
import kotlin.reflect.*
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.javaSetter

internal object ConfigApiImpl {

    private val gson by lazy {
        GsonBuilder().setPrettyPrinting().create()
    }

    private val jankson by lazy {
        Jankson.builder().build()
    }

    private val isClient by lazy {
        PlatformUtils.isClient()
    }

    private var wrapperLookup: WrapperLookup? = null

    internal fun invalidateLookup() {
        ResultApiImpl.invalidateProviderCaches()
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
    internal const val IGNORE_NON_SYNC_AND_IGNORE_VISIBILITY: Byte = 5
    internal const val RECORD_RESTARTS: Byte = 8
    internal const val CHECK_ACTIONS_AND_RECORD_RESTARTS: Byte = 10
    internal const val FLAT_WALK: Byte = 16
    internal const val IGNORE_NON_SYNC_AND_FLAT_WALK: Byte = 17

    private val configClass = Config::class
    private val configSectionClass = ConfigSection::class

    internal fun openScreen(scope: String) {
        if (isClient)
            ConfigApiImplClient.openScreen(scope)
    }

    internal fun isScreenOpen(scope: String): Boolean {
        if (isClient)
            return ConfigApiImplClient.isScreenOpen(scope)
        return false
    }

    internal fun openRestartScreen() {
        if (isClient)
            FCC.openRestartScreen()
    }

    internal fun getConfig(scope: String): Config? {
        return SyncedConfigRegistry.syncedConfigs()[scope] ?: if(isClient) ConfigApiImplClient.getClientConfig(scope) else null
    }

    internal fun getSyncedConfig(id: Identifier): Config? {
        return SyncedConfigRegistry.getConfig(id.toTranslationKey())
    }

    internal fun getClientConfig(id: Identifier): Config? {
        return if(isClient) ConfigApiImplClient.getClientConfig(id) else null
    }

    ///////////////////// Registration ///////////////////////////////////////////////////

    internal fun <T: Config> registerConfig(config: T, configClass: () -> T, registerType: RegisterType, noGui: Boolean = false): T {
        return when(registerType) {
            RegisterType.BOTH -> registerBoth(config, configClass, noGui)
            RegisterType.SERVER -> registerSynced(config)
            RegisterType.CLIENT -> registerClient(config, configClass, noGui)
        }
    }

    private fun <T: Config> registerBoth(config: T, configClass: () -> T, noGui: Boolean): T {
        SyncedConfigRegistry.registerConfig(config)
        return registerClient(config, configClass, noGui)
    }
    private fun <T: Config> registerSynced(config: T): T {
        SyncedConfigRegistry.registerConfig(config)
        return config
    }

    private fun <T: Config> registerClient(config: T, configClass: () -> T, noGui: Boolean): T {
        if(isClient)
            ConfigApiImplClient.registerConfig(config, configClass(), noGui)
        return config
    }

    internal fun <T: Config> registerAndLoadConfig(configClass: () -> T, registerType: RegisterType, noGui: Boolean = false): T {
        return when(registerType) {
            RegisterType.BOTH -> registerAndLoadBoth(configClass, noGui)
            RegisterType.SERVER -> registerAndLoadSynced(configClass)
            RegisterType.CLIENT -> registerAndLoadClient(configClass, noGui)
        }
    }
    private fun <T: Config> registerAndLoadBoth(configClass: () -> T, noGui: Boolean): T {
        return registerBoth(readOrCreateAndValidate(configClass), configClass, noGui)
    }
    private fun <T: Config> registerAndLoadSynced(configClass: () -> T): T {
        return registerSynced(readOrCreateAndValidate(configClass))
    }

    private fun <T: Config> registerAndLoadClient(configClass: () -> T, noGui: Boolean): T {
        return registerClient(readOrCreateAndValidate(configClass), configClass, noGui)
    }

    internal fun isConfigLoaded(scope: String, type: RegisterType): Boolean {
        return when (type) {
            RegisterType.BOTH -> isSyncedConfigLoaded(scope) || isClientConfigLoaded(scope)
            RegisterType.SERVER -> isSyncedConfigLoaded(scope)
            RegisterType.CLIENT -> isClientConfigLoaded(scope)
        }
    }

    internal fun isSyncedConfigLoaded(id: Identifier): Boolean {
        return SyncedConfigRegistry.hasConfig(id.toTranslationKey())
    }

    internal fun isSyncedConfigLoaded(scope: String): Boolean {
        var startIndex = 0
        while (startIndex < scope.length) {
            val nextStartIndex = scope.indexOf(".", startIndex)
            if (nextStartIndex == -1) {
                return false
            }
            startIndex = nextStartIndex + 1
            val testScope = scope.substring(0, nextStartIndex)
            if (SyncedConfigRegistry.hasConfig(testScope)) return true
        }
        return false
    }

    internal fun isClientConfigLoaded(id: Identifier): Boolean {
        if (!isClient) return false
        return ConfigApiImplClient.isConfigLoaded(id)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    internal fun isClientConfigLoaded(scope: String): Boolean {
        if (!isClient) return false
        return ConfigApiImplClient.isConfigLoaded(scope)
    }

    ///////////////// Flags //////////////////////////////////////////////////////////////

    //////////////// Read, Create, Save //////////////////////////////////////////////////

    internal fun <T: Config> readOrCreateAndValidate(name: String, folder: String = "", subfolder: String = "", configClass: () -> T): T {
        fun log(start: Long) {
            FC.LOGGER.info("Loaded config {} in {}ms", "$folder:$name", (System.currentTimeMillis() - start))
        }
        //wrap entire method in a try-catch. don't need to have config problems causing a hard crash, just fall back
        try {
            val start = System.currentTimeMillis()
            //create our directory, or bail if we can't for some reason
            val (dir, dirCreated) = makeDir(folder, subfolder)
            if (!dirCreated) {
                FC.LOGGER.error("Failed to create directory [${if(subfolder.isNotEmpty()) "./$folder/$subfolder" else "./$folder"}]. Using default config for [$name].")
                return configClass()
            }

            val classInstance = configClass()
            val files = findFiles(dir, name, classInstance.fileType())

            if (files.fIn.exists()) {
                val fErrorsIn = mutableListOf<String>()
                val str = files.fIn.readLines().joinToString("\n")

                val classVersion = getVersion(classInstance::class)
                val readConfigResult = deserializeConfig(classInstance, str, fErrorsIn, fileType = files.fInType)
                val readVersion = readConfigResult.get().getInt(VERSIONS)
                val readConfig = readConfigResult.get().config
                val needsUpdating = classVersion > readVersion
                if (readConfigResult.isError() || needsUpdating) {
                    if (readConfigResult.isError()) {
                        readConfigResult.writeWarning(fErrorsIn)
                    }
                    if (needsUpdating) {
                        readConfig.update(readVersion)
                    }
                    val fErrorsOut = mutableListOf<String>()
                    val correctedConfig = serializeConfig(readConfig, fErrorsOut, fileType = files.fOutType)
                    if (fErrorsOut.isNotEmpty()) {
                        val fErrorsOutResult = ValidationResult.error(
                            true,
                            "Critical error(s) encountered while re-serializing corrected Config Class! Output may not be complete."
                        )
                        fErrorsOutResult.writeError(fErrorsOut)
                    }
                    writeFile(files.fOut, correctedConfig, name, "correcting errors or updating version", files.fIn)
                } else if (files.fIn != files.fOut) {
                    val fErrorsOut = mutableListOf<String>()
                    val newFormatConfig = serializeConfig(readConfig, fErrorsOut, fileType = files.fOutType)
                    if (fErrorsOut.isNotEmpty()) {
                        val fErrorsOutResult = ValidationResult.error(
                            true,
                            "Critical error(s) encountered while re-serializing corrected Config Class! Output may not be complete."
                        )
                        fErrorsOutResult.writeError(fErrorsOut)
                    }
                    writeFile(files.fOut, newFormatConfig, name, "moving config to new file format", files.fIn)
                }
                log(start)
                return readConfig
            } else if (!files.fOut.createNewFile()) {
                FC.LOGGER.error("Couldn't create new file for config [$name]. Using default config.")
                return classInstance
            } else {
                val oldFilePair = getCompat(classInstance::class)
                val oldFile = oldFilePair.first
                if (oldFile != null && oldFile.exists()) {
                    val str = oldFile.readLines().joinToString("\n")
                    val errorBuilder = mutableListOf<String>()
                    val convertedConfigResult = deserializeConfig(classInstance, str, errorBuilder, fileType = oldFilePair.second)
                    if (convertedConfigResult.isError()) {
                        convertedConfigResult.writeWarning(errorBuilder)
                    }
                    oldFile.delete()
                }
                val fErrorsOut = mutableListOf<String>()
                val convertedConfig = serializeConfig(classInstance, fErrorsOut, fileType = files.fOutType)
                if (fErrorsOut.isNotEmpty()) {
                    val fErrorsOutResult = ValidationResult.error(true, "Critical error(s) encountered while re-serializing corrected Config Class! Output may not be complete.")
                    fErrorsOutResult.writeError(fErrorsOut)
                }
                writeFile(files.fOut, convertedConfig, name, "converting old config")
                log(start)
                return classInstance
            }
        } catch (e: Throwable) {
            FC.LOGGER.error("Critical error encountered while reading or creating [$name]. Using default config.", e)
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
                FC.LOGGER.error("Couldn't create directory [${if(subfolder.isNotEmpty()) "./$folder/$subfolder" else "./$folder"}]. Failed to save config file $name!")
                return
            }

            val files = findFiles(dir, name, configClass.fileType())

            if (files.fOut.exists() || files.fOut.createNewFile()) {
                val fErrorsOut = mutableListOf<String>()
                val str = serializeConfig(configClass, fErrorsOut, fileType = files.fOutType)
                if (fErrorsOut.isNotEmpty()) {
                    val fErrorsOutResult = ValidationResult.error(
                        true,
                        "Critical error(s) encountered while saving updated Config Class! Output may not be complete."
                    )
                    fErrorsOutResult.writeError(fErrorsOut)
                }
                writeFile(files.fOut, str, name, "saving config", files.fIn)
            } else  {
                FC.LOGGER.error("Failed to save config file $name to ${files.fOut}, config not saved.")
            }
        } catch (e: Throwable) {
            FC.LOGGER.error("Critical error while trying to save config file $name!", e)
        }
    }

    internal fun <T : Config> save(configClass: T) {
        save(configClass.name, configClass.folder, configClass.subfolder, configClass)
    }

    internal fun parseReader(reader: Reader): TomlElement {
        val r = (if (reader is BufferedReader) reader else BufferedReader(reader))
        return r.use {
            Toml.parseToTomlTable(TomlNativeReader(r))
        }
    }

    ///////////////// END Read, Create, Save /////////////////////////////////////////////

    ///////////////// Encode-Decode //////////////////////////////////////////////////////

    internal fun encodeToml(toml: TomlElement): ValidationResult<String> {
        return ValidationResult.success(Toml.encodeToString(toml))
    }

    internal fun decodeToml(string: String): ValidationResult<TomlElement> {
        return try {
            ValidationResult.success(Toml.parseToTomlTable(string))
        } catch (_: Throwable) {
            return  ValidationResult.error(TomlNull, "Critical error encountered while decoding TOML")
        }
    }

    internal fun encodeJson(toml: TomlElement): ValidationResult<String> {
        return try {
            val jsonElement = TomlOps.INSTANCE.convertTo(JsonOps.INSTANCE, toml)
            ValidationResult.success(gson.toJson(jsonElement))
        } catch (_: Throwable) {
            ValidationResult.error("", "Critical error encountered while encoding JSON")
        }
    }

    internal fun decodeJson(string: String): ValidationResult<TomlElement> {
        return try {
            val tomlElement = JsonOps.INSTANCE.convertTo(TomlOps.INSTANCE, JsonParser.parseString(string))
            ValidationResult.success(tomlElement)
        } catch (_: Throwable) {
            return  ValidationResult.error(TomlNull, "Critical error encountered while decoding JSON")
        }
    }

    internal fun encodeJson5(toml: TomlElement): ValidationResult<String> {
        if (toml !is TomlTable) return ValidationResult.error("", "Toml provided to json5 encoder not a table")
        return try {
            val table = toml.asTomlTable()
            val obj = TomlOps.convertToJson5(table)
            ValidationResult.success(obj.toJson(true, true))
        } catch (_: Throwable) {
            ValidationResult.error("", "Critical error encountered while encoding JSON5")
        }
    }

    internal fun decodeJson5(string: String): ValidationResult<TomlElement> {
        return try {
            val jsonObject = jankson.load(string)
            val jsonString = jsonObject.toJson(false, false)
            val tomlElement = JsonOps.INSTANCE.convertTo(TomlOps.INSTANCE, JsonParser.parseString(jsonString))
            ValidationResult.success(tomlElement)
        } catch (_: Throwable) {
            return  ValidationResult.error(TomlNull, "Critical error encountered while decoding JSON5")
        }
    }

    ///////////////// End Encode-Decode //////////////////////////////////////////////////

    ///////////////// Serialize //////////////////////////////////////////////////////////

    internal fun <T: Any> serializeToToml(config: T, errorBuilder: MutableList<String>, flags: Byte = IGNORE_NON_SYNC): TomlTable {
        //used to build a TOML table piece by piece
        val toml = TomlTableBuilder()
        try {
            val clazz = config::class
            if (config is Config) {
                val version = getVersion(clazz)
                val headerAnnotations = tomlHeaderAnnotations(clazz).toMutableList()
                headerAnnotations.add(TomlHeaderComment("Don't change this! Version used to track needed updates."))
                toml.element("version", TomlLiteral(version), headerAnnotations.map { TomlComment(it.text) })
            }
            val ignoreVisibility = isIgnoreVisibility(clazz) || ignoreVisibility(flags)
            //java fields are ordered in declared order, apparently not so for Kotlin properties. use these first to get ordering. skip Transient
            val fields = clazz.java.declaredFields.filter { !isTransient(it.modifiers) }.toMutableList()
            for (sup in clazz.allSuperclasses) {
                if (sup == configClass) continue //ignore Config itself, as that has state we don't need
                fields.addAll(sup.java.declaredFields.filter { !isTransient(it.modifiers) })
            }
            //generate an index map, so I can order the properties based on name
            val orderById = fields.withIndex().associate { it.value.name to it.index }
            //kotlin member properties filtered by [field map contains it && if NonSync matters, it isn't NonSync]. NonSync does not matter by default
            for (prop in config.javaClass.kotlin.memberProperties.filter {
                it is KMutableProperty<*>
                        && (if (ignoreNonSync(flags)) true else !isNonSync(it))
                        && !isTransient(it.javaField?.modifiers ?: Modifier.TRANSIENT)
                        && if(ignoreVisibility) trySetAccessible(it) else it.visibility == KVisibility.PUBLIC
            }.sortedBy { orderById[it.name] }) {
                //has to be a public mutable property. private and protected and val another way to have serialization ignore
                if (prop !is KMutableProperty<*>) continue
                //get the actual [thing] from the property
                val propVal = prop.get(config)
                if (propVal is EntryTransient) continue
                //if(ignoreVisibility) (prop.javaField?.trySetAccessible())
                //things name
                val name = prop.name
                //serialize the element. EntrySerializer elements will have a set serialization method
                val el = if (propVal is EntrySerializer<*>) { //is EntrySerializer
                    try {
                        val newFlags = if (ignoreVisibility || isIgnoreVisibility(propVal::class)) flags or IGNORE_VISIBILITY else flags
                        propVal.serializeEntry(null, errorBuilder, newFlags)
                    } catch (e: Throwable) {
                        errorBuilder.add("Problem encountered with serialization of [$name]: ${e.localizedMessage}")
                        TomlNull
                    }
                    //fallback is to use by-type TOML serialization
                } else if (propVal != null) {
                    val basicValidation = UpdateManager.basicValidationStrategy(propVal, prop.returnType, name, prop.annotations)
                    if (basicValidation != null) {
                        val newFlags = if (ignoreVisibility) flags or IGNORE_VISIBILITY else flags
                        basicValidation.trySerialize(propVal, errorBuilder, newFlags) ?: TomlNull
                    } else {
                        try {
                            encodeToTomlElement(propVal, prop.returnType) ?: TomlNull
                        } catch (e: Throwable) {
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
        } catch (e: Throwable) {
            errorBuilder.add("Critical error encountered while serializing config!: ${e.localizedMessage}")
            return toml.build()
        }
        //serialize the TomlTable to its string representation
        return toml.build()
    }

    internal fun <T: Any> serializeConfig(config: T, errorBuilder: MutableList<String>, flags: Byte = IGNORE_NON_SYNC, fileType: FileType = FileType.TOML): String {
        return fileType.encode(serializeToToml(config, errorBuilder, flags)).report(errorBuilder).get()
    }

    private fun <T: Config, M> serializeUpdateToToml(config: T, manager: M, errorBuilder: MutableList<String>, flags: Byte = CHECK_NON_SYNC): TomlTable where M: UpdateManager, M: BasicValidationProvider {
        val toml = TomlTableBuilder()
        try {
            walk(config, config.getId().toTranslationKey(), flags) { _, _, str, v, prop, annotations, _, _ ->
                if(manager.hasUpdate(str)) {
                    if(v is EntrySerializer<*>) {
                        toml.element(str, v.serializeEntry(null, errorBuilder, flags))
                    } else if (v != null) {
                        val basicValidation = manager.basicValidationStrategy(v, prop.returnType, str, annotations)
                        if (basicValidation != null) {
                            val el = basicValidation.trySerialize(v, errorBuilder, flags)
                            if (el != null)
                                toml.element(str, el)
                        }
                    }
                }
            }
        } catch (e: Throwable) {
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
            return ValidationResult.error(ConfigContext(config).withContext(ACTIONS, mutableSetOf()), "Improper TOML format passed to deserializeFromToml")
        }
        val clazz = config::class
        try {
            val checkActions = checkActions(flags)
            val recordRestarts = if (!checkActions) false else recordRestarts(flags)
            val globalAction = getAction(clazz.annotations)
            val ignoreVisibility = isIgnoreVisibility(clazz) || ignoreVisibility(flags)

            val fields = clazz.java.declaredFields.toMutableList()
            for (sup in clazz.allSuperclasses) {
                if (sup == configClass) continue
                if (sup == configSectionClass) continue
                if (sup.java.isInterface) continue
                fields.addAll(sup.java.declaredFields)
            }

            val orderById = fields.withIndex().associate { it.value.name to it.index }

            for (prop in config.javaClass.kotlin.memberProperties.filter {
                it is KMutableProperty<*>
                        && (if (ignoreNonSync(flags)) true else !isNonSync(it))
                        && !isTransient(it.javaField?.modifiers ?: Modifier.TRANSIENT)
                        && if(ignoreVisibility) trySetAccessible(it) else it.visibility == KVisibility.PUBLIC
            }.sortedBy { orderById[it.name] }) {
                if (prop !is KMutableProperty<*>) continue
                val propVal = prop.get(config)
                if (propVal is EntryTransient) continue
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
                        val newFlags = if (ignoreVisibility || isIgnoreVisibility(propVal::class)) flags or IGNORE_VISIBILITY else flags
                        propVal.deserializeEntry(tomlElement, errorBuilder, name, newFlags).also { r ->
                            if(propVal.deserializedChanged(before, r.get()) ) {
                                restartNeeded.add(action)
                                if(recordRestarts && action.restartPrompt) {
                                    restartRecords.add(((config as? Config)?.getId()?.toTranslationKey() ?: "") + "." + name)
                                }
                            }
                        }
                    } else {
                        val newFlags = if (ignoreVisibility || isIgnoreVisibility(propVal::class)) flags or IGNORE_VISIBILITY else flags
                        propVal.deserializeEntry(tomlElement, errorBuilder, name, newFlags)
                    }
                    if (result.isError()) {
                        errorBuilder.add(result.getError())
                    }
                } else {
                    val basicValidation = UpdateManager.basicValidationStrategy(propVal, prop.returnType, name, prop.annotations)
                    if (basicValidation != null) {
                        @Suppress("DEPRECATION")
                        val thing = basicValidation.deserializeEntry(tomlElement, errorBuilder, name, flags)
                        try {
                            val action = requiredAction(prop.annotations, globalAction)
                            if(checkActions && action != null)
                                if (basicValidation.deserializedChanged(propVal, thing.get())) {
                                    restartNeeded.add(action)
                                    if(recordRestarts && action.restartPrompt) {
                                        restartRecords.add(((config as? Config)?.getId()?.toTranslationKey() ?: "") + "." + name)
                                    }
                                }
                            if(ignoreVisibility) trySetAccessible(prop)
                            prop.setter.call(config, thing.get())
                        } catch(e: Throwable) {
                            errorBuilder.add("Error deserializing basic validation [$name]: ${e.localizedMessage}")
                        }
                    } else {
                        try {
                            val action = requiredAction(prop.annotations, globalAction)
                            if(checkActions && action != null) {
                                if(ignoreVisibility) trySetAccessible(prop)
                                prop.setter.call(config, validateNumber(decodeFromTomlElement(tomlElement, prop.returnType), prop).also {
                                    if (propVal != it) {
                                        restartNeeded.add(action)
                                        if(recordRestarts && action.restartPrompt) {
                                            restartRecords.add(((config as? Config)?.getId()?.toTranslationKey() ?: "") + "." + name)
                                        }
                                    }
                                })
                            } else {
                                if(ignoreVisibility) trySetAccessible(prop)
                                prop.setter.call(config, validateNumber(decodeFromTomlElement(tomlElement, prop.returnType), prop))
                            }
                        } catch (e: Throwable) {
                            errorBuilder.add("Error deserializing raw field [$name]: ${e.localizedMessage}")
                        }
                    }
                }
            }
        } catch (_: Throwable) {
            errorBuilder.add("Critical error encountered while deserializing")
        }
        return ValidationResult.predicated(ConfigContext(config).withContext(ACTIONS, restartNeeded).withContext(RESTART_RECORDS, restartRecords), errorBuilder.size <= inboundErrorSize, "Errors found while deserializing Config ${config.javaClass.canonicalName}!")
    }

    internal fun <T: Any> deserializeConfig(config: T, string: String, errorBuilder: MutableList<String>, flags: Byte = IGNORE_NON_SYNC, fileType: FileType = FileType.TOML): ValidationResult<ConfigContext<T>> {
        val toml = try {
            val tomlResult = fileType.decode(string)
            if (tomlResult.isError()) {
                return ValidationResult.error(ConfigContext(config), "Toml for config ${config.javaClass.canonicalName} is corrupted or improperly formatted for parsing")
            }
            tomlResult.get().asTomlTable()
        } catch (_: Throwable) {
            return  ValidationResult.error(ConfigContext(config), "Config ${config.javaClass.canonicalName} is corrupted or improperly formatted for parsing")
        }
        val version = if(toml.containsKey("version")) {
            try {
                toml["version"]?.asTomlLiteral()?.toInt() ?: 0
            }catch (_: Throwable) {
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
            walk(config, (config as? Config)?.getId()?.toTranslationKey() ?: "", flags) { c, _, str, v, prop, annotations, _, _ -> toml[str]?.let {
                if(v is EntryDeserializer<*>) {
                    val action = requiredAction(prop.annotations, globalAction)
                    if(checkActions && v is Supplier<*> && action != null) {
                        val before = v.get()
                        v.deserializeEntry(it, errorBuilder, str, flags).also { r ->
                            if(v.deserializedChanged(before, r.get())) {
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
                    val basicValidation = UpdateManager.basicValidationStrategy(v, prop.returnType, str, annotations)
                    if (basicValidation != null) {
                        @Suppress("DEPRECATION")
                        val thing = basicValidation.deserializeEntry(it, errorBuilder, str, flags)
                        try {
                            val action = requiredAction(prop.annotations, globalAction)
                            if(checkActions && action != null)
                                if (basicValidation.deserializedChanged(v, thing.get())) {
                                    actionsNeeded.add(action)
                                    if(recordRestarts && action.restartPrompt) {
                                        restartRecords.add(str)
                                    }
                                }
                            prop.setter.call(c, thing.get()) //change?
                        } catch(e: Throwable) {
                            errorBuilder.add("Error during update while deserializing basic validation [$str]: ${e.localizedMessage}")
                        }
                    }
                }
            }
            }
        } catch(_: Throwable) {
            errorBuilder.add("Critical error encountered while deserializing update")
        }
        return ValidationResult.predicated(ConfigContext(config).withContext(ACTIONS, actionsNeeded).withContext(RESTART_RECORDS, restartRecords), errorBuilder.size <= inboundErrorSize, "Errors found while deserializing Config ${config.javaClass.canonicalName}!")
    }

    internal fun <T: Any> deserializeUpdate(config: T, string: String, errorBuilder: MutableList<String>, flags: Byte = CHECK_NON_SYNC): ValidationResult<ConfigContext<T>> {
        val toml = try {
            Toml.parseToTomlTable(string)
        } catch (_: Throwable) {
            return ValidationResult.error(ConfigContext(config), "Config ${config.javaClass.canonicalName} is corrupted or improperly formatted for parsing")
        }
        return deserializeUpdateFromToml(config, toml, errorBuilder, flags)
    }

    internal fun <T> deserializeEntry(entry: Entry<T, *>, string: String, scope: String, errorBuilder: MutableList<String>, flags: Byte = CHECK_NON_SYNC): ValidationResult<out T?> {
        val toml = try {
            Toml.parseToTomlTable(string)
        } catch (_: Throwable) {
            return ValidationResult.error(null, "Toml $string isn't properly formatted to be deserialized")
        }
        val element = toml["entry"] ?: return ValidationResult.error(null, "Toml $string doesn't contain needed 'entry' key")
        return entry.deserializeEntry(element, errorBuilder, scope, flags)
    }

    ///////////////// END Deserialize ////////////////////////////////////////////////////

    ///////////////// Utilities //////////////////////////////////////////////////////////

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

    internal fun validatePermissions(player: ServerPlayerEntity, id: String, config: Config, configString: String, clientPermissions: Int): ValidationResult<List<String>> {
        val toml = try {
            Toml.parseToTomlTable(configString)
        } catch (_: Throwable) {
            return ValidationResult.error(emptyList(), "Update for $id is corrupted or improperly formatted for parsing")
        }
        val list: MutableList<String> = mutableListOf()
        val playerPermLevel = getPlayerPermissionLevel(player)

        if (playerPermLevel != clientPermissions) {
            return ValidationResult.error(emptyList(), "Client permission level does not match server permission level!")
        }

        try {
            walk(config, id, CHECK_NON_SYNC) { _, _, str, _, _, annotations, _, _ ->
                if(toml.containsKey(str)) {
                    if(!hasNeededPermLevel(player, playerPermLevel, config, annotations)) {
                        list.add(str)
                    }
                }
            }
        } catch(e: Throwable) {
            FC.LOGGER.error("Critical exception encountered while validating update permissions. Defaulting to rejection of the update", e)
            return ValidationResult.error(listOf(e.message ?: ""), "Critical exception encountered while validating update permissions. Defaulting to rejection of the update")
        }
        return ValidationResult.predicated(list, list.isEmpty(), "Access Violations Found!")
    }

    internal fun isConfigAdmin(player: ServerPlayerEntity, config: Config): Boolean {
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


    private fun writeFile(file: File, contents: String, name: String, phase: String, oldFile: File? = null) {
        if (file.exists()) {
            file.writeText(contents)
            if (oldFile != file) oldFile?.delete()
        } else if (file.createNewFile()) {
            file.writeText(contents)
            if (oldFile != file) oldFile?.delete()
        } else {
            FC.LOGGER.error("Couldn't write config $name to file $file while: $phase")
        }
    }

    private fun findFiles(dir: File, name: String, inputType: FileType): FileResult {
        var fIn: File = File(dir, "$name${inputType.suffix()}")
        var fInType: FileType = inputType
        val fOut: File
        val fOutType: FileType = inputType
        if (fIn.exists()) {
            fOut = fIn
        } else {
            val candidateFIInfo = FileType.entries.filter { it != inputType }.firstNotNullOfOrNull {
                File(dir, "$name${it.suffix()}").takeIf { f -> f.exists() }?.let { f -> Pair(f, it) }
            }
            fIn = candidateFIInfo?.first ?: fIn
            fInType = candidateFIInfo?.second ?: fInType
            fOut = File(dir, "$name${inputType.suffix()}")
        }
        return FileResult(fIn, fInType, fOut, fOutType)
    }

    private class FileResult(val fIn: File, val fInType: FileType, val fOut: File, val fOutType: FileType) {

    }

    private fun encodeToTomlElement(a: Any, clazz: KType): TomlElement? {
        return try {
            val strategy = Toml.serializersModule.serializer(clazz)
            Toml. encodeToTomlElement(strategy, a)
        } catch (_: Throwable) {
            null
        }
    }

    private fun decodeFromTomlElement(element: TomlElement, clazz: KType): Any? {
        return try {
            val strategy = Toml.serializersModule.serializer(clazz) as? KSerializer<*> ?: return null
            Toml.decodeFromTomlElement(strategy, element)
        } catch (_: Throwable) {
            null
        }
    }

    ///////////////// END Utilities //////////////////////////////////////////////////////

    ///////////////// Reflection /////////////////////////////////////////////////////////

    private fun isIgnoreVisibility(clazz: KClass<*>): Boolean {
        return clazz.annotations.any { it is IgnoreVisibility }
    }

    private fun trySetAccessible(prop: KMutableProperty<*>): Boolean {
        //println("Before ${prop.name}, ${prop.isAccessible}")
        if (prop.isAccessible) return true
        val bl = prop.javaField?.trySetAccessible() != false
                && prop.javaGetter?.trySetAccessible() != false
                && prop.javaSetter?.trySetAccessible() != false
        //println("After ${prop.name}, ${prop.isAccessible}")
        return bl
    }

    private fun isNonSync(property: KProperty<*>): Boolean {
        return isNonSync(property.annotations)
    }
    internal fun isNonSync(annotations: List<Annotation>): Boolean {
        return annotations.firstOrNull { (it is NonSync) }?.let { true } == true
    }

    internal fun isRootConfig(clazz: KClass<*>): Boolean {
        return clazz.annotations.any { it is RootConfig }
    }

    @Suppress("DEPRECATION")
    private fun getAction(annotations: List<Annotation>): Action? {
        return (annotations.firstOrNull {
            (it is RequiresAction)
        } as? RequiresAction ?: annotations.firstOrNull {
            (it is RequiresRestart)
        }?.let {
            RequiresAction(Action.RESTART)
        })?.action
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
        val propActions: SortedSet<Action> = sortedSetOf()
        walk(thing, "", flags) { _, _, _, v, _, annotations, _, _ ->
            val action = requiredAction(annotations, classAction)
            if (action != null) {
                propActions.add(action)
            }
            if (v is EntryParent) {
                propActions.addAll(v.actions())
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
        val ver = clazz.java.annotations.firstOrNull { it is Version } as? Version
        return ver?.version ?: 0
    }

    private fun getCompat(clazz: KClass<*>): Pair<File?, FileType> {
        val version = clazz.findAnnotation<ConvertFrom>() ?: return Pair(null, FileType.TOML)
        val dir = makeDir(version.folder, version.subfolder)
        if (!dir.second) return Pair(null, FileType.TOML)
        val fileType = fileType(version.fileName) ?: return Pair(null, FileType.TOML)
        return Pair(File(dir.first, version.fileName), fileType)
    }

    private fun fileType(fileName: String): FileType? {
        return FileType.entries.firstOrNull { fileName.endsWith(it.suffix()) }
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
        if (isNonSync(annotations)) return true
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

    private fun flatWalk(flags: Byte): Boolean {
        return flags and 16.toByte() == 16.toByte()
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
                            && if (ignoreVisibility) trySetAccessible(it) else it.visibility == KVisibility.PUBLIC
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
                    if (propVal is Walkable && !flatWalk(flags)) {
                        val newFlags = if (ignoreVisibility || isIgnoreVisibility(propVal::class)) flags or IGNORE_VISIBILITY else flags
                        walk(propVal, newPrefix, newFlags, walkAction)
                    }
                } catch (e: Throwable) {
                    FC.LOGGER.error("Critical exception caught while walking $prefix for property $property.name")
                    FC.LOGGER.error(" > Walk Flags: $flags, Ignoring Visibility: $ignoreVisibility")
                    FC.LOGGER.error(" > Exception:", e)
                    // continue without borking
                }
            }
        } catch (e: Throwable) {
            FC.LOGGER.error("Critical exception encountered while Walking through ${walkable::class.simpleName}", e)
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
                        && if (ignoreVisibility) trySetAccessible(it) else it.visibility == KVisibility.PUBLIC
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
                } catch (e: Throwable) {
                    FC.LOGGER.error("Critical exception caught while acting on a drill for $target")
                    FC.LOGGER.error(" > Drill Final Flags: $flags, Ignoring Visibility: $ignoreVisibility")
                    FC.LOGGER.error(" > Exception:", e)
                    // continue without borking
                }
            }
            if (flatWalk(flags)) return
            for ((string, property) in props) {
                if (target.startsWith(string)) {
                    try {
                        val propVal = property.get(walkable)
                        if (propVal is Walkable) {
                            val newFlags = if (ignoreVisibility || isIgnoreVisibility(propVal::class)) flags or IGNORE_VISIBILITY else flags
                            drill(propVal, target.substringAfter(delimiter), delimiter, newFlags, walkAction)
                        } else {
                            break
                        }
                    } catch (e: Throwable) {
                        FC.LOGGER.error("Critical exception caught while drilling to $target")
                        FC.LOGGER.error(" > Drill Flags: $flags, Ignoring Visibility: $ignoreVisibility")
                        FC.LOGGER.error(" > Current Property: $string, new target: ${target.substringAfter(delimiter)}")
                        FC.LOGGER.error(" > Exception:", e)
                        // continue without borking
                    }
                }
            }
        } catch (e: Throwable) {
            FC.LOGGER.error("Critical exception encountered while Drilling into ${walkable::class.simpleName}", e)
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