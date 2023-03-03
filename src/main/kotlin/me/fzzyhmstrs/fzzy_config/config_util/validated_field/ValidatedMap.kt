package me.fzzyhmstrs.fzzy_config.config_util.validated_field

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import me.fzzyhmstrs.fzzy_config.config_util.SyncedConfigHelperV1
import me.fzzyhmstrs.fzzy_config.config_util.ValidationResult
import me.fzzyhmstrs.fzzy_config.config_util.validated_field.ValidatedField.EntryDeserializer
import me.fzzyhmstrs.fzzy_config.config_util.validated_field.ValidatedMap.KeyDeserializer
import net.minecraft.network.PacketByteBuf
import java.util.function.BiPredicate

open class ValidatedMap<R,T>(
    defaultValue: Map<R,T>,
    private val keyType: Class<R>,
    private val valueType: Class<T>,
    private val mapEntryValidator: BiPredicate<R,T> = BiPredicate{_,_ -> true},
    private val invalidEntryMessage: String = "None",
    private val keyDeserializer: KeyDeserializer<R> =
        KeyDeserializer {str -> SyncedConfigHelperV1.gson.fromJson(str, keyType)},
    private val entryDeserializer: EntryDeserializer<T> =
        EntryDeserializer { json -> SyncedConfigHelperV1.gson.fromJson(json, valueType) })
    :
    ValidatedField<Map<R,T>>(defaultValue)
{

    override fun deserializeHeldValue(json: JsonElement, fieldName: String): ValidationResult<Map<R,T>> {
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

    override fun validateAndCorrectInputs(input: Map<R,T>): ValidationResult<Map<R,T>> {
        val tempList: MutableMap<R,T> = mutableMapOf()
        val errorList:MutableList<String> = mutableListOf()
        input.forEach {
            if(mapEntryValidator.test(it.key,it.value)){
                tempList[it.key] = it.value
            } else {
                errorList.add(it.toString())
            }
        }
        if (errorList.isNotEmpty()){
            return ValidationResult.error(tempList, "Config map has errors, entries need to follow these constraints: $invalidEntryMessage. The following entries couldn't be validated and were removed: $errorList")
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

}