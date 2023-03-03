package me.fzzyhmstrs.fzzy_config.config_util

import com.google.gson.*
import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.config_util.validated_field.ValidatedField
import net.fabricmc.loader.api.FabricLoader
import java.io.File
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaType

object SyncedConfigHelperV1 {

    val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    ////////////////////////////////////////
    // Updated config creator methods  /////
    ////////////////////////////////////////

    /**
     * Improved basic config serializer/deserializer method that is no longer inline and created validated configs using [ValidatedField]
     *
     * incorrect inputs will automatically be corrected where possible, or reverted to default if not, and the validated config re-written to it's file
     */
    fun <T : Any> readOrCreateAndValidate(file: String, child: String = "", base: String = FC.MOD_ID, configClass: () -> T): T {
        val (dir,dirCreated) = makeDir(child, base)
        if (!dirCreated) {
            return configClass()
        }
        val f = File(dir, file)
        try {
            if (f.exists()) {
                val str = f.readLines().joinToString("\n")
                println(">>> Found config:")
                println(str)
                val readConfig = deserializeConfig(configClass(), JsonParser.parseString(str))
                if (readConfig.isError()) {
                    FC.LOGGER.warn("Errors found in $file per above log entries, attempting to correct invalid inputs automatically.")
                    val correctedConfig = serializeConfig(readConfig.get())
                    println(">>> Corrected config:")
                    println(correctedConfig)
                    f.writeText(correctedConfig)
                }
                return readConfig.get()
            } else if (!f.createNewFile()) {
                println("Failed to create default config file ($file), using default config.")
            } else {
                val initialClass = configClass()
                val str = serializeConfig(initialClass)
                println(">>>> Initial Config: ")
                println(str)
                f.writeText(str)
            }
            return configClass()
        } catch (e: Exception) {
            println("Failed to read config file $file! Using default values: " + e.message)
            e.printStackTrace()
            return configClass()
        }
    }

    /**
     * Improved advanced config serializer/deserializer method that is no longer inline and created validated configs using [ValidatedField], allowing for 1 layer of version control
     *
     * incorrect inputs from the new and old config will be automatically corrected where possible, or reverted to default if not, and the validated and updated config written to it's file
     */
    fun <T: Any> readOrCreateUpdatedAndValidate(file: String, previous: String, child: String = "", base: String = FC.MOD_ID, configClass: () -> T, previousClass: () -> OldClass<T>): T{
        val (dir,dirCreated) = makeDir(child, base)
        if (!dirCreated) {
            return configClass()
        }
        val p = File(dir, previous)
        try {
            if (p.exists()) {
                val pStr = p.readLines().joinToString("")
                val previousConfig = deserializeConfig(previousClass(), JsonParser.parseString(pStr))
                if (previousConfig.isError()){
                    FC.LOGGER.error("Old config $previous had errors, attempted to correct before updating.")
                }
                val newClass = previousConfig.get().generateNewClass()
                val f = File(dir,file)
                if (f.exists()){
                    p.delete() //attempts to delete the now useless old config version file
                    val str = f.readLines().joinToString("")
                    println(">>> Found config:")
                    println(str)
                    val readConfig = deserializeConfig(configClass(), JsonParser.parseString(str))
                    if (readConfig.isError()){
                        FC.LOGGER.warn("Errors found in $file per above logs, attempting to correct invalid inputs automatically.")
                        val correctedConfig = serializeConfig(readConfig.get())
                        println(">>> Corrected config:")
                        println(correctedConfig)
                        f.writeText(correctedConfig)
                    }
                    return readConfig.get()
                } else if (!f.createNewFile()){
                    //don't delete old file if the new one can't be generated to take its place
                    println("Failed to create new config file ($file), using old config with new defaults.")
                } else {
                    p.delete() //attempts to delete the now useless old config version file
                    f.writeText(serializeConfig(newClass))
                }
                return newClass
            } else {
                return readOrCreateAndValidate(file,child, base, configClass)
            }
        } catch (e: Exception) {
            println("Failed to read config file $file! Using default values: " + e.message)
            e.printStackTrace()
            return configClass()
        }
    }

    //custom serializer that utilizes GSON on un-validated properties, and custom serialization on validated ones. Only serializes mutable properties
    fun serializeConfig(config: Any): String{
        val json = JsonObject()
        val fields = config::class.java.declaredFields
        val orderById = fields.withIndex().associate { it.value.name to it.index }
        for (it in config.javaClass.kotlin.declaredMemberProperties.sortedBy { orderById[it.name] }) {
            if (it is KMutableProperty<*>){
                val propVal = it.get(config)
                val name = it.name
                val el = if (propVal is ConfigSerializable){
                    propVal.serialize()
                } else {
                    gson.toJsonTree(propVal,it.returnType.javaType)
                }
                json.add(name,el)
            }
        }
        return gson.toJson(json)
    }


    fun <T: Any> deserializeConfig(config: T, json: JsonElement): ValidationResult<T> {
        if (!json.isJsonObject) return ValidationResult.error(config,"Config ${config.javaClass.canonicalName} is corrupted or improperly formatted for parsing")
        val jsonObject = json.asJsonObject
        var error = false
        val fields = config::class.java.declaredFields
        val orderById = fields.withIndex().associate { it.value.name to it.index }
        for (it in config.javaClass.kotlin.declaredMemberProperties.sortedBy { orderById[it.name] }){
            if (it is KMutableProperty<*>){
                val propVal = it.get(config)
                val name = it.name
                val jsonElement = if(jsonObject.has(name)) {
                    jsonObject.get(name)
                } else {
                    error = true
                    continue
                }
                if (propVal is ConfigSerializable){
                    val result = propVal.deserialize(jsonElement, name)
                    if(result.isError()){
                        error = true
                    }
                } else {
                    it.setter.call(config, gson.fromJson(jsonElement,it.returnType.javaType))
                }
            }
        }
        return if(!error){
            ValidationResult.success(config)
        } else {
            ValidationResult.error(config,"Errors found!")
        }
    }

    /**
     * method can be used to create a directory in the config parent directory. If the directory can't be created, the right member of the returning Pair will be false.
     */
    fun makeDir(child: String, base: String): Pair<File,Boolean>{
        val dir = if (child != ""){
            File(File(FabricLoader.getInstance().configDir.toFile(), base), child)
        } else {
            File(FabricLoader.getInstance().configDir.toFile(), base)
        }
        if (!dir.exists() && !dir.mkdirs()) {
            println("Could not create directory, using default configs.")
            return Pair(dir,false)
        }
        return Pair(dir,true)
    }

}