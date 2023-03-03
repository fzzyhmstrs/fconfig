package me.fzzyhmstrs.fzzy_config.config_util.validated_field

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import me.fzzyhmstrs.fzzy_config.config_util.SyncedConfigHelperV1
import me.fzzyhmstrs.fzzy_config.config_util.ValidationResult
import net.minecraft.network.PacketByteBuf
import java.util.function.Predicate

open class ValidatedList<R>(
    defaultValue: List<R>,
    private val lType: Class<R>,
    private val listEntryValidator: Predicate<R> = Predicate {true},
    private val invalidEntryMessage: String = "None",
    private val entryDeserializer: EntryDeserializer<R> = EntryDeserializer {json ->  SyncedConfigHelperV1.gson.fromJson(json,lType)})
    : 
    ValidatedField<List<R>>(defaultValue) 
{

    override fun deserializeHeldValue(json: JsonElement, fieldName: String): ValidationResult<List<R>> {
        return try{
            if (!json.isJsonArray) {
                ValidationResult.error(storedValue,"Couldn't deserialize list [$json] from key [$fieldName] in config class [${this.javaClass.enclosingClass?.canonicalName}]")
            } else {
                val list: MutableList<R> = mutableListOf()
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
                if(errorList.isEmpty()){
                    ValidationResult.success(list)
                } else {
                    ValidationResult.error(list,"Errors in list at key [$fieldName], the following elements couldn't be deserialized and were skipped: $errorList")
                }
            }
        } catch(e: Exception){
            ValidationResult.error(storedValue,"Couldn't deserialize list [$json] from key [$fieldName] in config class [${this.javaClass.enclosingClass?.canonicalName}]")
        }
    }

    override fun serializeHeldValue(): JsonElement {
        return gson.toJsonTree(storedValue,storedValue.javaClass)
    }

    override fun validateAndCorrectInputs(input: List<R>): ValidationResult<List<R>> {
        val tempList: MutableList<R> = mutableListOf()
        val errorList:MutableList<String> = mutableListOf()
        input.forEach {
            if(listEntryValidator.test(it)){
                tempList.add(it)
            } else {
                errorList.add(it.toString())
            }
        }
        if (errorList.isNotEmpty()){
            return ValidationResult.error(tempList, "Config list has errors, entries need to follow these constraints: $invalidEntryMessage. The following entries couldn't be validated and were removed: $errorList")
        }
        return ValidationResult.success(input)
    }

    override fun readmeText(): String{
        return "List of values that meet the following criteria: $invalidEntryMessage"
    }

    override fun toBuf(buf: PacketByteBuf) {
        buf.writeString(gson.toJson(serializeHeldValue()))
    }

    override fun fromBuf(buf: PacketByteBuf): List<R> {
        return deserializeHeldValue(JsonParser.parseString(buf.readString()),"").get()
    }
}
