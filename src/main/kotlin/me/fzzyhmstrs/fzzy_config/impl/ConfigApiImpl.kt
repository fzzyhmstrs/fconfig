/*
* Copyright (c) 2024-2025 Fzzyhmstrs
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
import me.fzzyhmstrs.fzzy_config.config.ConfigSection
import me.fzzyhmstrs.fzzy_config.entry.*
import me.fzzyhmstrs.fzzy_config.nullCast
import me.fzzyhmstrs.fzzy_config.registry.SyncedConfigRegistry
import me.fzzyhmstrs.fzzy_config.result.impl.ResultApiImpl
import me.fzzyhmstrs.fzzy_config.screen.ConfigScreenProvider
import me.fzzyhmstrs.fzzy_config.updates.BasicValidationProvider
import me.fzzyhmstrs.fzzy_config.updates.UpdateManager
import me.fzzyhmstrs.fzzy_config.util.*
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.attachTo
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.map
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.outmap
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
import java.util.concurrent.CompletableFuture
import java.util.function.BiConsumer
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

    private fun debug(start: Long, phase: String, prefix: String = "") {
        FC.DEVLOG.info("{}{} in {}ms", prefix, phase, (System.currentTimeMillis() - start))
    }

    internal const val CHECK_NON_SYNC: Byte = 0
    internal const val IGNORE_NON_SYNC: Byte = 1
    internal const val CHECK_ACTIONS: Byte = 2
    internal const val IGNORE_NON_SYNC_AND_CHECK_ACTIONS: Byte = 3
    internal const val IGNORE_VISIBILITY: Byte = 4
    internal const val IGNORE_NON_SYNC_AND_IGNORE_VISIBILITY: Byte = 5
    internal const val RECORD_RESTARTS: Byte = 8
    internal const val CHECK_ACTIONS_AND_RECORD_RESTARTS: Byte = 10
    internal const val FLAT_WALK: Byte = 16
    internal const val IGNORE_NON_SYNC_AND_FLAT_WALK: Byte = 17
    internal const val CRITICAL_ERRORS_ONLY: Byte = 32
    internal const val IGNORE_NON_SYNC_AND_CRITICAL_ERRORS_ONLY: Byte = 33
    internal const val NO_WALK_ANNOTATIONS: Byte = 64

    private val configClass = Config::class
    private val configSectionClass = ConfigSection::class
    private val walkableClass = Walkable::class

    internal fun openScreen(scope: String) {
        if (isClient)
            ConfigApiImplClient.openScreen(scope)
    }

    internal fun isScreenOpen(scope: String): Boolean {
        if (isClient)
            return ConfigApiImplClient.isScreenOpen(scope)
        return false
    }

    internal fun registerScreenProvider(namespace: String, provider: ConfigScreenProvider) {
        if (isClient)
            ConfigApiImplClient.registerScreenProvider(namespace, provider)
    }

    internal fun openRestartScreen() {
        if (isClient)
            FCC.openRestartScreen()
    }

    internal fun getConfig(scope: String): Config? {
        return SyncedConfigRegistry.getConfig(scope) ?: if(isClient) ConfigApiImplClient.getClientConfig(scope) else null
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
        SyncedConfigRegistry.registerConfig(config, RegisterType.BOTH)
        return registerClient(config, configClass, noGui)
    }
    private fun <T: Config> registerSynced(config: T): T {
        SyncedConfigRegistry.registerConfig(config, RegisterType.SERVER)
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

    ///////////////////// END Registration ///////////////////////////////////////////////

    //////////////// Read, Create, Save //////////////////////////////////////////////////

    /*internal fun <T: Config> readOrCreateAndValidateAsync(configClass: () -> T, classInstance: T = configClass(), name: String = classInstance.name, folder: String = classInstance.folder, subfolder: String = classInstance.subfolder): ConfigHolder<T> {
        val future = CompletableFuture.supplyAsync {
            readOrCreateAndValidate(configClass, classInstance, name, folder, subfolder)
        }
        return ConfigHolder.Future(future)
    }*/

    private val seenLogs: MutableSet<String> = hashSetOf()

    internal fun <T: Config> readOrCreateAndValidate(configClass: () -> T, classInstance: T = configClass(), name: String = classInstance.name, folder: String = classInstance.folder, subfolder: String = classInstance.subfolder): T {
        fun log(start: Long) {
            val cfg = "$folder:$name"
            if (seenLogs.contains(cfg)) return //log once
            seenLogs.add(cfg)
            FC.LOGGER.info("Loaded config {} in {}ms", cfg, (System.currentTimeMillis() - start))
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

            val files = findFiles(dir, name, classInstance.fileType())

            if (files.fIn.exists()) {
                val fInErrorContext = ValidationResult.createMutable("Error(s) found while deserializing config [$name]!")

                val str = files.fIn.readLines().joinToString("\n")
                val classVersion = getVersion(classInstance::class)

                val readConfigResult = deserializeConfig(classInstance, str, fInErrorContext, IGNORE_NON_SYNC_AND_CHECK_ACTIONS, fileType = files.fInType)

                if (readConfigResult.test(ValidationResult.Errors.ACTION) { classInstance.saveType().incompatibleWith(it.content) }) {
                    throw IncompatibleSaveTypeException("Config $name uses SaveType SEPARATE but also has incompatible @RequiresAction with Action.RESTART flags")
                }

                val readConfig = readConfigResult.get()
                val needsUpdating = readConfigResult.test(ValidationResult.Errors.VERSION) {
                    val bl = (classVersion > it.content)
                    if (bl) readConfig.update(it.content)
                    bl
                }
                if (readConfigResult.isError() || needsUpdating) {
                    if (readConfigResult.isError()) {
                        readConfigResult.log()
                    }
                    CompletableFuture.runAsync( {
                        val fOutErrorContext = ValidationResult.createMutable("Error(s) encountered while re-serializing corrected config [$name]! Output may not be complete.")
                        val correctedConfig = serializeConfig(readConfig, fOutErrorContext, fileType = files.fOutType)
                        if (correctedConfig.isError()) {
                            correctedConfig.log()
                        }
                        writeFile(files.fOut, correctedConfig.get(), name, "correcting errors or updating version", files.fIn)
                    }, ThreadUtils.EXECUTOR)
                } else if (files.fIn != files.fOut) {
                    val fOutErrorContext = ValidationResult.createMutable("Error(s) encountered while re-serializing config [$name] to new file location/type! Output may not be complete.")
                    val newFormatConfig = serializeConfig(readConfig, fOutErrorContext, fileType = files.fOutType)
                    if (newFormatConfig.isError()) {
                        newFormatConfig.log()
                    }
                    writeFile(files.fOut, newFormatConfig.get(), name, "moving config to new file format", files.fIn)
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
                    val fConvertErrorContext = ValidationResult.createMutable("Error(s) encountered while converting old file for [$name]! Output may not be complete.")
                    val convertedConfigResult = deserializeConfig(classInstance, str, fConvertErrorContext, IGNORE_NON_SYNC_AND_CHECK_ACTIONS, fileType = oldFilePair.second)
                    if (convertedConfigResult.isError()) {
                        convertedConfigResult.log()
                    }
                    oldFile.delete()
                }
                val fOutErrorContext = ValidationResult.createMutable("Error(s) encountered while re-serializing converted config for [$name]! Output may not be complete.")
                val reConvertedConfigResult = serializeConfig(classInstance, fOutErrorContext, fileType = files.fOutType)
                if (reConvertedConfigResult.isError()) {
                    reConvertedConfigResult.log()
                }
                writeFile(files.fOut, reConvertedConfigResult.get(), name, "converting old config")
                log(start)
                return classInstance
            }
        } catch (e: IncompatibleSaveTypeException) {
            throw IllegalStateException("Config can't be created!", e)
        } catch (e: Throwable) {
            FC.LOGGER.error("Critical error encountered while reading or creating [$name]. Using default config.", e)
            return configClass()
        }
    }

    internal fun <T : Config> save(name: String, d: File, configClass: T) {
        try {
            val (dir, dirCreated) = makeDir(d)
            if (!dirCreated) {
                FC.LOGGER.error("Couldn't create directory [$dir]. Failed to save config file $name!")
                return
            }

            val files = findFiles(dir, name, configClass.fileType())

            if (files.fOut.exists() || files.fOut.createNewFile()) {
                val fOutErrorContext = ValidationResult.createMutable("Error(s) encountered while saving config [$name]! Output may not be complete.")
                val result = serializeConfig(configClass, fOutErrorContext, fileType = files.fOutType)
                if (result.isError()) {
                    result.log()
                }
                FC.LOGGER.info("Saved config $name to file ${files.fOut}")
                files.fOut.lastModified()
                writeFile(files.fOut, result.get(), name, "saving config", files.fIn)
            } else  {
                FC.LOGGER.error("Failed to save config file $name to ${files.fOut}, config not saved.")
            }
        } catch (e: Throwable) {
            FC.LOGGER.error("Critical error while trying to save config file $name!", e)
        }
    }

    internal fun <T : Config> save(configClass: T) {
        save(configClass.name, configClass.getDir(), configClass)
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
        return try {
            ValidationResult.success(Toml.encodeToString(toml))
        } catch (e: Throwable) {
            ValidationResult.error("", FileType.TOML.encodeType, "Exception encountered while encoding TOML", e)
        }
    }

    internal fun decodeToml(string: String): ValidationResult<TomlElement> {
        return try {
            ValidationResult.success(Toml.parseToTomlTable(string))
        } catch (e: Throwable) {
            return  ValidationResult.error(TomlNull, FileType.TOML.decodeType, "Exception encountered while decoding TOML", e)
        }
    }

    internal fun encodeJson(toml: TomlElement): ValidationResult<String> {
        return try {
            val jsonElement = TomlOps.INSTANCE.convertTo(JsonOps.INSTANCE, toml)
            ValidationResult.success(gson.toJson(jsonElement))
        } catch (e: Throwable) {
            ValidationResult.error("", FileType.JSON.encodeType, "Exception encountered while encoding JSON", e)
        }
    }

    internal fun decodeJson(string: String): ValidationResult<TomlElement> {
        return try {
            val tomlElement = JsonOps.INSTANCE.convertTo(TomlOps.INSTANCE, JsonParser.parseString(string))
            ValidationResult.success(tomlElement)
        } catch (e: Throwable) {
            return  ValidationResult.error(TomlNull, FileType.JSON.decodeType, "Exception encountered while decoding JSON", e)
        }
    }

    internal fun encodeJson5(toml: TomlElement): ValidationResult<String> {
        if (toml !is TomlTable) return ValidationResult.error("",  FileType.JSON5.encodeType, "Toml provided to json5 encoder not a table")
        return try {
            val table = toml.asTomlTable()
            val obj = TomlOps.convertToJson5(table)
            ValidationResult.success(obj.toJson(true, true))
        } catch (e: Throwable) {
            ValidationResult.error("", FileType.JSON5.encodeType, "Exception encountered while encoding JSON5", e)
        }
    }

    internal fun decodeJson5(string: String): ValidationResult<TomlElement> {
        return try {
            val jsonObject = jankson.load(string)
            val jsonString = jsonObject.toJson(false, false)
            val tomlElement = JsonOps.INSTANCE.convertTo(TomlOps.INSTANCE, JsonParser.parseString(jsonString))
            ValidationResult.success(tomlElement)
        } catch (e: Throwable) {
            return  ValidationResult.error(TomlNull, FileType.JSON5.decodeType, "Critical error encountered while decoding JSON5", e)
        }
    }

    ///////////////// End Encode-Decode //////////////////////////////////////////////////

    ///////////////// Serialize //////////////////////////////////////////////////////////

    @Deprecated("Use overload with Mutable")
    internal fun <T: Any> serializeToToml(config: T, errorBuilder: MutableList<String>, flags: Byte = IGNORE_NON_SYNC): ValidationResult<TomlTable> {
        val builder = ValidationResult.createMutable()
        val result = serializeToToml(config, builder, flags)
        builder.entry.log { s, _ -> errorBuilder.add(s) }
        return result
    }

    internal fun <T: Any> serializeToToml(config: T, errorHeader: String = "", flags: Byte = IGNORE_NON_SYNC): ValidationResult<TomlTable> {
        return serializeToToml(config, ValidationResult.createMutable(errorHeader), flags)
    }

    internal fun <T: Any> serializeToToml(config: T, errorBuilder: ValidationResult.ErrorEntry.Mutable, flags: Byte = IGNORE_NON_SYNC): ValidationResult<TomlTable> {
        //used to build a TOML table piece by piece
        val toml = TomlTableBuilder()
        try {
            val clazz = config::class
            //apply header information as applicable
            if (config is Config) {
                val version = getVersion(clazz)
                val headerAnnotations = tomlHeaderAnnotations(clazz).toMutableList()
                headerAnnotations.add(TomlHeaderComment("Don't change this! Version used to track needed updates."))
                toml.element("version", TomlLiteral(version), headerAnnotations.map { TomlComment(it.text) })
            }
            val ignoreVisibility = isIgnoreVisibility(clazz) || ignoreVisibility(flags)

            //java fields are ordered in declared order, apparently not so for Kotlin properties. use these first to get ordering. skip Transient
            //generate an index map, so I can order the properties based on name
            val orderById = clazz.java.declaredFields.filter { !isTransient(it.modifiers) }.withIndex().associate { it.value.name to it.index }.toMutableMap()
            for (sup in clazz.allSuperclasses) {
                if (sup == configClass) continue //ignore Config itself, as that has state we don't need
                if (sup == configSectionClass) continue //ignore ConfigSection itself, as that has state we don't need
                orderById.putAll(sup.java.declaredFields.filter { !isTransient(it.modifiers) }.withIndex().associate { it.value.name to it.index })
            }

            //kotlin member properties filtered by [field map contains it && if NonSync matters, it isn't NonSync]. NonSync does not matter by default
            val props = clazz.memberProperties.filter {
                it is KMutableProperty<*>
                        && (if (ignoreNonSync(flags)) true else !isNonSync(it))
                        && !isTransient(it.javaField?.modifiers ?: Modifier.TRANSIENT)
                        && if(ignoreVisibility) trySetAccessible(it) else it.visibility == KVisibility.PUBLIC
            }.sortedBy { orderById[it.name] }

            for (prop in props.cast<List<KMutableProperty1<T, *>>>()) {
                //get the actual [thing] from the property
                val propVal = prop.get(config)
                if (propVal is EntryTransient) continue
                //if(ignoreVisibility) (prop.javaField?.trySetAccessible())
                //things name
                val name = prop.name
                //serialize the element. EntrySerializer elements will have a set serialization method
                val elResult = if (propVal is EntrySerializer<*>) { //is EntrySerializer
                    try {
                        val newFlags = if (ignoreVisibility || isIgnoreVisibility(prop.annotations)) flags or IGNORE_VISIBILITY else flags
                        propVal.serializeEntry(null, newFlags)
                    } catch (e: Throwable) {
                        ValidationResult.error(TomlNull, ValidationResult.Errors.SERIALIZATION, "Exception encountered serializing [$name]", e)
                    }
                } else if (propVal != null) { //fallback is to use by-type TOML serialization
                    val basicValidation = UpdateManager.basicValidationStrategy(propVal, prop, name)
                    if (basicValidation != null) {
                        val newFlags = if (ignoreVisibility || isIgnoreVisibility(prop.annotations)) flags or IGNORE_VISIBILITY else flags
                        basicValidation.trySerialize(propVal, newFlags)
                    } else {
                        try {
                            encodeToTomlElement(propVal, prop.returnType)?.let { ValidationResult.success(it) } ?: ValidationResult.error(TomlNull, ValidationResult.Errors.SERIALIZATION, "Couldn't serialize raw field [$name]")
                        } catch (e: Throwable) {
                            ValidationResult.error(TomlNull, ValidationResult.Errors.SERIALIZATION, "Exception encountered with raw data while serializing [$name]", e)
                        }
                    }
                    //TomlNull for properties with Null state (improper state, no config values should be nullable)
                } else {
                    ValidationResult.error(TomlNull, ValidationResult.Errors.SERIALIZATION, "Property [$name] was null during serialization!")
                }.attachTo(errorBuilder)
                //scrape all the TomlAnnotations associated
                val tomlAnnotations = tomlAnnotations(prop)
                //add the element to the TomlTable, with annotations
                toml.element(name, elResult.get(), tomlAnnotations)

            }
        } catch (e: Throwable) {
            errorBuilder.addError(ValidationResult.Errors.SERIALIZATION, "Exception encountered while serializing config, output may not be complete!", e)
            return ValidationResult.ofMutable(toml.build(), errorBuilder)
        }
        //serialize the TomlTable to its string representation
        return ValidationResult.ofMutable(toml.build(), errorBuilder)
    }

    internal fun <T: Config> serializeConfigSafe(config: T, errorHeader: String = "", flags: Byte = IGNORE_NON_SYNC, fileType: FileType = FileType.TOML): ValidationResult<String> {
        return serializeConfig(config, ValidationResult.createMutable(errorHeader), flags, fileType)
    }

    internal fun <T: Config> serializeConfigSafe(config: T, errorBuilder: ValidationResult.ErrorEntry.Mutable, flags: Byte = IGNORE_NON_SYNC, fileType: FileType = FileType.TOML): ValidationResult<String> {
        return serializeConfig(config, errorBuilder, flags, fileType)
    }

    @Deprecated("Use overload with Mutable param")
    internal fun <T: Any> serializeConfig(config: T, errorBuilder: MutableList<String>, flags: Byte = IGNORE_NON_SYNC, fileType: FileType = FileType.TOML): ValidationResult<String> {
        @Suppress("DEPRECATION")
        return serializeToToml(config, errorBuilder, flags).outmap(fileType::encode)
    }

    internal fun <T: Any> serializeConfig(config: T, errorHeader: String = "", flags: Byte = IGNORE_NON_SYNC, fileType: FileType = FileType.TOML): ValidationResult<String> {
        return serializeToToml(config, ValidationResult.createMutable(errorHeader), flags).outmap(fileType::encode)
    }

    internal fun <T: Any> serializeConfig(config: T, errorBuilder: ValidationResult.ErrorEntry.Mutable, flags: Byte = IGNORE_NON_SYNC, fileType: FileType = FileType.TOML): ValidationResult<String> {
        return serializeToToml(config, errorBuilder, flags).outmap(fileType::encode)
    }

    private fun <T: Config, M> serializeUpdateToToml(config: T, manager: M, errorBuilder: ValidationResult.ErrorEntry.Mutable, flags: Byte = CHECK_NON_SYNC): ValidationResult<TomlTable> where M: UpdateManager, M: BasicValidationProvider {
        val toml = TomlTableBuilder()
        try {
            walk(config, config.getId().toTranslationKey(), flags) { _, _, str, v, prop, _, _, _ ->
                if(manager.hasUpdate(str)) {
                    if(v is EntrySerializer<*>) {
                        toml.element(str, v.serializeEntry(null, flags).attachTo(errorBuilder).get())
                    } else if (v != null) {
                        val basicValidation = manager.basicValidationStrategy(v, prop, str)
                        if (basicValidation != null) {
                            toml.element(str, basicValidation.trySerialize(v, flags).attachTo(errorBuilder).get())
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            errorBuilder.addError(ValidationResult.Errors.SERIALIZATION, "Critical error encountered while serializing config update, update may be incomplete!", e)
            return ValidationResult.ofMutable(toml.build(), errorBuilder)
        }
        return ValidationResult.ofMutable(toml.build(), errorBuilder)
    }

    internal fun <T: Config, M> serializeUpdate(config: T, manager: M, errorHeader: String = "", flags: Byte = CHECK_NON_SYNC): ValidationResult<String> where M: UpdateManager, M: BasicValidationProvider {
        return serializeUpdateToToml(config, manager, ValidationResult.createMutable(errorHeader), flags).map(Toml::encodeToString)
    }

    internal fun <T: Config, M> serializeUpdate(config: T, manager: M, errorBuilder: ValidationResult.ErrorEntry.Mutable, flags: Byte = CHECK_NON_SYNC): ValidationResult<String> where M: UpdateManager, M: BasicValidationProvider {
        return serializeUpdateToToml(config, manager, errorBuilder, flags).map(Toml::encodeToString)
    }

    internal fun serializeEntry(entry: Entry<*, *>, flags: Byte = IGNORE_NON_SYNC): ValidationResult<String> {
        return entry.serializeEntry(null, flags).map { TomlTableBuilder().element("entry", it) }.map (Toml::encodeToString)
    }

    ///////////////// END Serialize //////////////////////////////////////////////////////

    ///////////////// Deserialize ////////////////////////////////////////////////////////

    @Deprecated("Use overload with Mutable input")
    internal fun <T: Any> deserializeFromToml(config: T, toml: TomlElement, errorBuilder: MutableList<String>, flags: Byte = IGNORE_NON_SYNC): ValidationResult<T> {
        val builder = ValidationResult.createMutable()
        val result = deserializeFromToml(config, toml, builder, flags)
        result.log { s, _ -> errorBuilder.add(s) }
        return result
    }

    internal fun <T: Any> deserializeFromToml(config: T, toml: TomlElement, errorHeader: String = "", flags: Byte = IGNORE_NON_SYNC): ValidationResult<T> {
        return deserializeFromToml(config, toml, ValidationResult.createMutable(errorHeader), flags)
    }

    internal fun <T: Any> deserializeFromToml(config: T, toml: TomlElement, errorBuilder: ValidationResult.ErrorEntry.Mutable, flags: Byte = IGNORE_NON_SYNC): ValidationResult<T> {
        if (toml !is TomlTable) {
            errorBuilder.addError(ValidationResult.Errors.FILE_STRUCTURE, "TomlElement passed to deserializeFromToml not a TomlTable! Deserialization aborted.")
            return ValidationResult.ofMutable(config, errorBuilder)
        }
        @Suppress("UNCHECKED_CAST")
        val clazz = config::class as KClass<T>
        val tomlMap = HashMap(toml)

        try {
            val checkActions = checkActions(flags)
            val recordRestarts = if (!checkActions) false else recordRestarts(flags)
            val globalAction = getAction(clazz.annotations)
            val ignoreVisibility = ignoreVisibility(flags) || isIgnoreVisibility(clazz)

            val propsRaw = clazz.memberProperties

            val props = propsRaw.filter {
                it is KMutableProperty<*>
                        && (if (ignoreNonSync(flags)) true else !isNonSync(it))
                        && !isTransient(it.javaField?.modifiers ?: Modifier.TRANSIENT)
                        && if(ignoreVisibility) trySetAccessible(it) else it.visibility == KVisibility.PUBLIC
            }

            for (prop in props.cast<List<KMutableProperty1<T, *>>>()) {
                val propVal = prop.get(config)
                if (propVal is EntryTransient) continue
                val name = prop.name
                val tomlElement = tomlMap.remove(name)
                if (tomlElement == null || tomlElement is TomlNull) {
                    errorBuilder.addError(ValidationResult.Errors.DESERIALIZATION, "TomlElement [$name] was missing or null.")
                    continue
                }
                if (propVal is EntryDeserializer<*>) { //is EntryDeserializer
                    val action = requiredAction(checkActions, prop, globalAction)
                    val newFlags = if (ignoreVisibility || isIgnoreVisibility(prop.annotations)) flags or IGNORE_VISIBILITY else flags
                    if (propVal is Supplier<*> && action != null) {
                        errorBuilder.addError(ValidationResult.Errors.ACTION) { b -> b.content(action) }
                        val before = propVal.get()
                        propVal.deserializeEntry(tomlElement, name, newFlags).also { r ->
                            if (action.restartPrompt) {
                                if (propVal.deserializedChanged(before, r.get())) {
                                    if (recordRestarts) {
                                        errorBuilder.addError(ValidationResult.Errors.RESTART) { b -> b.content(action).message(((config as? Config)?.getId()?.toTranslationKey() ?: "") + "." + name) }
                                    } else {
                                        errorBuilder.addError(ValidationResult.Errors.RESTART) { b -> b.content(action) }
                                    }
                                }
                            }
                        }
                    } else {
                        propVal.deserializeEntry(tomlElement, name, newFlags)
                    }.attachTo(errorBuilder)
                } else {
                    val basicValidation = UpdateManager.basicValidationStrategy(propVal, prop, name)
                    if (basicValidation != null) {
                        val newFlags = if (ignoreVisibility || isIgnoreVisibility(prop.annotations)) flags or IGNORE_VISIBILITY else flags
                        val result = basicValidation.deserializeEntry(tomlElement, name, newFlags)
                        try {
                            val action = requiredAction(checkActions, prop, globalAction)
                            if (action != null) {
                                errorBuilder.addError(ValidationResult.Errors.ACTION) { b -> b.content(action) }
                                if (action.restartPrompt) {
                                    if (basicValidation.deserializedChanged(propVal, result.get())) {
                                        if (recordRestarts) {
                                            errorBuilder.addError(ValidationResult.Errors.RESTART) { b -> b.content(action).message(((config as? Config)?.getId()?.toTranslationKey() ?: "") + "." + name) }
                                            //restartRecords.add(((config as? Config)?.getId()?.toTranslationKey() ?: "") + "." + name)
                                        } else {
                                            errorBuilder.addError(ValidationResult.Errors.RESTART) { b -> b.content(action) }
                                        }
                                    }
                                }
                            }
                            if (ignoreVisibility) trySetAccessible(prop)
                            prop.setter.call(config, result.get())
                        } catch (e: Throwable) {
                            errorBuilder.addError(ValidationResult.Errors.DESERIALIZATION, "Exception while deserializing basic validation [$name]", e)
                        }
                        errorBuilder.addError(result)
                    } else {
                        try {
                            val action = requiredAction(checkActions, prop, globalAction)
                            if(action != null) {
                                errorBuilder.addError(ValidationResult.Errors.ACTION) { b -> b.content(action) }
                                if(ignoreVisibility) trySetAccessible(prop)
                                prop.setter.call(config, validateNumber(decodeFromTomlElement(tomlElement, prop.returnType), prop).also {
                                    if (action.restartPrompt && propVal != it) {
                                        if (recordRestarts) {
                                            errorBuilder.addError(ValidationResult.Errors.RESTART) { b -> b.content(action).message(((config as? Config)?.getId()?.toTranslationKey() ?: "") + "." + name) }
                                        } else {
                                            errorBuilder.addError(ValidationResult.Errors.RESTART) { b -> b.content(action) }
                                        }
                                    }
                                })
                            } else {
                                if(ignoreVisibility) trySetAccessible(prop)
                                prop.setter.call(config, validateNumber(decodeFromTomlElement(tomlElement, prop.returnType), prop))
                            }
                        } catch (e: Throwable) {
                            errorBuilder.addError(ValidationResult.Errors.DESERIALIZATION, "Exception while deserializing raw field [$name]", e)
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            errorBuilder.addError(ValidationResult.Errors.DESERIALIZATION, "Exception encountered while deserializing TOML", e)
        }
        if (tomlMap.keys.any { it != "version" }) {
            errorBuilder.addError(ValidationResult.Errors.FILE_STRUCTURE, "Config file for ${clazz.simpleName} contained obsolete or non-matching keys: ${tomlMap.keys.filter { it != "version" }}")
        }
        return ValidationResult.ofMutable(config, errorBuilder)
    }

    internal fun <T: Config> deserializeConfigSafe(config: T, string: String, errorHeader: String = "", flags: Byte = IGNORE_NON_SYNC, fileType: FileType = FileType.TOML): ValidationResult<T> {
        return deserializeConfig(config, string, errorHeader, flags, fileType)
    }

    @Deprecated("Use overload with Mutable input")
    internal fun <T: Any> deserializeConfig(config: T, string: String, errorBuilder: MutableList<String>, flags: Byte = IGNORE_NON_SYNC, fileType: FileType = FileType.TOML): ValidationResult<T> {
        val builder = ValidationResult.createMutable()
        val result = deserializeConfig(config, string, builder, flags, fileType)
        builder.entry.log { s, _ -> errorBuilder.add(s) }
        return result
    }

    internal fun <T: Any> deserializeConfig(config: T, string: String, errorHeader: String = "", flags: Byte = IGNORE_NON_SYNC, fileType: FileType = FileType.TOML): ValidationResult<T> {
        return deserializeConfig(config, string, ValidationResult.createMutable(errorHeader), flags, fileType)
    }

    internal fun <T: Any> deserializeConfig(config: T, string: String, errorBuilder: ValidationResult.ErrorEntry.Mutable, flags: Byte = IGNORE_NON_SYNC, fileType: FileType = FileType.TOML): ValidationResult<T> {
        val toml = try {
            val tomlResult = fileType.decode(string)
            if (tomlResult.isError()) {
                errorBuilder.addError(ValidationResult.Errors.FILE_STRUCTURE, "Toml for config ${config.javaClass.canonicalName} is corrupted or improperly formatted for parsing")
                return ValidationResult.ofMutable(config, errorBuilder)
            }
            tomlResult.get().asTomlTable()
        } catch (e: Throwable) {
            errorBuilder.addError(ValidationResult.Errors.FILE_STRUCTURE, "Config file for ${config.javaClass.canonicalName} is critically corrupted or improperly formatted for parsing", e)
            return ValidationResult.ofMutable(config, errorBuilder)
        }
        if(toml.containsKey("version")) {
            try {
                toml["version"]?.asTomlLiteral()?.toInt()?.let {
                    errorBuilder.addError(ValidationResult.Errors.VERSION) { b -> b.content(it) }
                }
            }catch (e: Throwable) {
                errorBuilder.addError(ValidationResult.Errors.FILE_STRUCTURE, "Exception while parsing config version", e)
            }
        }
        return deserializeFromToml(config, toml, errorBuilder, flags)
    }

    private fun <T: Any> deserializeUpdateFromToml(config: T, toml: TomlElement, errorBuilder: ValidationResult.ErrorEntry.Mutable, flags: Byte = CHECK_NON_SYNC): ValidationResult<ConfigContext<T>> {
        try {
            val checkActions = checkActions(flags)
            val recordRestarts = if (!checkActions) false else recordRestarts(flags)
            val globalAction = getAction(config::class.annotations)

            if (toml !is TomlTable) {
                errorBuilder.addError(ValidationResult.Errors.FILE_STRUCTURE, "TomlElement passed to deserializeUpdateFromToml not a TomlTable! Deserialization aborted.")
                return ValidationResult.ofMutable(ConfigContext(config), errorBuilder)
            }
            val id = (config as? Config)?.getId()?.toTranslationKey() ?: ""
            walk(config, id, flags) { c, _, str, v, prop, _, _, _ -> toml[str]?.let {
                if(v is EntryDeserializer<*>) {
                    val action = requiredAction(checkActions, prop, globalAction)
                    if(v is Supplier<*> && action != null) {
                        errorBuilder.addError(ValidationResult.Errors.ACTION) { b -> b.content(action) }
                        val before = v.get()
                        v.deserializeEntry(it, str, flags).also { r ->
                            if (action.restartPrompt) {
                                if(v.deserializedChanged(before, r.get())) {
                                    if (recordRestarts) {
                                        errorBuilder.addError(ValidationResult.Errors.RESTART) { b -> b.content(action).message(((config as? Config)?.getId()?.toTranslationKey() ?: "") + "." + str) }
                                    } else {
                                        errorBuilder.addError(ValidationResult.Errors.RESTART) { b -> b.content(action) }
                                    }
                                }
                            }
                        }
                    } else {
                        v.deserializeEntry(it, str, flags)
                    }.attachTo(errorBuilder)
                } else if (v != null) {
                    val basicValidation = UpdateManager.getValidation(v, prop, str, id, this.isClient)
                    if (basicValidation != null) {
                        val result = basicValidation.deserializeEntry(it, str, flags)
                        try {
                            val action = requiredAction(checkActions, prop, globalAction)
                            if(action != null) {
                                errorBuilder.addError(ValidationResult.Errors.ACTION) { b -> b.content(action) }
                                if (action.restartPrompt) {
                                    if (basicValidation.deserializedChanged(v, result.get())) {
                                        if (recordRestarts) {
                                            errorBuilder.addError(ValidationResult.Errors.RESTART) { b -> b.content(action).message(((config as? Config)?.getId()?.toTranslationKey() ?: "") + "." + str) }
                                        } else {
                                            errorBuilder.addError(ValidationResult.Errors.RESTART) { b -> b.content(action) }
                                        }
                                    }
                                }
                            }
                            prop.setter.call(c, result.get()) //change?
                        } catch(e: Throwable) {
                            errorBuilder.addError(ValidationResult.Errors.DESERIALIZATION, "Exception while deserializing basic validation [$str] for update", e)
                        }
                        errorBuilder.addError(result)
                    }
                }
            }
            }
        } catch(e: Throwable) {
            errorBuilder.addError(ValidationResult.Errors.DESERIALIZATION, "Exception encountered while deserializing TOML update", e)
        }
        return ValidationResult.ofMutable(ConfigContext(config), errorBuilder)
    }

    internal fun <T: Config> deserializeUpdate(config: T, string: String, errorHeader: String = "", flags: Byte = CHECK_NON_SYNC): ValidationResult<ConfigContext<T>> {
        return deserializeUpdate(config, string, ValidationResult.createMutable(errorHeader), flags)
    }

    internal fun <T: Config> deserializeUpdate(config: T, string: String, errorBuilder: ValidationResult.ErrorEntry.Mutable, flags: Byte = CHECK_NON_SYNC): ValidationResult<ConfigContext<T>> {
        val toml = try {
            Toml.parseToTomlTable(string)
        } catch (e: Throwable) {
            return ValidationResult.error(ConfigContext(config), ValidationResult.Errors.FILE_STRUCTURE, "Config ${config.javaClass.canonicalName} is corrupted or improperly formatted for parsing", e)
        }
        return deserializeUpdateFromToml(config, toml, errorBuilder, flags)
    }

    internal fun <T> deserializeEntry(entry: Entry<T, *>, string: String, scope: String, flags: Byte = CHECK_NON_SYNC): ValidationResult<out T?> {
        val toml = try {
            Toml.parseToTomlTable(string)
        } catch (e: Throwable) {
            return ValidationResult.error(null, ValidationResult.Errors.FILE_STRUCTURE, "Toml $string isn't properly formatted to be deserialized", e)
        }
        val element = toml["entry"] ?: return ValidationResult.error(null, ValidationResult.Errors.DESERIALIZATION, "Toml $string doesn't contain needed 'entry' key")
        return entry.deserializeEntry(element, scope, flags)
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
            return ValidationResult.error(emptyList(), ValidationResult.Errors.ACCESS_VIOLATION, "Update for $id is corrupted or improperly formatted for parsing")
        }
        val list: MutableList<String> = mutableListOf()
        val playerPermLevel = getPlayerPermissionLevel(player)

        if (playerPermLevel != clientPermissions) {
            return ValidationResult.error(emptyList(), ValidationResult.Errors.ACCESS_VIOLATION, "Client permission level does not match server permission level!")
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
            return ValidationResult.error(emptyList(), ValidationResult.Errors.ACCESS_VIOLATION, "Exception while validating permissions; defaulting to update rejection to be safe", e)
        }
        return ValidationResult.predicated(list, list.isEmpty(), ValidationResult.Errors.ACCESS_VIOLATION) { b -> b.content("Config updated without proper permissions!") }
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

    private fun makeDir(dir: File): Pair<File, Boolean> {
        if (!dir.exists() && !dir.mkdirs()) {
            FC.LOGGER.error("Could not create directory $dir")
            return Pair(dir, false)
        }
        return Pair(dir, true)
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

    @Synchronized
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

    private fun findFiles(dir: File, name: String, outputType: FileType): FileResult {
        var fIn = File(dir, FcText.concat(name, outputType.suffix()))
        var fInType: FileType = outputType
        val fOut: File
        val fOutType: FileType = outputType
        if (fIn.exists()) {
            fOut = fIn
        } else {
            val candidateFIInfo = FileType.entries.filter { it != outputType }.firstNotNullOfOrNull {
                File(dir,  FcText.concat(name, it.suffix())).takeIf { f -> f.exists() }?.let { f -> Pair(f, it) }
            }
            fIn = candidateFIInfo?.first ?: fIn
            fInType = candidateFIInfo?.second ?: fInType
            fOut = File(dir,  FcText.concat(name, outputType.suffix()))
        }
        return FileResult(fIn, fInType, fOut, fOutType)
    }

    private class FileResult(val fIn: File, val fInType: FileType, val fOut: File, val fOutType: FileType)

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

    ///////////////// END Utilities //////////////////////////////////////////////////////

    ///////////////// Reflection /////////////////////////////////////////////////////////

    private fun isIgnoreVisibility(annotations: List<Annotation>): Boolean {
        return annotations.any { it is IgnoreVisibility }
    }

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
        return annotations.any { it is NonSync }
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

    private fun requiredAction(checkActions: Boolean, setting: KCallable<*>, classAction: Action?): Action? {
        if (!checkActions) return null
        val settingAction = getAction(setting.annotations)
        if (settingAction == null && classAction == null) return null
        if (settingAction == null) return classAction
        if (classAction == null) return settingAction
        return if (settingAction.isPriority(classAction)) {
            settingAction
        } else {
            classAction
        }
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
        val noComment = property.annotations.none { it is Comment || it is TomlComment }
        return property.annotations.map { mapJvmAnnotations(it, noComment) }.filter { it is TomlComment || it is TomlInline || it is TomlBlockArray || it is TomlMultilineString || it is TomlLiteralString || it is TomlInteger }
    }

    private fun mapJvmAnnotations(input: Annotation, noComment: Boolean): Annotation {
        return when(input) {
            is Comment -> TomlComment(input.value)
            is Inline -> TomlInline()
            is BlockArray -> TomlBlockArray(input.itemsPerLine)
            is MultilineString -> TomlMultilineString()
            is LiteralString -> TomlLiteralString()
            is Integer -> TomlInteger(input.base, input.group)
            is Translatable.Desc -> if (noComment && input.lang == "en_us") TomlComment(input.value) else input
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

    private fun criticalOnly(flags: Byte): Boolean {
        return flags and 32.toByte() == 32.toByte()
    }

    private fun noAnnotations(flags: Byte): Boolean {
        return flags and 64.toByte() == 64.toByte()
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
            val orderById = walkable::class.java.declaredFields.withIndex().associate { it.value.name to it.index }
            val globalAnnotations = walkable::class.annotations
            val walkCallback = WalkCallback(walkable)
            for (property in walkable.javaClass.kotlin.memberProperties
                .filter {
                    it is KMutableProperty<*>
                            && if (ignoreVisibility) trySetAccessible(it) else it.visibility == KVisibility.PUBLIC
                            && (if (ignoreNonSync(flags)) true else !isNonSync(it))
                            && !isTransient(it.javaField?.modifiers ?: Modifier.TRANSIENT)
                }.sortedBy {
                    orderById[it.name]
                }
            ) {
                try {
                    val newPrefix = FcText.concat(prefix, ".", property.name)
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

    private class IncompatibleSaveTypeException(message: String): RuntimeException(message)
}