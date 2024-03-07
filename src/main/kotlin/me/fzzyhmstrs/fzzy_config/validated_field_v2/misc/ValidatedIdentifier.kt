package me.fzzyhmstrs.fzzy_config.validated_field_v2.misc

import me.fzzyhmstrs.fzzy_config.api.StringTranslatable
import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import me.fzzyhmstrs.fzzy_config.impl.FzzySerializable
import me.fzzyhmstrs.fzzy_config.updates.Updatable
import me.fzzyhmstrs.fzzy_config.validated_field_v2.entry.Entry
import me.fzzyhmstrs.fzzy_config.validated_field_v2.entry.EntryValidator
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.util.Identifier
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral
import java.util.function.Supplier
import java.util.function.UnaryOperator

class ValidatedIdentifier(private val defaultValue: String, private val allowableIds: Supplier<List<Identifier>>, private val validator: EntryValidator<String> = default(allowableIds)): FzzySerializable,
    Entry<String>,
    Updatable,
    StringTranslatable,
    Comparable<Identifier>
{

    private var storedValue: String = defaultValue
    private var cachedIdentifier: Identifier = Identifier("c:/c")

    fun get(): Identifier{
        return cachedIdentifier
    }

    private var pushedValue: String? = null
    private var updateKey = ""

    override fun getUpdateKey(): String {
        return updateKey
    }
    override fun setUpdateKey(key: String) {
        updateKey = key
    }
    override fun restoreDefault(){
        storedValue = defaultValue
        cachedIdentifier = Identifier(storedValue)
    }
    override fun pushState(){
        pushedValue = String(storedValue.toCharArray())
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

    override fun deserializeEntry(toml: TomlElement, fieldName: String): ValidationResult<String> {
        return try {
            val string = toml.toString()
            if(Identifier.tryParse(string) == null) return ValidationResult.error(storedValue,"Invalid identifier [$fieldName].")
            ValidationResult.success(string)
        } catch (e: Exception){
            ValidationResult.error(storedValue,"Critical error deserializing identifier [$fieldName]: ${e.localizedMessage}")
        }
    }

    override fun serializeEntry(input: String): TomlElement {
        return TomlLiteral(input)
    }

    override fun correctEntry(input: String, type: EntryValidator.ValidationType): ValidationResult<String> {
        val result = validator.validateEntry(input, type)
        return if(result.isError()) {ValidationResult.error(storedValue, "Invalid identifier [$input] found, corrected to [$storedValue]: ${result.getError()}")} else result
    }

    override fun validateEntry(input: String, type: EntryValidator.ValidationType): ValidationResult<String> {
        return validator.validateEntry(input, type)
    }

    override fun instanceEntry(): Entry<String> {
        return ValidatedIdentifier(defaultValue, allowableIds, validator)
    }

    override fun widgetEntry(): ClickableWidget {
        TODO("Not yet implemented")
    }

    ////////////////////////

    /**
     * {@return the path of the identifier}
     */
    fun getPath(): String {
        return cachedIdentifier.path
    }

    /**
     * {@return the namespace of the identifier}
     *
     *
     * This returns {@value #DEFAULT_NAMESPACE} for identifiers created without a namespace.
     */
    fun getNamespace(): String {
        return cachedIdentifier.namespace
    }

    fun withPath(path: String?): Identifier {
        return cachedIdentifier.withPath(path)
    }

    fun withPath(pathFunction: UnaryOperator<String?>): Identifier {
        return cachedIdentifier.withPath(pathFunction)
    }

    fun withPrefixedPath(prefix: String): Identifier {
        return cachedIdentifier.withPrefixedPath(prefix)
    }

    fun withSuffixedPath(suffix: String): Identifier {
        return cachedIdentifier.withSuffixedPath(suffix)
    }

    override fun toString(): String {
        return storedValue
    }

    override fun translationKey(): String {
        return getUpdateKey()
    }

    override fun descriptionKey(): String {
        return getUpdateKey() + ".desc"
    }

    override fun equals(other: Any?): Boolean {
        return cachedIdentifier == other
    }

    override fun hashCode(): Int {
        return cachedIdentifier.hashCode()
    }

    override fun compareTo(other: Identifier): Int {
        return cachedIdentifier.compareTo(other)
    }

    fun toUnderscoreSeparatedString(): String {
        return cachedIdentifier.toUnderscoreSeparatedString()
    }

    fun toTranslationKey(): String {
        return cachedIdentifier.toTranslationKey()
    }

    fun toShortTranslationKey(): String {
        return cachedIdentifier.toShortTranslationKey()
    }

    fun toTranslationKey(prefix: String): String {
        return cachedIdentifier.toTranslationKey(prefix)
    }

    fun toTranslationKey(prefix: String, suffix: String): String? {
        return cachedIdentifier.toTranslationKey(prefix, suffix)
    }

    companion object{

        val DEFAULT_WEAK: EntryValidator<String> = EntryValidator { i, _ -> ValidationResult.predicated(i,Identifier.tryParse(i) != null, "Unparsable Identifier") }

        fun default(supplier: Supplier<List<Identifier>>): EntryValidator<String>{
            return EntryValidator.Builder<String>()
                .weak(DEFAULT_WEAK)
                .strong { i, t -> ValidationResult.predicated(i, (Identifier.tryParse(i)?.let { supplier.get().contains(it) } ?: false), "Identifier invalid or not allowed") }
                .build()
        }

    }
}