package me.fzzyhmstrs.fzzy_config.validation

import me.fzzyhmstrs.fzzy_config.entry.Entry
import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.updates.Updatable
import me.fzzyhmstrs.fzzy_config.updates.UpdateManager
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.Translatable
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.report
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedList
import net.minecraft.network.PacketByteBuf
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.peanuuutz.tomlkt.TomlElement
import org.jetbrains.annotations.ApiStatus.Internal

/**
 * Validated Field Collection - serialization is indistinguishable from their wrapped values, but deserialized into a validated wrapper
 *
 * Validated Fields CANNOT be serialized and deserialized by GSON or other "automagic" serializers properly. The Toml Element does not provide enough context, because the validation is hidden within code only, not serialized. These fields are not building new classes from scratch, they are updating and validating a pre-existing default class framework.
 *
 * Helper methods are provided to more easily sync configs directly via [PacketByteBuf]s, rather than serializing and then deserializing the entire JSON
 * @param T Type of the wrapped value
 * @param storedValue T. The wrapped value that this field validates, serializes, and syncs between server and client.
 * @param defaultValue T. The default value of the wrapped value
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

    /**
     * @suppress
     */
    @Internal
    @Deprecated("Internal Method, don't Override unless you know what you are doing!")
    override fun getUpdateManager(): UpdateManager? {
        return updateManager
    }
    /**
     * @suppress
     */
    @Internal
    @Deprecated("Internal Method, don't Override unless you know what you are doing!")
    override fun setUpdateManager(manager: UpdateManager) {
        this.updateManager = manager
    }
    /**
     * @suppress
     */
    @Internal
    @Deprecated("Internal Method, don't Override unless you know what you are doing!")
    override fun getEntryKey(): String {
        return updateKey
    }
    /**
     * @suppress
     */
    @Internal
    @Deprecated("Internal Method, don't Override unless you know what you are doing!")
    override fun setEntryKey(key: String) {
        updateKey = key
    }
    /**
     * @suppress
     */
    @Internal
    @Deprecated("Internal Method, don't Override unless you know what you are doing!")
    override fun isDefault(): Boolean {
        return defaultValue == get()
    }
    /**
     * @suppress
     */
    @Internal
    @Deprecated("Internal Method, don't Override unless you know what you are doing!")
    override fun restore(){
        reset()
        updateManager?.addUpdateMessage(this, FcText.translatable("fc.validated_field.default",translation(), defaultValue.toString()))
    }
    /**
     * @suppress
     */
    @Internal
    @Deprecated("Internal Method, don't Override unless you know what you are doing!")
    override fun revert() {
        if(pushedValue != null){
            try {
                pushedValue?.let {
                    updateManager?.addUpdateMessage(this, FcText.translatable("fc.validated_field.revert",translation(), get().toString(), pushedValue.toString()))
                    set(it)
                }
            } catch (e: Exception){
                updateManager?.addUpdateMessage(this,FcText.translatable("fc.validated_field.revert.error",translation(), e.localizedMessage))
            }
        } else {
            updateManager?.addUpdateMessage(this,FcText.translatable("fc.validated_field.revert.error",translation(), "Unexpected null PushedState."))
        }
    }
    /**
     * @suppress
     */
    @Internal
    @Deprecated("Internal Method, don't Override unless you know what you are doing!")
    override fun pushState(){
        pushedValue = copyStoredValue()
    }
    /**
     * @suppress
     */
    @Internal
    @Deprecated("Internal Method, don't Override unless you know what you are doing!")
    override fun peekState(): Boolean {
        return pushedValue != get()
    }
    /**
     * @suppress
     */
    @Internal
    @Deprecated("Internal Method, don't Override unless you know what you are doing!")
    override fun popState(): Boolean{
        if (pushedValue == null) return false
        val updated = pushedValue != get()
        pushedValue = null
        return updated
    }

    open fun copyStoredValue(): T{
        return get()
    }

    /**
     * @suppress
     */
    @Internal
    @Deprecated("use deserialize to avoid accidentally overwriting validation and error reporting")
    override fun deserializeEntry(
        toml: TomlElement,
        errorBuilder: MutableList<String>,
        fieldName: String,
        ignoreNonSync: Boolean
    ): ValidationResult<T> {
        val tVal = deserialize(toml, fieldName) //1
        if (tVal.isError()){ //2
            return ValidationResult.error(get(),"Error deserializing config entry [$fieldName], using default value [${tVal.get()}]  >>> Possible reasons: ${tVal.getError()}")
        }
        //3
        val tVal2 = correctEntry(tVal.get(), EntryValidator.ValidationType.WEAK)
        set(tVal2.get()) //4
        if (tVal2.isError()){ //5
            return ValidationResult.error(get(),"Config entry [$fieldName] had validation errors, corrected to [${tVal2.get()}]  >>> Possible reasons: ${tVal2.getError()}")
        }
        return ValidationResult.success(get())
    }

    abstract fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<T>

    /**
     * @suppress
     */
    @Internal
    @Deprecated(
        "use serialize for consistency and to enable usage in list- and map-based Fields",
        ReplaceWith("serializeEntry(input: T)")
    )
    override fun serializeEntry(input: T?, errorBuilder: MutableList<String>, ignoreNonSync: Boolean): TomlElement {
        return (if(input != null) serialize(input) else serialize(get())).report(errorBuilder).get()
    }

    fun trySerialize(input: Any?, errorBuilder: MutableList<String>, ignoreNonSync: Boolean): TomlElement? {
        return try {
            @Suppress("DEPRECATION", "UNCHECKED_CAST")
            serializeEntry(input as T?,errorBuilder, ignoreNonSync)
        } catch (e: Exception){
            null
        }
    }

    abstract fun serialize(input: T): ValidationResult<TomlElement>

    override fun correctEntry(input: T, type: EntryValidator.ValidationType): ValidationResult<T> {
        return validateEntry(input, type)
    }

    override fun validateEntry(input: T, type: EntryValidator.ValidationType): ValidationResult<T> {
        return ValidationResult.success(input)
    }

    @Internal
    override fun canCopyEntry(): Boolean {
        return true
    }

    protected open fun reset() {
        setAndUpdate(defaultValue)
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
        set(tVal1.get())
        if (tVal1.isError()){
            return ValidationResult.error(tVal1.get(),"Error validating and setting input [$input]. Corrected to [${tVal1.get()}] >>>> Possible reasons: [${tVal1.getError()}]")
        }
        return ValidationResult.success(get())
    }

    @Internal
    open fun setAndUpdate(input: T) {
        if (input == get()) return
        val oldVal = get()
        val tVal1 = correctEntry(input, EntryValidator.ValidationType.STRONG)
        set(tVal1.get())
        val message = if (tVal1.isError()){
            FcText.translatable("fc.validated_field.update.error",translation(),oldVal.toString(),get().toString(),tVal1.getError())
        } else {
            updateMessage(oldVal, get())
        }
        update(message)
    }

    override fun trySet(input: Any?) {
        try {
            @Suppress("UNCHECKED_CAST")
            setAndUpdate(input as T)
        } catch (e: Exception){
            //
        }
    }

    protected open fun updateMessage(old: T, new: T): Text {
        return FcText.translatable("fc.validated_field.update",translation(),old.toString(),new.toString())
    }

    /**
     * supplies the wrapped value
     *
     * This method is implemented from [java.util.function.Supplier].
     * @return This field wrapped value
     * @author fzzyhmstrs
     * @since 0.1.0
     */
    override fun get(): T {
        return storedValue
    }

    @Internal
    protected open fun set(input: T) {
        storedValue = input
    }

    /**
     * updates the wrapped value. NOTE: this method will push updates to an UpdateManager, if any. For in-game updating consider [validateAndSet]
     *
     * This method is implemented from [java.util.function.Consumer].
     * @param input new value to wrap
     * @see validateAndSet
     * @author fzzyhmstrs
     * @since 0.1.0
     */
    override fun accept(input: T) {
        setAndUpdate(input)
    }

    override fun translationKey(): String {
        @Suppress("DEPRECATION")
        return getEntryKey()
    }

    override fun descriptionKey(): String {
        @Suppress("DEPRECATION")
        return getEntryKey() + ".desc"
    }

    override fun translation(): MutableText {
        return FcText.translatableWithFallback(translationKey(),this.translationKey().substringAfterLast('.').split(FcText.regex).joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } })
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