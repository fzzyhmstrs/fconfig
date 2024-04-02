package me.fzzyhmstrs.fzzy_config.validation

import me.fzzyhmstrs.fzzy_config.util.Translatable
import me.fzzyhmstrs.fzzy_config.entry.Entry
import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.updates.Updatable
import me.fzzyhmstrs.fzzy_config.updates.UpdateManager
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.report
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedList
import net.minecraft.network.PacketByteBuf
import net.minecraft.text.Text
import net.peanuuutz.tomlkt.TomlElement

/**
 * Validated Field Collection - serialization is indistinguishable from their wrapped values, but deserialized into a validated wrapper
 *
 * Validated Fields CANNOT be serialized and deserialized by GSON or other "automagic" serializers properly. The Toml Element does not provide enough context, because the validation is hidden within code only, not serialized. These fields are not building new classes from scratch, they are updating and validating a pre-existing default class framework.
 *
 * Helper methods are provided to more easily sync configs directly via [PacketByteBuf]s, rather than serializing and then deserializing the entire JSON
 *
 * @param storedValue T. The wrapped value that this field validates, serializes, and syncs between server and client.
 * @sample me.fzzyhmstrs.fzzy_config.examples.ExampleTranslations.lang
 * @author fzzyhmstrs
 * @since 0.1.0
 */

@Suppress("DeprecatedCallableAddReplaceWith")
abstract class ValidatedField<T>(protected var storedValue: T, protected val defaultValue: T = storedValue):
    Entry<T,ValidatedField<T>>,
    Updatable,
    Translatable
{

    private var pushedValue: T? = null
    private var updateKey = ""
    private var updateManager: UpdateManager? = null

    @Deprecated("Internal Method, don't Override unless you know what you are doing!")
    override fun getUpdateManager(): UpdateManager? {
        return updateManager
    }
    @Deprecated("Internal Method, don't Override unless you know what you are doing!")
    override fun setUpdateManager(manager: UpdateManager) {
        this.updateManager = manager
    }
    @Deprecated("Internal Method, don't Override unless you know what you are doing!")
    override fun getEntryKey(): String {
        return updateKey
    }
    @Deprecated("Internal Method, don't Override unless you know what you are doing!")
    override fun setEntryKey(key: String) {
        updateKey = key
    }
    @Deprecated("Internal Method, don't Override unless you know what you are doing!")
    override fun isDefault(): Boolean {
        return defaultValue == storedValue
    }
    @Deprecated("Internal Method, don't Override unless you know what you are doing!")
    override fun restore(){
        reset()
        updateManager?.addUpdateMessage(getEntryKey(), FcText.translatable("fc.validated_field.default",translation(), defaultValue.toString()))
    }
    @Deprecated("Internal Method, don't Override unless you know what you are doing!")
    override fun revert() {
        if(pushedValue != null){
            try {
                pushedValue?.let {
                    storedValue = it
                    updateManager?.addUpdateMessage(getEntryKey(), FcText.translatable("fc.validated_field.revert",translation(), storedValue.toString(), pushedValue.toString()))
                }

            } catch (e: Exception){
                updateManager?.addUpdateMessage(getEntryKey(),FcText.translatable("fc.validated_field.revert.error",translation(), e.localizedMessage))
            }
        } else {
            updateManager?.addUpdateMessage(getEntryKey(),FcText.translatable("fc.validated_field.revert.error",translation(), "Unexpected null PushedState."))
        }
    }
    @Deprecated("Internal Method, don't Override unless you know what you are doing!")
    override fun pushState(){
        pushedValue = copyStoredValue()
    }
    @Deprecated("Internal Method, don't Override unless you know what you are doing!")
    override fun peekState(): Boolean {
        return pushedValue != storedValue
    }
    @Deprecated("Internal Method, don't Override unless you know what you are doing!")
    override fun popState(): Boolean{
        if (pushedValue == null) return false
        val updated = pushedValue != storedValue
        pushedValue = null
        return updated
    }

    abstract fun copyStoredValue(): T

    @Deprecated("use deserialize to avoid accidentally overwriting validation and error reporting")
    override fun deserializeEntry(
        toml: TomlElement,
        errorBuilder: MutableList<String>,
        fieldName: String,
        ignoreNonSync: Boolean
    ): ValidationResult<T> {
        val tVal = deserialize(toml, fieldName) //1
        if (tVal.isError()){ //2
            return ValidationResult.error(storedValue,"Error deserializing config entry [$fieldName], using default value [${tVal.get()}]  >>> Possible reasons: ${tVal.getError()}")
        }
        //3
        val tVal2 = correctEntry(tVal.get(), EntryValidator.ValidationType.WEAK)
        storedValue = tVal2.get() //4
        if (tVal2.isError()){ //5
            return ValidationResult.error(storedValue,"Config entry [$fieldName] had validation errors, corrected to [${tVal2.get()}]  >>> Possible reasons: ${tVal2.getError()}")
        }
        return ValidationResult.success(storedValue)
    }

    abstract fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<T>

    /**
     * Serializes the wrapped value of this validated field, and ONLY the wrapped value. Validation is maintained internally. If this field stores an Int, serialization is equivalent to a JSON Primitive storing an integer value.
     *
     * @return A JSON Element with the serialized wrapped value contained within.
     */

    @Deprecated(
        "use serialize for consistency and to enable usage in list- and map-based Fields",
        ReplaceWith("serializeEntry(input: T)")
    )
    override fun serializeEntry(input: T?, errorBuilder: MutableList<String>, ignoreNonSync: Boolean): TomlElement {
        return (if(input != null) serialize(input) else serialize(storedValue)).report(errorBuilder).get()
    }

    fun trySerialize(input: Any?, errorBuilder: MutableList<String>, ignoreNonSync: Boolean): TomlElement?{
        return try {
            serializeEntry(input as T?,errorBuilder, ignoreNonSync)
        } catch (e: Exception){
            null
        }
    }

    abstract fun serialize(input: T): ValidationResult<TomlElement>

    override fun supplyEntry(): T {
        return get()
    }

    override fun accept(input: T) {
        setAndUpdate(input)
    }

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
        val message = if (tVal1.isError()){
            FcText.translatable("fc.validated_field.update.error",translation(),oldVal.toString(),storedValue.toString(),tVal1.getError())
        } else {
            updateMessage(oldVal, storedValue)
        }
        update(message)
    }

    open fun updateMessage(old: T, new: T): Text {
        return FcText.translatable("fc.validated_field.update",translation(), old.toString(), new.toString())
    }

    /**
     * Get the wrapped value with this method
     *
     * This method is implemented from [java.util.function.Supplier].
     * @return The wrapped value inside this Field
     * @author fzzyhmstrs
     * @since 0.1.0
     */
    override fun get(): T{
        return storedValue
    }

    override fun translationKey(): String {
        return getEntryKey()
    }

    override fun descriptionKey(): String {
        return getEntryKey() + ".desc"
    }

    /**
     * wraps the provided values into a [ValidatedList] with this field as validation
     * @param elements the inputs for the list generation. Same type as this field
     * @return [ValidatedList] wrapping the provided values and this field as validation
     * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedCollectionExamples.listFromFieldVararg
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun toList(vararg elements: T): ValidatedList<T> {
        return ValidatedList(listOf(*elements), this)
    }
    /**
     * wraps the provided collection into a [ValidatedList] with this field as validation
     * @param collection the collection to wrap. Same type as this field
     * @return [ValidatedList] wrapping the collection and this field as validation
     * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedCollectionExamples.listFromFieldCollection
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun toList(collection: Collection<T>): ValidatedList<T> {
        return ValidatedList(collection.toList(), this)
    }
}