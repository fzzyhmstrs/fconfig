/*
package me.fzzyhmstrs.fzzy_config.validated_field.list

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import me.fzzyhmstrs.fzzy_config.api.ConfigHelper
import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import me.fzzyhmstrs.fzzy_config.validated_field.ValidatedField
import net.minecraft.network.PacketByteBuf
import java.util.function.Predicate

*/
/**
 * A validated list of objects
 *
 * Validated Lists deserialize their contents line-by-line, which allows fail-soft behavior of deserialization of entries. If only one entry has a syntax error, for example, it will simply be logged and skipped.
 *
 * Validation is performed by the provided entry validator.
 *
 * ValidatedList implements kotlin [List], enabling direct usage of the validated field in the same manner as a normal List<T>. For manipulation of the entire list contents, it is recommended to extract the stored list directly with [get]
 *
 * There are several pre-defined List type subclasses that may be preferable to use over this "raw" implementation.
 *
 * @param defaultValue List<R>. The default list settings
 * @param lType Class<R>. The java class of the stored objects
 * @param listEntryValidator Predicate<R>, optional. If not provided, validation will always pass (no validation). The supplied predicate should return true on validation success, false on fail.
 * @param invalidEntryMessage String, optional. Provide a message detailing the criteria the user needs to follow in the case they make a mistake.
 * @param entryDeserializer EntryDeserializer<R>, optional. If not provided, will attempt to use GSON to parse the values. Otherwise, provide a deserializer that parses the provided JsonElement.
 *//*

open class ValidatedList<R>(
    defaultValue: List<R>,
    private val lType: Class<R>,
    private val listEntryValidator: Predicate<R> = Predicate {true},
    private val invalidEntryMessage: String = "None",
    private val entryDeserializer: EntryDeserializer<R> = EntryDeserializer { json ->  ConfigHelper.gson.fromJson(json,lType)})
    :
    ValidatedField<List<R>>(defaultValue),
    List<R>
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
                    } catch (e: Exception) {
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

    override val size: Int
        get() = storedValue.size

    override fun contains(element: R): Boolean {
        return storedValue.contains(element)
    }

    override fun containsAll(elements: Collection<R>): Boolean {
        return storedValue.containsAll(elements)
    }

    override fun get(index: Int): R {
        return storedValue.get(index)
    }

    override fun indexOf(element: R): Int {
        return storedValue.indexOf(element)
    }

    override fun isEmpty(): Boolean {
        return storedValue.isEmpty()
    }

    override fun iterator(): Iterator<R> {
        return storedValue.iterator()
    }

    override fun lastIndexOf(element: R): Int {
        return storedValue.lastIndexOf(element)
    }

    override fun listIterator(): ListIterator<R> {
        return storedValue.listIterator()
    }

    override fun listIterator(index: Int): ListIterator<R> {
        return storedValue.listIterator(index)
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<R> {
        return storedValue.subList(fromIndex, toIndex)
    }
}*/