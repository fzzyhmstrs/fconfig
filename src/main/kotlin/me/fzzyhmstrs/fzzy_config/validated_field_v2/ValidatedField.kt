package me.fzzyhmstrs.fzzy_config.validated_field_v2

import me.fzzyhmstrs.fzzy_config.api.StringTranslatable
import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import me.fzzyhmstrs.fzzy_config.config.*
import me.fzzyhmstrs.fzzy_config.impl.FzzySerializable
import me.fzzyhmstrs.fzzy_config.updates.Updatable
import me.fzzyhmstrs.fzzy_config.updates.UpdateManager
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.Update
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.widget.Widget
import net.minecraft.network.PacketByteBuf
import net.minecraft.text.Text
import net.peanuuutz.tomlkt.TomlElement

/**
 * Validated Field Collection - serialization is indistinguishable from their wrapped values, but deserialized into a validated wrapper
 *
 * Validated Fields CANNOT be serialized and deserialized by GSON properly. The JSON Element provided does not provide enough context, because the validation is hidden within code only, not serialized. These fields are not building new classes from scratch, they are updating and validating a pre-existing default class framework.
 *
 * Helper methods are provided to more easily sync configs directly via [PacketByteBuf]s, rather than serializing and then deserializing the entire JSON
 *
 * @param storedValue T. The wrapped value that this field validates, serializes, and syncs between server and client.
 *
 */

abstract class ValidatedField<T: Any>(protected var storedValue: T): FzzySerializable, Updatable, StringTranslatable {

    private var pushedValue: T? = null
    private var updateKey = ""

    override fun getUpdateKey(): String {
        return updateKey
    }
    override fun setUpdateKey(key: String) {
        updateKey = key
    }
    override fun pushState(){
        pushedValue = copyStoredValue()
    }
    override fun peekState(): Boolean {
        return pushedValue != storedValue
    }
    override fun popState(): Boolean{
        if (pushedValue == null) return false
        val updated = pushedValue != storedValue
        pushedValue = null
        return updated
    }

    abstract fun copyStoredValue(): T

    @Deprecated("use deserializeHeldValue to avoid accidentally overwriting validation and error reporting")
    override fun deserialize(
        toml: TomlElement,
        errorBuilder: MutableList<String>,
        fieldName: String,
        ignoreNonSync: Boolean
    ): ValidationResult<Boolean> {
        val tVal = deserializeHeldValue(toml, fieldName)
        if (tVal.isError()){
            return ValidationResult.error(false,"Error deserializing config entry [$fieldName], using default value [${tVal.get()}]  >>> Possible reasons: ${tVal.getError()}")
        }
        val tVal2 = validateAndCorrectInputs(tVal.get())
        storedValue = tVal2.get()
        if (tVal2.isError()){
            return ValidationResult.error(false,"Config entry [$fieldName] had validation errors, corrected to [${tVal2.get()}]  >>> Possible reasons: ${tVal2.getError()}")
        }
        return ValidationResult.success(true)
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
    protected abstract fun deserializeHeldValue(toml: TomlElement, fieldName: String): ValidationResult<T>

    /**
     * Serializes the wrapped value of this validated field, and ONLY the wrapped value. Validation is maintained internally. If this field stores an Int, serialization is equivalent to a JSON Primitive storing an integer value.
     *
     * @return A JSON Element with the serialized wrapped value contained within.
     */

    @Deprecated("use serializeHeldValue for consistency", ReplaceWith("serializeHeldValue()"))
    override fun serialize(errorBuilder: MutableList<String>, ignoreNonSync: Boolean): TomlElement {
        return serializeHeldValue()
    }

    protected abstract fun serializeHeldValue(): TomlElement

    /**
     * Perform input validation and correction in this method. A simple example can be seen in [ValidatedNumber], where this method bounds the input number to within the max and min values provided.
     *
     * @param input T. An instance of type T to be validated and corrected as needed, where T is the type of value wrapped in this Field.
     * @return a [ValidationResult] that wraps the validated and/or corrected result of type T, along with an error message if needed.
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    protected abstract fun validateAndCorrectInputs(input: T): ValidationResult<T>

    protected abstract fun validate(input: T): ValidationResult<T>

    protected abstract fun reset()

    /**
     * A setter method for the [storedValue] that first validates the value being set and then stores the post-validation result.
     *
     * @param input T. the pre-validation input of type T that will be validated and then stored, where T is the type of the wrapped value in this field.
     * @return ValidationResult of the input, after corrections, with applicable error messages.
     * @author fzzyhmstrs
     * @since 0.1.0
     */
    open fun validateAndSet(input: T): ValidationResult<T> {
        val oldVal = storedValue
        val tVal1 = validateAndCorrectInputs(input)
        storedValue = tVal1.get()
        if (tVal1.isError()){
            return ValidationResult.error(tVal1.get(),"Error validating and setting input [$input]. Corrected to [${tVal1.get()}] >>>> Possible reasons: [${tVal1.getError()}]")
        }
        return ValidationResult.success(storedValue)
    }

    open fun setAndUpdate(input: T) {
        val oldVal = storedValue
        val tVal1 = validateAndCorrectInputs(input)
        storedValue = tVal1.get()
        if (tVal1.isError()){
            UpdateManager.addUpdateMessage(FcText.translatable("validated_field.update.error",tVal1.getError(),oldVal.toString(),storedValue.toString()))
        } else {
            UpdateManager.addUpdateMessage(updateMessage(oldVal, storedValue))
        }
        val update = Update(updateMessage(oldVal, storedValue), { set(oldVal) }, { set(tVal1.get()) })
        update(update)
    }

    open fun updateMessage(old: T, new: T): Text {
        return FcText.translatable("validated_field.update", old.toString(), new.toString())
    }

    private fun set(input: T): Text{
        val text = FcText.translatable("validated_field.set", input.toString(), storedValue.toString())
        storedValue = input
        return text
    }

    /**
     * Get the wrapped value with this method
     *
     * @return The wrapped value inside this Field
     * @author fzzyhmstrs
     * @since 0.1.0
     */
    open fun get(): T{
        return storedValue
    }

    fun createEntry(): ConfigEntry{
        val translation = translation().takIf{ it.getString() != translationKey() } ?: FcText.literal(this::class.java.simpleName)
        val description = description().takIf{ it.getString() != descriptionKey() } ?: FcText.empty()
        return createEntry(translation(), description())
    }

    @Environment(EnvType.CLIENT)
    abstract fun createEntry(name: Text, desc: Text): ConfigEntry

    override fun translationKey(): String {
        return getUpdateKey()
    }
    override fun translation(): Text {
        return FcText.translatable(translationKey())
    }

    override fun descriptionKey(): String {
        return getUpdateKey() + ".desc"
    }
    override fun description(): Text {
        return FcText.translatable(descriptionKey())
    }

    
    /**
     * Deserializes individual entries in a complex [ValidatedField]
     *
     * SAM: [deserialize] takes a TomlElement, returns a deserialized instance of T
     * @author fzzyhmstrs
     * @since 0.1.1
     */
    fun interface EntryDeserializer<T> {
        fun deserialize(toml: TomlElement): T
    }

    /**
     * Validates individual entries in a complex [ValidatedField].
     *
     * For example, in a [ValidatedList], individual new additions need to be validated, and validation of the entire list will take place as a piece-wise validation of each element, to preserve as much of the valid contents as possible
     *
     * SAM: [validate] takes an input of type T, returns a [ValidationResult]<T>
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun interface EntryValidator<T> {
        fun validate(input: T): ValidationResult<T>
    }

}
