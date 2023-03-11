package me.fzzyhmstrs.fzzy_config.validated_field

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.config_util.*
import me.fzzyhmstrs.fzzy_config.interfaces.ConfigSerializable
import me.fzzyhmstrs.fzzy_config.interfaces.ReadMeTextProvider
import me.fzzyhmstrs.fzzy_config.interfaces.ServerClientSynced
import net.minecraft.network.PacketByteBuf
import kotlin.reflect.full.hasAnnotation

/**
 * Validated Field Collection - serialization is indistinguishable from their wrapped values, but deserialized into a validated wrapper
 *
 * Validated Fields CANNOT be serialized and deserialized by GSON properly. The JSON Element provided does not provide enough context, because the validation is hidden within code only, not serialized. These fields are not building new classes from scratch, they are updating and validating a pre-existing default class framework.
 *
 * Helper methods are provided to more easily sync configs directly via [PacketByteBuf]s, rather than serializing and then deserializing the entire JSON
 *
 * @param storedValue T. The wrapped value that this field validates, serializes, and syncs between server and client.
 *
 * @see ConfigSerializable
 * @see ServerClientSynced
 * @see ReadMeTextProvider
 */

abstract class ValidatedField<T>(protected var storedValue: T):
    ConfigSerializable,
    ServerClientSynced,
    ReadMeTextProvider {

    /**
     * An internal GSON instance with pretty printing that can be used by ValidatedFields
     */
    protected val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private var locked = false

    /**
     * The primary deserialization method for a validated field used by the fzzy-config serializer. Checks for a client-side lock (not currently implemented); if the property isn't lockable, returns an error (therefore resetting the held value to the default one).
     *
     * @param json The json element to be deserialized. Typically passed in from the auto-deserializer in [SyncedConfigHelperV1]
     *
     * @param fieldName The declared name of the property for use in error reporting. With a property declared like "var propName1: Clazz()", fieldName will be "propName1"
     *
     * @return A [ValidationResult] with a boolean value that is functionally redundant and a stored error message, if the deserialization had an error.
     */

    /////// fzzy core deserialization, NOT gson compatible ////////
    override fun deserialize(json: JsonElement, fieldName: String): ValidationResult<Boolean> {
        if(json.isJsonObject){
            val jsonObject = json.asJsonObject
            if (jsonObject.has("lock")){
                if(!this.javaClass.kotlin.hasAnnotation<Lockable>()){
                    return ValidationResult.error(true,"Illegal lock found, key $fieldName is not lockable.")
                }
                locked = true
                return deserializeAfterLockCheck(jsonObject.get("lock"),fieldName)
            }
        }
        return deserializeAfterLockCheck(json,fieldName)

    }

    /**
     * Internal helper method for [deserialize] that performs the actual deserialization after lock-checking, along with the input validation and error-generation and reporting
     *
     * Uses the abstract method [deserializeHeldValue] to perform the specific deserialization for a Field implementation.
     *
     * Params and Return values are the same as described in deserialize.
     */
    private fun deserializeAfterLockCheck(json: JsonElement, fieldName: String): ValidationResult<Boolean>{
        val tVal = deserializeHeldValue(json, fieldName)
        if (tVal.isError()){
            FC.LOGGER.error("Error deserializing manually entered config entry [$fieldName], using default value [${tVal.get()}]")
            FC.LOGGER.error("  >>> Possible reasons: ${tVal.getError()}")
            return ValidationResult.error(true, tVal.getError())
        }
        val tVal2 = validateAndCorrectInputs(tVal.get())
        storedValue = tVal2.get()
        if (tVal2.isError()){
            FC.LOGGER.error("Manually entered config entry [$fieldName] had errors, corrected to [${tVal2.get()}]")
            FC.LOGGER.error("  >>> Possible reasons: ${tVal2.getError()}")
            return ValidationResult.error(true, tVal2.getError())
        }

        return ValidationResult.success(false)
    }

    /**
     * The primary serialization method used by the fzzy-config serializer. If the field is locked, it wraps the stored value in a "locked" JSON Object.
     *
     * Uses the abstract method [serializeHeldValue] to perform the specific serialization tasks.
     *
     * @return A JSON Element with the serialized value contained within.
     */
    override fun serialize(): JsonElement {
        return if (locked){
            val json = JsonObject()
            json.add("lock",serializeHeldValue())
            json
        } else {
            serializeHeldValue()
        }
    }

    override fun writeToClient(buf: PacketByteBuf) {
        toBuf(buf)
    }
    override fun readFromServer(buf: PacketByteBuf) {
        val temp = fromBuf(buf)
        if (!locked) {
            storedValue = temp
        }
    }

    /**
     * Deserializes the wrapped value for updating [storedValue]. The goal of this method is to deserialize only the wrapped value. If an integer is wrapped, deserilaization should look exactly the same as a plain integer (except for locking).
     *
     * @param json JsonElement. The json element to be deserialized. Passed in from [deserialize] after lock-checking
     *
     * @param fieldName String. The declared name of the property for use in error reporting. With a property declared like "var propName1: Clazz()", fieldName will be "propName1"
     *
     * @return A [ValidationResult] that wraps an instance of the value to be stored, as well as a stored error message if the deserialization had an error.
     */
    protected abstract fun deserializeHeldValue(json: JsonElement, fieldName: String): ValidationResult<T>

    /**
     * Serializes the wrapped value of this validated field, and ONLY the wrapped value. Validation is maintained internally. If this field stores an Int, serialization is equivalent to a JSON Primitive storing an integer value.
     *
     * @return A JSON Element with the serialized wrapped value contained within.
     */
    protected abstract fun serializeHeldValue(): JsonElement

    /**
     * Perform input validation and correction in this method. A simple example can be seen in [ValidatedNumber], where this method bounds the input number to within the max and min values provided.
     *
     * @param input T. An instance of type T to be validated and corrected as needed, where T is the type of value wrapped in this Field.
     *
     * @return a [ValidationResult] that wraps the validated and/or corrected result of type T, along with an error message if needed.
     *
     * @see ValidatedNumber
     */
    protected abstract fun validateAndCorrectInputs(input: T): ValidationResult<T>

    /**
     * Serialize the wrapped value into a PacketByteBuf for syncing with the client.
     *
     * @param buf PacketByteBuf. The [PacketByteBuf] to serialize the wrapped value into
     */
    protected abstract fun toBuf(buf: PacketByteBuf)

    /**
     * Deserialize the value wrapped in [toBuf] for passing into [storedValue] via [readFromServer]
     *
     * @param buf PacketByteBuf. The [PacketByteBuf] to deserialize the wrapped value from
     */
    protected abstract fun fromBuf(buf: PacketByteBuf): T

    /**
     * Get the wrapped value with this method
     *
     * @return The wrapped value inside this Field
     */
    open fun get(): T{
        return storedValue
    }

    /**
     * A setter method for the [storedValue] that first validates the value being set and then stores the post-validation result.
     *
     * @param input T. the pre-validation input of type T that will be validated and then stored, where T is the type of the wrapped value in this field.
     */
    open fun validateAndSet(input: T){
        val tVal1 = validateAndCorrectInputs(input)
        if (tVal1.isError()){
            FC.LOGGER.error("Manually entered config entry had errors, corrected to [${tVal1.get()}]")
            FC.LOGGER.error("  >>> Possible reasons: ${tVal1.getError()}")
        }
        storedValue = tVal1.get()
    }

    /**
     * An EntryDeserializer can be used to deserialize an individual field entry for intermediate validation. An example of this can be seen in [ValidatedList](me.fzzyhmstrs.fzzy_config.validated_field.list.ValidatedList)
     *
     * SAM: [deserialize] takes a JsonElement, returns a deserialized instance of T
     *
     * @see me.fzzyhmstrs.fzzy_config.validated_field.list.ValidatedList
     */
    fun interface EntryDeserializer<T>{
        fun deserialize(json: JsonElement): T
    }

}