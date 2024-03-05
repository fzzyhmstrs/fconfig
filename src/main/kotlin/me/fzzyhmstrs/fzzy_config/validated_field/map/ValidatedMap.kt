/*
package me.fzzyhmstrs.fzzy_config.validated_field.map

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import me.fzzyhmstrs.fzzy_config.config.ConfigHelper
import me.fzzyhmstrs.fzzy_config.config.ValidationResult
import me.fzzyhmstrs.fzzy_config.validated_field.ValidatedField
import me.fzzyhmstrs.fzzy_config.validated_field.ValidatedField.EntryDeserializer
import me.fzzyhmstrs.fzzy_config.validated_field.map.ValidatedMap.KeyDeserializer
import net.minecraft.network.PacketByteBuf
import java.util.function.BiFunction
import java.util.function.BiPredicate

*/
/**
 * A validated [Map] collection.
 *
 * This class is very "raw" in the sense that it can't do very much automatically and requires that input from the user. As such, it's recommended to use one of the pre-defined subclasses except in special circumstances where those don't fit the use case.
 *
 * Validation is performed both on deserialization, where problems in deserialization of individual entries are trimmed from the resulting map and added to an error log message; and in validation, where the provided validator is tested.
 *
 * ValidatedMap implements kotlin [Map], enabling direct usage of the validated field in the same manner as a normal Map<K,V>. For manipulation of the entire map contents, it is recommended to extract the stored list directly with [get]
 *
 * @param keyType Class<R>. A java class of the key type.
 * @param valueType Class<T>. A java class of the value type.
 * @param mapEntryCorrector BiPredicate<R,T>, optional. If not provided, will always test true (no validation). Pass a BiPredicate that tests both the key and entry against your specific criteria. True passes validation, false fails.
 * @param invalidEntryMessage String, optional. Provide a message detailing the criteria the user needs to follow in the case they make a mistake.
 * @param keyDeserializer KeyDeserializer<R>, optional. If not provided, will attempt to use GSON to parse the keys. Otherwise, provide a deserializer that parses the passed key string.
 * @param entryDeserializer EntryDeserializer<T>, optional. If not provided, will attempt to use GSON to parse the values. Otherwise, provide a deserializer that parses the provided JsonElement.
 *//*

open class ValidatedMap<R,T>(
    defaultValue: Map<R,T>,
    private val keyType: Class<R>,
    private val valueType: Class<T>,
    private val mapEntryValidator: BiPredicate<R,T> = BiPredicate{_,_ -> true},
    private val mapEntryCorrector: BiFunction<R,T,T> = BiFunction{ _, it -> it},
    private val invalidEntryMessage: String = "None",
    private val keyDeserializer: KeyDeserializer<R> =
        KeyDeserializer {str -> ConfigHelper.gson.fromJson(str, keyType)},
    private val entryDeserializer: EntryDeserializer<T> =
        EntryDeserializer { json -> ConfigHelper.gson.fromJson(json, valueType) })
    :
    ValidatedField<Map<R, T>>(defaultValue),
    Map<R,T>
{

    override fun deserializeHeldValue(json: JsonElement, fieldName: String): ValidationResult<Map<R, T>> {
        return try{
            if (!json.isJsonObject){
                ValidationResult.error(storedValue,"Couldn't deserialize map [$json] from key [$fieldName] in config class [${this.javaClass.enclosingClass?.canonicalName}]")
            } else {
                val map: MutableMap<R,T> = mutableMapOf()
                val errorList: MutableList<String> = mutableListOf()
                val jsonObject = json.asJsonObject
                for (entry in jsonObject.entrySet()) {
                    try {
                        val keyTry = keyDeserializer.deserialize(entry.key)
                        try {
                            val entryTry = entryDeserializer.deserialize(entry.value)
                            map[keyTry] = entryTry
                        } catch (e: Exception){
                            println("exception in value deserialization ${entry.value}")
                            errorList.add(entry.toString())
                        }
                    } catch (e: Exception){
                        println("exception in key deserialization ${entry.key}")
                        e.printStackTrace()
                        errorList.add(entry.toString())
                    }
                }
                if (errorList.isEmpty()) {
                    ValidationResult.success(map)
                } else {
                    ValidationResult.error(map,"Errors in map at key [$fieldName], the following entries couldn't be deserialized and were skipped: $errorList")
                }
            }


            //ValidationResult.success(gson.fromJson(json,lType.type))
        } catch(e: Exception){
            ValidationResult.error(storedValue,"Couldn't deserialize map [$json] from key [$fieldName] in config class [${this.javaClass.enclosingClass?.canonicalName}]")
        }
    }

    override fun serializeHeldValue(): JsonElement {
        return gson.toJsonTree(storedValue,storedValue.javaClass)
    }

    override fun validateAndCorrectInputs(input: Map<R,T>): ValidationResult<Map<R, T>> {
        val tempList: MutableMap<R,T> = mutableMapOf()
        val errorList1: MutableList<String> = mutableListOf()
        val errorList2: MutableList<String> = mutableListOf()
        for (it in input){
            if (!mapEntryValidator.test(it.key,it.value)){
                errorList1.add(it.toString())
                continue
            }
            val temp = mapEntryCorrector.apply(it.key,it.value)
            if (temp != it.value){
                errorList2.add(it.toString())
            }
            tempList[it.key] = temp
        }
        if (errorList1.isNotEmpty() || errorList2.isNotEmpty()){
            return ValidationResult.error(tempList, "Config map has errors, entries need to follow these constraints: $invalidEntryMessage. Invalid entries that were skipped: $errorList1. Corrected entries: $errorList2.")
        }
        return ValidationResult.success(input)
    }

    override fun readmeText(): String{
        return "Map of key-value pairs that meet the following criteria: $invalidEntryMessage"
    }

    override fun toBuf(buf: PacketByteBuf) {
        buf.writeString(gson.toJson(serializeHeldValue()))
    }

    override fun fromBuf(buf: PacketByteBuf): Map<R, T> {
        return deserializeHeldValue(JsonParser.parseString(buf.readString()),"").get()
    }

    */
/**
     * functional interface for parsing keys in ValidatedMaps.
     *
     * A set of default implementations is provided for several basic types.
     *
     * SAM: [deserialize]. takes a string key, returns a deserialized instance of T
     *//*

    fun interface KeyDeserializer<T>{
        fun deserialize(string: String): T
        companion object{
            val STRING: KeyDeserializer<String> = KeyDeserializer {str -> str}
            val BOOLEAN: KeyDeserializer<Boolean> = KeyDeserializer { str -> "true".equals(str,true)}
            val INT: KeyDeserializer<Int> = KeyDeserializer {str -> Integer.parseInt(str)}
            val FLOAT: KeyDeserializer<Float> = KeyDeserializer {str -> java.lang.Float.parseFloat(str)}
            val DOUBLE: KeyDeserializer<Double> = KeyDeserializer {str -> java.lang.Double.parseDouble(str)}
            val LONG: KeyDeserializer<Long> = KeyDeserializer {str -> java.lang.Long.parseLong(str)}
            val SHORT: KeyDeserializer<Short> = KeyDeserializer {str -> java.lang.Short.parseShort(str)}
            val BYTE: KeyDeserializer<Byte> = KeyDeserializer {str -> java.lang.Byte.parseByte(str)}
        }
    }

    override val entries: Set<Map.Entry<R, T>>
        get() = storedValue.entries
    override val keys: Set<R>
        get() = storedValue.keys
    override val size: Int
        get() = storedValue.size
    override val values: Collection<T>
        get() = storedValue.values

    override fun containsKey(key: R): Boolean {
        return storedValue.containsKey(key)
    }

    override fun containsValue(value: T): Boolean {
        return storedValue.containsValue(value)
    }

    override fun get(key: R): T? {
        return storedValue[key]
    }

    override fun isEmpty(): Boolean {
        return storedValue.isEmpty()
    }

}*/