package me.fzzyhmstrs.fzzy_config.validated_field_v2

import me.fzzyhmstrs.fzzy_config.api.StringTranslatable
import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import me.fzzyhmstrs.fzzy_config.config.*
import me.fzzyhmstrs.fzzy_config.impl.FzzySerializable
import me.fzzyhmstrs.fzzy_config.updates.Updatable
import me.fzzyhmstrs.fzzy_config.updates.UpdateManager
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.Update
import me.fzzyhmstrs.fzzy_config.validated_field_v2.entry.Entry
import me.fzzyhmstrs.fzzy_config.validated_field_v2.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.validated_field_v2.list.ValidatedList
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
 * @author fzzyhmstrs
 * @since 0.1.0
 */

abstract class ValidatedField<T: Any>(protected var storedValue: T, protected val defaultValue: T = storedValue):
    FzzySerializable,
    Entry<T>,
    Updatable,
    StringTranslatable
{

    private var pushedValue: T? = null
    private var updateKey = ""

    override fun getUpdateKey(): String {
        return updateKey
    }
    override fun setUpdateKey(key: String) {
        updateKey = key
    }
    override fun restoreDefault(){
        reset()
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

    @Deprecated("use deserializeEntry to avoid accidentally overwriting validation and error reporting")
    override fun deserialize(
        toml: TomlElement,
        errorBuilder: MutableList<String>,
        fieldName: String,
        ignoreNonSync: Boolean
    ): ValidationResult<Boolean> {
        val tVal = deserializeEntry(toml, fieldName)
        if (tVal.isError()){
            return ValidationResult.error(false,"Error deserializing config entry [$fieldName], using default value [${tVal.get()}]  >>> Possible reasons: ${tVal.getError()}")
        }
        val tVal2 = correctEntry(tVal.get(), EntryValidator.ValidationType.WEAK)
        storedValue = tVal2.get()
        if (tVal2.isError()){
            return ValidationResult.error(false,"Config entry [$fieldName] had validation errors, corrected to [${tVal2.get()}]  >>> Possible reasons: ${tVal2.getError()}")
        }
        return ValidationResult.success(true)
    }

    /**
     * Serializes the wrapped value of this validated field, and ONLY the wrapped value. Validation is maintained internally. If this field stores an Int, serialization is equivalent to a JSON Primitive storing an integer value.
     *
     * @return A JSON Element with the serialized wrapped value contained within.
     */

    @Deprecated("use serializeEntry for consistency and to enable usage in list- and map-based Fields", ReplaceWith("serializeEntry(input: T)"))
    override fun serialize(errorBuilder: MutableList<String>, ignoreNonSync: Boolean): TomlElement {
        return serializeEntry(storedValue)
    }

    /**
     * Perform input validation and correction in this method. A simple example can be seen in [ValidatedNumber], where this method bounds the input number to within the max and min values provided.
     *
     * @param input T. An instance of type T to be validated and corrected as needed, where T is the type of value wrapped in this Field.
     * @return a [ValidationResult] that wraps the validated and/or corrected result of type T, along with an error message if needed.
     * @author fzzyhmstrs
     * @since 0.2.0
     */

    protected open fun reset(){
        storedValue = defaultValue
    }

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
        val tVal1 = correctEntry(input, EntryValidator.ValidationType.WEAK)
        storedValue = tVal1.get()
        if (tVal1.isError()){
            return ValidationResult.error(tVal1.get(),"Error validating and setting input [$input]. Corrected to [${tVal1.get()}] >>>> Possible reasons: [${tVal1.getError()}]")
        }
        return ValidationResult.success(storedValue)
    }

    open fun setAndUpdate(input: T) {
        val oldVal = storedValue
        val tVal1 = correctEntry(input, EntryValidator.ValidationType.STRONG)
        storedValue = tVal1.get()
        if (tVal1.isError()){
            UpdateManager.addUpdateMessage(getUpdateKey(),FcText.translatable("validated_field.update.error",tVal1.getError(),oldVal.toString(),storedValue.toString()))
        } else {
            UpdateManager.addUpdateMessage(getUpdateKey(),updateMessage(oldVal, storedValue))
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

    override fun translationKey(): String {
        return getUpdateKey()
    }

    override fun descriptionKey(): String {
        return getUpdateKey() + ".desc"
    }

    fun toList(vararg elements: T): ValidatedList<T> {
        return ValidatedList(listOf(*elements), this)
    }
    fun toList(collection: Collection<T>): ValidatedList<T> {
        return ValidatedList(collection.toList(), this)
    }
}