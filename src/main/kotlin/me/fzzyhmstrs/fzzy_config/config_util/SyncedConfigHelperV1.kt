package me.fzzyhmstrs.fzzy_config.config_util

import com.google.gson.*
import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.validated_field.ValidatedField
import me.fzzyhmstrs.fzzy_config.interfaces.ConfigSerializable
import me.fzzyhmstrs.fzzy_config.interfaces.OldClass
import net.fabricmc.loader.api.FabricLoader
import java.io.File
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaType

/**
 * Helper object that provides methods for reading, creating, and de/serializing configuration JSON's.
 *
 * In the vast majority of use-cases, the user can keep the scope of utilization to [readOrCreateAndValidate] and [readOrCreateUpdatedAndValidate], with other methods exposed for convenience in special circumstances.
 */
object SyncedConfigHelperV1 {

    /**
     * A GSON instance that is used by this helper and can be utilized as needed outside the helper.
     */
    val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    /**
     * Generator method for brand new config classes (rev 0, if you will). This method performs a sequence of tasks:
     * 1. makes a directory/sub-directory as needed for storing the config file
     * 2. makes a [File] instance
     * 3. Checks if an existing config JSON exists. This is the "read" part of the method.
     *
     * Then if a config JSON exists:
     * 4. Deserializes and validates the JSON using [deserializeConfig]. Provide any error logging if relevant. This is the "AndValidate"
     * 5. If there were validation errors found, correct the .json file with the post-validation config settings
     * 6. Returns the deserialized and corrected config class.
     *
     * or if no file is found:
     * 4. Attempt to serialize and write the config settings to a .json file. This is the "OrCreate"
     * 5. Returns the default config settings class.
     *
     * In the case of an exception, this method will catch it, print the stack trace, and resort to passing back the default config settings provided to it.
     *
     * @param file String. The file name for the config json. Needs the suffix, so "my_file_name.json", for example.
     * @param child String, optional. A subfolder name if desired. By default is left out, meaning the config will appear in the base subfolder
     * @param base String, optional. A base config folder name. If left out, the config will be written into the "fzzy_config" subfolder. To write to the main config folder, pass "" to this.
     * @param configClass () -> T. A provider of instances of the config class itself. In Kotlin this can typically be written like `{ MyConfigClass() }`
     *
     * @return An instance of the configClass passed to it, either as-is or updated and validated per the above process.
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
                val readConfig = deserializeConfig(configClass(), JsonParser.parseString(str))
                if (readConfig.isError()) {
                    FC.LOGGER.warn("Errors found in $file per above log entries, attempting to correct invalid inputs automatically.")
                    val correctedConfig = serializeConfig(readConfig.get())
                    f.writeText(correctedConfig)
                }
                return readConfig.get()
            } else if (!f.createNewFile()) {
                FC.LOGGER.error("Failed to create default config file ($file), using default config.")
            } else {
                val initialClass = configClass()
                val str = serializeConfig(initialClass)
                f.writeText(str)
            }
            return configClass()
        } catch (e: Exception) {
            FC.LOGGER.error("Failed to read config file $file! Using default values: " + e.message)
            e.printStackTrace()
            return configClass()
        }
    }

    fun <T : Any> save(file: String, child: String = "", base: String = FC.MOD_ID, configClass: T) {
        val (dir,dirCreated) = makeDir(child, base)
        if (!dirCreated) {
            return
        }
        val f = File(dir, file)
        try {
            if (f.exists()) {
                val str = serializeConfig(configClass)
                f.writeText(str)
            } else if (!f.createNewFile()) {
                FC.LOGGER.error("Failed to open config file ($file), config not saved.")
            } else {
                val str = serializeConfig(configClass)
                f.writeText(str)
            }
        } catch (e: Exception) {
            FC.LOGGER.error("Failed to save config file $file!")
            e.printStackTrace()
        }
    }

    /**
     * Generator method for updating an existing config to a new version (Rev. >0). This method follows the same basic sequence of tasks as [readOrCreateAndValidate] with the following additions
     * 1. First determines if an old version of the config file exists.
     * 2. If it exists, deserialize and validate the old config, then call [OldClass.generateNewClass](generateNewClass) on the [OldClass] instance generated this way. Writes this new class to a .json file. Returns the new class.
     * 3. If no old config exists, call [readOrCreateAndValidate] on the new config class.
     *
     * @param file String. The file name for the new config json. Needs the suffix, so "my_file_name_v0.json", for example.
     * @param previous String. The file name for the old config json. Needs the suffix, so "my_file_name_v1.json", for example.
     * @param child String, optional. A subfolder name if desired. By default is left out, meaning the config will appear in the base subfolder
     * @param base String, optional. A base config folder name. If left out, the config will be written into the "fzzy_config" subfolder. To write to the main config folder, pass "" to this.
     * @param configClass () -> T. A provider of instances of the config class itself. In Kotlin this can typically be written like `{ MyConfigClass() }`
     * @param configClass () -> [OldClass]<T>. A provider of instances of the old config class for use in generating new classes.
     *
     * @return An instance of the configClass passed to it, either as-is or updated and validated per the above process.
     */
    fun <T: Any> readOrCreateUpdatedAndValidate(file: String, previous: String, child: String = "", base: String = FC.MOD_ID, configClass: () -> T, previousClass: () -> OldClass<T>): T{
        //println("Read or create and validate updated: $file")
        val (dir,dirCreated) = makeDir(child, base)
        if (!dirCreated) {
            //println("totally failed!")
            return configClass()
        }
        val p = File(dir, previous)
        try {
            if (p.exists()) {
                val pStr = p.readLines().joinToString("\n")
                //println(">>> Found Old config:")
                //println(pStr)
                val previousConfig = deserializeConfig(previousClass(), JsonParser.parseString(pStr))
                if (previousConfig.isError()){
                    FC.LOGGER.error("Old config $previous had errors, attempted to correct before updating.")
                }
                val newClass = previousConfig.get().generateNewClass()
                val f = File(dir,file)
                if (f.exists()){
                    p.delete() //attempts to delete the now useless old config version file
                    val str = f.readLines().joinToString("")
                    //println(">>> Found config:")
                    //println(str)
                    val readConfig = deserializeConfig(configClass(), JsonParser.parseString(str))
                    if (readConfig.isError()){
                        FC.LOGGER.warn("Errors found in $file per above logs, attempting to correct invalid inputs automatically.")
                        val correctedConfig = serializeConfig(readConfig.get())
                        //println(">>> Corrected config:")
                        //println(correctedConfig)
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
                //println("couldn't find previous class!")
                return readOrCreateAndValidate(file,child, base, configClass)
            }
        } catch (e: Exception) {
            println("Failed to read config file $file! Using default values: " + e.message)
            e.printStackTrace()
            return configClass()
        }
    }

    /**
     * custom serializer that utilizes GSON on un-validated properties, and custom serialization on validated ones.
     *
     * Only serializes mutable properties. For a config to be stored successfully this must be kept in mind.
     *
     * [ValidatedField] *cannot* typically be serialized properly by GSON, as the validation information is meant to be hidden from the .json file itself, staying in code as a secure key of sorts. Without that context, GSON doesn't have enough context to properly serialize or deserialize a ValidatedField, and it is *STRONGLY* recommended not to expose the valdiation to the config file, as this defeats the validation by allowing for user editing of validation parameters.
     */
    fun serializeConfig(config: Any): String{
        val json = JsonObject()
        val fields = config::class.java.declaredFields
        val orderById = fields.withIndex().associate { it.value.name to it.index }
        for (it in config.javaClass.kotlin.memberProperties.sortedBy { orderById[it.name] }) {
            if (it is KMutableProperty<*> && it.visibility == KVisibility.PUBLIC){
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

    /**
     * custom deserializer that utilizes GSON on un-validated properties, and custom deserialization on validated ones.
     *
     * Only deserializes mutable properties. For a config to be read from file successfully this must be kept in mind.
     *
     * [ValidatedField] *cannot* typically be serialized properly by GSON, as the validation information is meant to be hidden from the .json file itself, staying in code as a secure key of sorts. Without that context, GSON doesn't have enough context to properly serialize or deserialize a ValidatedField, and it is *STRONGLY* recommended not to expose the valdiation to the config file, as this defeats the validation by allowing for user editing of validation parameters.
     */
    fun <T: Any> deserializeConfig(config: T, json: JsonElement): ValidationResult<T> {
        if (!json.isJsonObject) return ValidationResult.error(config,"Config ${config.javaClass.canonicalName} is corrupted or improperly formatted for parsing")
        val jsonObject = json.asJsonObject
        var error = false
        val fields = config::class.java.declaredFields
        val orderById = fields.withIndex().associate { it.value.name to it.index }
        for (it in config.javaClass.kotlin.memberProperties.sortedBy { orderById[it.name] }){
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
     * Used to create a directory in the config parent directory inside the .minecraft folder. If the directory can't be created, the right member of the returning Pair will be false.
     */
    fun makeDir(child: String, base: String): Pair<File,Boolean>{
        val dir = if (child != ""){
            File(File(FabricLoader.getInstance().configDir.toFile(), base), child)
        } else {
            if (base != "") {
                File(FabricLoader.getInstance().configDir.toFile(), base)
            } else {
                FabricLoader.getInstance().configDir.toFile()
            }
        }
        if (!dir.exists() && !dir.mkdirs()) {
            println("Could not create directory, using default configs.")
            return Pair(dir,false)
        }
        return Pair(dir,true)
    }

}