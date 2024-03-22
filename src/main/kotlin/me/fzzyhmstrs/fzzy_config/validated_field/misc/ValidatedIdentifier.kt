package me.fzzyhmstrs.fzzy_config.validated_field.misc

import me.fzzyhmstrs.fzzy_config.api.StringTranslatable
import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import me.fzzyhmstrs.fzzy_config.updates.Updatable
import me.fzzyhmstrs.fzzy_config.validated_field.entry.Entry
import me.fzzyhmstrs.fzzy_config.validated_field.entry.EntryValidator
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.tag.TagKey
import net.minecraft.util.Identifier
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral
import java.util.function.Supplier
import java.util.function.UnaryOperator

@Suppress("unused")
class ValidatedIdentifier(private val defaultValue: String, private val allowableIds: Supplier<List<Identifier>>, private val validator: EntryValidator<String> = default(allowableIds)):
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

    override fun isDefault(): Boolean {
        TODO("Not yet implemented")
    }

    override fun restore(){
        storedValue = defaultValue
        cachedIdentifier = Identifier(storedValue)
    }

    override fun revert() {
        TODO("Not yet implemented")
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

    override fun deserializeEntry(
        toml: TomlElement,
        errorBuilder: MutableList<String>,
        fieldName: String,
        ignoreNonSync: Boolean
    ): ValidationResult<String> {
        return try {
            val string = toml.toString()
            if(Identifier.tryParse(string) == null) return ValidationResult.error(storedValue,"Invalid identifier [$fieldName].")
            ValidationResult.success(string)
        } catch (e: Exception){
            ValidationResult.error(storedValue,"Critical error deserializing identifier [$fieldName]: ${e.localizedMessage}")
        }
    }

    override fun serializeEntry(
        input: String?,
        errorBuilder: MutableList<String>,
        ignoreNonSync: Boolean
    ): TomlElement {
        return TomlLiteral(input ?: storedValue)
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

        @JvmStatic
        val DEFAULT_WEAK: EntryValidator<String> = EntryValidator { i, _ -> ValidationResult.predicated(i,Identifier.tryParse(i) != null, "Unparsable Identifier") }

        @JvmStatic
        fun default(supplier: Supplier<List<Identifier>>): EntryValidator<String> {
            return EntryValidator.Builder<String>()
                .weak(DEFAULT_WEAK)
                .strong { i, t -> ValidationResult.predicated(i, (Identifier.tryParse(i)?.let { supplier.get().contains(it) } ?: false), "Identifier invalid or not allowed") }
                .build()
        }

        /**
         * builds an EntryValidator with always-strong behavior
         *
         * Use if your identifier list is available both at loading (during modInitializtion, typically), and during updating (in-game).
         * @param supplier a supplier of valid ids
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun strong(supplier: Supplier<List<Identifier>>): EntryValidator<String> {
            return EntryValidator.Builder<String>()
                .weak { i, t -> ValidationResult.predicated(i, (Identifier.tryParse(i)?.let { supplier.get().contains(it) } ?: false), "Identifier invalid or not allowed") }
                .strong { i, t -> ValidationResult.predicated(i, (Identifier.tryParse(i)?.let { supplier.get().contains(it) } ?: false), "Identifier invalid or not allowed") }
                .build()
        }

        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        fun <T> fromTag(defaultValue: Identifier, tag: TagKey<T>): ValidatedIdentifier{
            val maybeRegistry = Registries.REGISTRIES.getOrEmpty(tag.registry().value)
            if (maybeRegistry.isEmpty) return ValidatedIdentifier(defaultValue.toString(), { listOf() })
            val registry = maybeRegistry.get() as? Registry<T> ?: return ValidatedIdentifier(defaultValue.toString(), { listOf() })
            return ValidatedIdentifier(defaultValue.toString(), {  registry.iterateEntries(tag).mapNotNull { registry.getId(it.value()) } })
        }

        @JvmStatic
        fun <T> fromRegistry(defaultValue: Identifier, registry: Registry<T>): ValidatedIdentifier {
            return ValidatedIdentifier(defaultValue.toString(), {  registry.ids.toList() })
        }

        @JvmStatic
        @Deprecated("Make sure your list is available at Validation time!")
        fun fromList(defaultValue: Identifier, list: List<Identifier>): ValidatedIdentifier{
            return ValidatedIdentifier(defaultValue.toString(), list.supply())
        }

        @Deprecated("Make sure your list is available at Validation time!")
        fun<T> List<T>.supply(): Supplier<List<T>>{
            return Supplier { this }
        }

    }
}