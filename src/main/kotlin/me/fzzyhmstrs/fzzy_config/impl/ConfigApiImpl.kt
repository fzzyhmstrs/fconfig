package me.fzzyhmstrs.fzzy_config.impl

import kotlinx.serialization.KSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.serializer
import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.annotations.*
import me.fzzyhmstrs.fzzy_config.api.RegisterType
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.entry.Entry
import me.fzzyhmstrs.fzzy_config.entry.EntryDeserializer
import me.fzzyhmstrs.fzzy_config.entry.EntrySerializer
import me.fzzyhmstrs.fzzy_config.registry.SyncedConfigRegistry
import me.fzzyhmstrs.fzzy_config.updates.UpdateManager
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.validation.BasicValidationProvider
import net.fabricmc.api.EnvType
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.entity.player.PlayerEntity
import net.peanuuutz.tomlkt.*
import java.io.File
import java.lang.reflect.Modifier
import java.lang.reflect.Modifier.isTransient
import kotlin.reflect.*
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

object ConfigApiImpl {

    private val isClient by lazy {
        FabricLoader.getInstance().environmentType == EnvType.CLIENT
    }

    internal fun <T: Config> registerConfig(config: T, registerType: RegisterType): T{
        return when(registerType){
            RegisterType.BOTH -> registerBoth(config)
            RegisterType.SYNC -> registerSynced(config)
            RegisterType.CLIENT -> registerClient(config)
        }
    }

    private fun <T: Config> registerBoth(config: T): T{
        SyncedConfigRegistry.registerConfig(config)
        return registerClient(config)
    }
    private fun <T: Config> registerSynced(config: T): T{
        SyncedConfigRegistry.registerConfig(config)
        return config
    }
    private fun <T: Config> registerClient(config: T): T{
        if(isClient)
            ConfigApiImplClient.registerConfig(config)
        return config
    }

    internal fun <T: Config> registerAndLoadConfig(configClass: () -> T, registerType: RegisterType): T{
        return when(registerType){
            RegisterType.BOTH -> registerAndLoadBoth(configClass)
            RegisterType.SYNC -> registerAndLoadSynced(configClass)
            RegisterType.CLIENT -> registerAndLoadClient(configClass)
        }
    }
    private fun <T: Config> registerAndLoadBoth(configClass: () -> T): T{
        return registerBoth(readOrCreateAndValidate(configClass))
    }
    private fun <T: Config> registerAndLoadSynced(configClass: () -> T): T{
        return registerSynced(readOrCreateAndValidate(configClass))
    }
    private fun <T: Config> registerAndLoadClient(configClass: () -> T): T{
        return registerClient(readOrCreateAndValidate(configClass))
    }

    internal fun <T: Config> readOrCreateAndValidate(name: String, folder: String = "", subfolder: String = "", configClass: () -> T): T{
        //wrap entire method in a try-catch. don't need to have config problems causing a hard crash, just fall back
        try {
            //create our directory, or bail if we can't for some reason
            val (dir,dirCreated) = makeDir(folder, subfolder)
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
                        val fErrorsOutResult = ValidationResult.error(
                            true,
                            "Critical error(s) encountered while re-serializing corrected Config Class! Output may not be complete."
                        )
                        fErrorsOutResult.writeError(fErrorsOut)
                    }
                    f.writeText(correctedConfig)
                }
                if (needsUpdating){
                    readConfig.update(readVersion)
                    val fErrorsOut = mutableListOf<String>()
                    val updatedConfig = serializeConfig(readConfig,fErrorsOut)
                    if (fErrorsOut.isNotEmpty()){
                        val fErrorsOutResult = ValidationResult.error(
                            true,
                            "Critical error(s) encountered while re-serializing updated Config Class! Output may not be complete."
                        )
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
                    val fErrorsOutResult = ValidationResult.error(
                        true,
                        "Critical error(s) encountered while re-serializing corrected Config Class! Output may not be complete."
                    )
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

    internal fun <T: Config> readOrCreateAndValidate(configClass: () -> T): T{
        val tempInstance = configClass()
        return readOrCreateAndValidate(tempInstance.name,tempInstance.folder,tempInstance.subfolder, configClass)
    }

    internal fun <T : Config> save(name: String, folder: String = "", subfolder: String = "", configClass: T) {
        try {
            val (dir,dirCreated) = makeDir(folder, subfolder)
            if (!dirCreated) {
                return
            }
            val f = File(dir, "$name.toml")
            if (f.exists()) {
                val fErrorsOut = mutableListOf<String>()
                val str = serializeConfig(configClass, fErrorsOut)
                if (fErrorsOut.isNotEmpty()){
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
                if (fErrorsOut.isNotEmpty()){
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
        save(configClass.name,configClass.folder,configClass.subfolder, configClass)
    }

    internal fun openScreen(scope: String){
        if (isClient)
            ConfigApiImplClient.openScreen(scope)
    }

    internal fun <T: Any> serializeToToml(config: T, errorBuilder: MutableList<String>, ignoreNonSync: Boolean = true): TomlElement{
        //used to build a TOML table piece by piece
        val toml = TomlTableBuilder()
        val version = getVersion(config::class)
        val headerAnnotations = tomlHeaderAnnotations(config::class).toMutableList()
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
                && if (ignoreNonSync) true else !isNonSync(it)
            }.sortedBy { orderById[it.name] }) {
                //has to be a public mutable property. private and protected and val another way to have serialization ignore
                if (it is KMutableProperty<*> && it.visibility == KVisibility.PUBLIC) {
                    //get the actual [thing] from the property
                    val propVal = it.get(config)
                    //things name
                    val name = it.name
                    //serialize the element. EntrySerializer elements will have a set serialization method
                    val el = if (propVal is EntrySerializer<*>) { //is EntrySerializer
                        try {
                            propVal.serializeEntry(null, errorBuilder, ignoreNonSync)
                        } catch (e: Exception) {
                            errorBuilder.add("Problem encountered with serialization of [$name]: ${e.localizedMessage}")
                            TomlNull
                        }
                        //fallback is to use by-type TOML serialization
                    } else if (propVal != null) {
                        try {
                            encodeToTomlElement(propVal, it.returnType) ?: TomlNull
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
                    val tomlAnnotations = tomlAnnotations(it)
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

    internal fun <T: Any> serializeConfig(config: T, errorBuilder: MutableList<String>, ignoreNonSync: Boolean = true): String{
        return Toml.encodeToString(serializeToToml(config,errorBuilder,ignoreNonSync))
    }

    private fun <T: Config, M> serializeUpdateToToml(config: T, manager: M, errorBuilder: MutableList<String>, ignoreNonSync: Boolean = false): TomlElement where M: UpdateManager, M:BasicValidationProvider{
        val toml = TomlTableBuilder()
        try {
            walk(config,config.getId().toTranslationKey(),false) { _,str,v,prop,_ ->
                if(manager.hasUpdate(str)){
                    if(v is EntrySerializer<*>){
                        toml.element(str, v.serializeEntry(null, errorBuilder, ignoreNonSync))
                    } else if (v != null) {
                        val basicValidation = manager.basicValidationStrategy(v,prop.returnType)
                        if (basicValidation != null){
                            val el = basicValidation.trySerialize(v, errorBuilder, ignoreNonSync)
                            if (el != null)
                                toml.element(str, el)
                        }
                    }
                }
            }
        } catch (e: Exception){
            errorBuilder.add("Critical error encountered while serializing config update!: ${e.localizedMessage}")
            return toml.build()
        }
        return toml.build()
    }

    internal fun <T: Config, M> serializeUpdate(config: T, manager: M, errorBuilder: MutableList<String>, ignoreNonSync: Boolean = false): String where M: UpdateManager, M:BasicValidationProvider{
        return Toml.encodeToString(serializeUpdateToToml(config,manager,errorBuilder,ignoreNonSync))
    }

    internal fun serializeEntry(entry: Entry<*,*>, errorBuilder: MutableList<String>, ignoreNonSync: Boolean = true): String{
        val toml = TomlTableBuilder()
        toml.element("entry", entry.serializeEntry(null,errorBuilder, ignoreNonSync))
        return Toml.encodeToString(toml.build())
    }

    internal fun <T: Any> deserializeFromToml(config: T, toml: TomlElement, errorBuilder: MutableList<String>, ignoreNonSync: Boolean = true): ValidationResult<T> {
        val inboundErrorSize = errorBuilder.size
        if (toml !is TomlTable) {
            errorBuilder.add("TomlElement passed not a TomlTable! Using default Config")
            return ValidationResult.error(config, "Improper TOML format passed to deserializeFromToml")
        }
        try {
            val fields = config::class.java.declaredFields.filter { !isTransient(it.modifiers) }
            val orderById = fields.withIndex().associate { it.value.name to it.index }
            for (it in config.javaClass.kotlin.memberProperties.filter {
                orderById.containsKey(it.name)
                && if (ignoreNonSync) true else !isNonSync(it)
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
                    if (propVal is EntryDeserializer<*>) { //is EntryDeserializer
                        val result = propVal.deserializeEntry(tomlElement, errorBuilder, name, ignoreNonSync)
                        if (result.isError()) {
                            errorBuilder.add(result.getError())
                        }
                    } else {
                        try {
                            it.setter.call(config, decodeFromTomlElement(tomlElement, it.returnType))
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

    internal fun <T: Any> deserializeConfig(config: T, string: String, errorBuilder: MutableList<String>, ignoreNonSync: Boolean = true): Pair<ValidationResult<T>,Int> {
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


    private fun <T: Config> deserializeUpdateFromToml(config: T, toml: TomlElement, errorBuilder: MutableList<String>, ignoreNonSync: Boolean = false): ValidationResult<T> {
        val inboundErrorSize = errorBuilder.size
        if (toml !is TomlTable) {
            errorBuilder.add("TomlElement passed not a TomlTable! Using default Config")
            return ValidationResult.error(config,"Improper TOML format passed to deserializeDirtyFromToml")
        }
        try {
            walk(config, config.getId().toTranslationKey(), ignoreNonSync) {_, str, v, prop, _ -> toml[str]?.let{
                if(v is EntryDeserializer<*>) {
                    v.deserializeEntry(it, errorBuilder, str, ignoreNonSync)
                } else if (v != null){
                    val basicValidation = UpdateManager.INSTANCE.basicValidationStrategy(v,prop.returnType)
                    if (basicValidation != null){
                        @Suppress("DEPRECATION")
                        val thing = basicValidation.deserializeEntry(it, errorBuilder, str, ignoreNonSync)
                        if (prop is KMutableProperty<*> && prop.visibility == KVisibility.PUBLIC){
                            prop.setter.call(config, thing.get())
                        }
                    }
                }
            }
            }
        } catch(e: Exception){
            errorBuilder.add("Critical error encountered while deserializing update")
        }
        return ValidationResult.predicated(config, errorBuilder.size <= inboundErrorSize, "Errors found while deserializing Config ${config.javaClass.canonicalName}!")
    }

    internal fun <T: Config> deserializeUpdate(config: T, string: String, errorBuilder: MutableList<String>, ignoreNonSync: Boolean = false): ValidationResult<T> {
        val toml = try {
            Toml.parseToTomlTable(string)
        } catch (e:Exception){
            return ValidationResult.error(config, "Config ${config.javaClass.canonicalName} is corrupted or improperly formatted for parsing")
        }
        return deserializeUpdateFromToml(config, toml, errorBuilder, ignoreNonSync)
    }

    internal fun deserializeEntry(entry: Entry<*,*>, string: String, scope: String, errorBuilder: MutableList<String>, ignoreNonSync: Boolean = false): ValidationResult<*> {
        val toml = try {
            Toml.parseToTomlTable(string)
        } catch (e:Exception){
            return ValidationResult.error(null, "Toml $string isn't properly formatted to be deserialized")
        }
        val element = toml["entry"] ?: return ValidationResult.error(null, "Toml $string doesn't contain needed 'entry' key")
        return entry.deserializeEntry(element, errorBuilder, scope, ignoreNonSync)
    }

    internal fun makeDir(folder: String, subfolder: String): Pair<File,Boolean>{
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

    private fun encodeToTomlElement(a: Any, clazz: KType): TomlElement?{
        return try {
            val strategy = Toml.serializersModule.serializer(clazz)
            Toml. encodeToTomlElement(strategy, a)
        } catch (e: Exception){
            null
        }
    }

    private fun decodeFromTomlElement(element: TomlElement, clazz: KType): Any?{
        return try {
            val strategy = Toml.serializersModule.serializer(clazz) as? KSerializer<*> ?: return null
            Toml.decodeFromTomlElement(strategy, element)
        } catch (e: Exception){
            null
        }
    }

    private fun isNonSync(property: KProperty<*>): Boolean{
        return isNonSync(property.annotations)
    }
    internal fun isNonSync(annotations: List<Annotation>): Boolean{
        return annotations.firstOrNull { (it is NonSync) }?.let { true } ?: false
    }
    internal fun tomlAnnotations(property: KAnnotatedElement): List<Annotation> {
        return property.annotations.map { mapJvmAnnotations(it) }.filter { it is TomlComment || it is TomlInline || it is TomlBlockArray || it is TomlMultilineString || it is TomlLiteralString || it is TomlInteger }
    }
    private fun mapJvmAnnotations(input: Annotation): Annotation{
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
    private fun <T: Any> tomlHeaderAnnotations(field: KClass<T>): List<TomlHeaderComment>{
        return field.annotations.mapNotNull { it as? TomlHeaderComment }
    }
    private fun getVersion(clazz: KClass<*>): Int {
        val version = clazz.findAnnotation<Version>()
        return version?.version ?: 0
    }

    internal fun printChangeHistory(history: List<String>, id: String, player: PlayerEntity? = null){
        FC.LOGGER.info("∨∨∨∨∨∨∨∨∨∨∨∨∨∨∨∨∨∨∨∨∨∨∨∨∨")
        FC.LOGGER.info("Completed updates for configs: [$id]")
        if (player != null)
            FC.LOGGER.info("Updates made by: ${player.name.string}")
        FC.LOGGER.info("-------------------------")
        for (str in history)
            FC.LOGGER.info("  $str")
        FC.LOGGER.info("∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧")
    }

    fun<W: Walkable> walk(config: W, prefix: String, ignoreNonSync: Boolean,  walkAction: WalkAction){
        for (property in config.javaClass.kotlin.memberProperties.filter {
            !isTransient(it.javaField?.modifiers ?: Modifier.TRANSIENT)
            && it is KMutableProperty<*>
            && (if (ignoreNonSync) true else !isNonSync(it) ) }
        ) {
            val newPrefix = prefix + "." + property.name
            val propVal = property.get(config)
            walkAction.act(prefix, newPrefix, propVal, property, property.annotations)
            if (propVal is Walkable){
                walk(propVal, newPrefix, ignoreNonSync, walkAction)
            }
        }
    }

    fun interface WalkAction{
        fun act(oldPrefix: String, newPrefix: String, element: Any?, elementProp: KProperty<*>, annotations: List<Annotation>)
    }
}
