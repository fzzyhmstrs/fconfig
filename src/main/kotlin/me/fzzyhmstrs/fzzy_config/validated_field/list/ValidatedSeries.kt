package me.fzzyhmstrs.fzzy_config.validated_field.list

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import me.fzzyhmstrs.fzzy_config.config.SyncedConfigHelperV1
import me.fzzyhmstrs.fzzy_config.config.ValidationResult
import me.fzzyhmstrs.fzzy_config.validated_field.ValidatedField
import net.minecraft.network.PacketByteBuf
import java.util.function.BiPredicate

class ValidatedSeries<T: Comparable<T>>(
    inputSeries: Array<T>,
    valueType: Class<T>,
    private val seriesValidator: BiPredicate<T, T>,
    private val invalidEntryMessage: String = "None",
    private val entryDeserializer: EntryDeserializer<T> = EntryDeserializer { json ->  SyncedConfigHelperV1.gson.fromJson(json,valueType)})
    :
    ValidatedField<Array<T>>(inputSeries)
{

    override fun readmeText(): String {
        return "Ordered array of size [${storedValue.size}] with entries that meet the following criteria: $invalidEntryMessage"
    }

    override fun deserializeHeldValue(json: JsonElement, fieldName: String): ValidationResult<Array<T>> {
        return try{
            if (!json.isJsonArray) {
                ValidationResult.error(storedValue,"Couldn't deserialize list [$json] from key [$fieldName] in config class [${this.javaClass.enclosingClass?.canonicalName}]")
            } else {
                var list: MutableList<T> = mutableListOf()
                val errorList: MutableList<JsonElement> = mutableListOf()
                val jsonArray = json.asJsonArray

                for (jsonEl in jsonArray){
                    try{
                        val el = entryDeserializer.deserialize(jsonEl)
                        list.add(el)
                    } catch (e: Exception){
                        errorList.add(jsonEl)
                    }
                }
                var sizeError = 0
                if (list.size < storedValue.size){
                    sizeError = list.size - storedValue.size
                    for (i in list.size until storedValue.size){
                        list.add(storedValue[i])
                    }
                } else if(list.size > storedValue.size) {
                    sizeError = list.size - storedValue.size
                    list = list.subList(0,storedValue.size).toMutableList()
                }
                if(errorList.isEmpty() && sizeError == 0){
                    ValidationResult.success(list.stream().toArray { storedValue.copyOf() })
                } else if(sizeError != 0){
                    ValidationResult.error(list.stream().toArray { storedValue.copyOf() },"Errors in list at key [$fieldName], the provided array was the wrong size: [$sizeError] difference from the needed size.")
                } else {
                    ValidationResult.error(list.stream().toArray { storedValue.copyOf() },"Errors in list at key [$fieldName], the following elements couldn't be deserialized and were skipped: $errorList")
                }
            }
        } catch(e: Exception){
            ValidationResult.error(storedValue,"Couldn't deserialize list [$json] from key [$fieldName] in config class [${this.javaClass.enclosingClass?.canonicalName}]")
        }
    }

    override fun serializeHeldValue(): JsonElement {
        return gson.toJsonTree(storedValue,storedValue.javaClass)
    }

    override fun validateAndCorrectInputs(input: Array<T>): ValidationResult<Array<T>> {
        val returnArray = input.copyOf()
        for (i in 0 until input.size - 1){
            if (!seriesValidator.test(input[i],input[i+1])){
                if(input[i] != storedValue[i]){
                    returnArray[i] = storedValue[i]
                } else {
                    returnArray[i+1] = storedValue[i+1]
                }
                val result = validateAndCorrectInputs(returnArray).get()
                return ValidationResult.error(result,"Errors found while validating ordered series, input: $input, repaired to: $result, potential reason: $invalidEntryMessage")
            }
        }
        return ValidationResult.success(input)
    }

    override fun toBuf(buf: PacketByteBuf) {
        buf.writeString(gson.toJson(serializeHeldValue()))
    }

    override fun fromBuf(buf: PacketByteBuf): Array<T> {
        return deserializeHeldValue(JsonParser.parseString(buf.readString()),"").get()
    }
}