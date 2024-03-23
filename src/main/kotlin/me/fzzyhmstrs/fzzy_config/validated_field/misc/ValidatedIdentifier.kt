package me.fzzyhmstrs.fzzy_config.validated_field.misc

import me.fzzyhmstrs.fzzy_config.api.Translatable
import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import me.fzzyhmstrs.fzzy_config.updates.Updatable
import me.fzzyhmstrs.fzzy_config.updates.UpdateManager
import me.fzzyhmstrs.fzzy_config.util.AllowableIdentifiers
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.validated_field.entry.Entry
import me.fzzyhmstrs.fzzy_config.validated_field.entry.EntryValidator
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.registry.tag.TagKey
import net.minecraft.util.Identifier
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral
import java.util.function.Predicate
import java.util.function.Supplier
import java.util.function.UnaryOperator

@Suppress("unused")
class ValidatedIdentifier(private val defaultValue: String, private val allowableIds: AllowableIdentifiers, private val validator: EntryValidator<String> = default(allowableIds)):
    Entry<String>,
    Updatable,
    Translatable,
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
        return storedValue == defaultValue
    }
    override fun restore(){
        storedValue = defaultValue
        cachedIdentifier = Identifier(storedValue)
    }
    override fun revert() {
        if(pushedValue != null){
            try {
                pushedValue?.let {
                    storedValue = it
                    UpdateManager.addUpdateMessage(getUpdateKey(), FcText.translatable("fc.validated_field.revert",translation(), storedValue, pushedValue.toString()))
                }

            } catch (e: Exception){
                UpdateManager.addUpdateMessage(getUpdateKey(), FcText.translatable("fc.validated_field.revert.error",translation(), e.localizedMessage))
            }
        } else {
            UpdateManager.addUpdateMessage(getUpdateKey(), FcText.translatable("fc.validated_field.revert.error",translation(), "Unexpected null PushedState."))
        }
    }
    @Deprecated("Internal Method, don't use unless you know what you are doing!")
    override fun pushState(){
        pushedValue = String(storedValue.toCharArray())
    }
    @Deprecated("Internal Method, don't use unless you know what you are doing!")
    override fun peekState(): Boolean {
        return pushedValue != storedValue
    }
    @Deprecated("Internal Method, don't use unless you know what you are doing!")
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
            Identifier.tryParse(string)?.let { cachedIdentifier = it } ?: return ValidationResult.error(storedValue,"Invalid identifier [$fieldName].")
            storedValue = string
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
     * @return the path of the cached Identifier
     */
    fun getPath(): String {
        return cachedIdentifier.path
    }
    /**
     * @return the namespace of the cached Identifier
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

    @Suppress("DeprecatedCallableAddReplaceWith")
    companion object{

        @JvmStatic
        val DEFAULT_WEAK: EntryValidator<String> = EntryValidator { i, _ -> ValidationResult.predicated(i,Identifier.tryParse(i) != null, "Unparsable Identifier") }

        /**
         * builds a String EntryValidator with default behavior
         *
         * Use if your identifier list may not be available at load-time (during modInitializtion, typically), but will be available during updating (in-game). Lists from a Tag or Registry are easy examples, as the registry may not be fully populated yet, and the tag may not be loaded.
         * @param allowableIds an [AllowableIdentifiers] instance.
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun default(allowableIds: AllowableIdentifiers): EntryValidator<String> {
            return EntryValidator.Builder<String>()
                .weak(DEFAULT_WEAK)
                .strong { i, _ -> ValidationResult.predicated(i, (Identifier.tryParse(i)?.let { allowableIds.test(it) } ?: false), "Identifier invalid or not allowed") }
                .build()
        }

        /**
         * builds a String EntryValidator with always-strong behavior
         *
         * Use if your identifier list is available both at loading (during modInitializtion, typically), and during updating (in-game).
         * @param allowableIds an [AllowableIdentifiers] instance.
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun strong(allowableIds: AllowableIdentifiers): EntryValidator<String> {
            return EntryValidator.Builder<String>()
                .weak { i, _ -> ValidationResult.predicated(i, (Identifier.tryParse(i)?.let { allowableIds.test(it) } ?: false), "Identifier invalid or not allowed") }
                .strong { i, _ -> ValidationResult.predicated(i, (Identifier.tryParse(i)?.let { allowableIds.test(it) } ?: false), "Identifier invalid or not allowed") }
                .build()
        }

        /**
         * Builds a ValidatedIdentifier based on an allowable tag of values
         * @param defaultValue the default value of the ValidatedIdentifier
         * @param tag the tag of allowable values to choose from
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        fun <T> ofTag(defaultValue: Identifier, tag: TagKey<T>): ValidatedIdentifier{
            val maybeRegistry = Registries.REGISTRIES.getOrEmpty(tag.registry().value)
            if (maybeRegistry.isEmpty) return ValidatedIdentifier(defaultValue.toString(), AllowableIdentifiers({ false }, { listOf() }))
            val registry = maybeRegistry.get() as? Registry<T> ?: return ValidatedIdentifier(defaultValue.toString(), AllowableIdentifiers({ false }, { listOf() }))
            val supplier = Supplier { registry.iterateEntries(tag).mapNotNull { registry.getId(it.value()) } }
            return ValidatedIdentifier(defaultValue.toString(), AllowableIdentifiers({ id -> supplier.get().contains(id) }, supplier))
        }
        /**
         * Builds a ValidatedIdentifier based on an allowable tag of values
         * @param defaultValue the default value of the ValidatedIdentifier
         * @param registry the registry whose ids are valid for this identifier
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun <T> ofRegistry(defaultValue: Identifier, registry: Registry<T>): ValidatedIdentifier {
            return ValidatedIdentifier(defaultValue.toString(), AllowableIdentifiers({ id -> registry.containsId(id) }, { registry.ids.toList() }))
        }

        /**
         * Builds a ValidatedIdentifier based on an allowable tag of values
         * @param defaultValue the default value of the ValidatedIdentifier
         * @param registry the registry whose ids are valid for this identifier
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun <T> ofRegistry(defaultValue: Identifier, registry: Registry<T>, predicate: Predicate<RegistryEntry<T>>): ValidatedIdentifier {
            return ValidatedIdentifier(
                defaultValue.toString(),
                AllowableIdentifiers(
                    { id -> registry.containsId(id) && predicate.test ((registry.getEntry(id).takeIf { it.isPresent } ?: return@AllowableIdentifiers false).get()) },
                    { registry.ids.filter { id -> predicate.test ((registry.getEntry(id).takeIf { it.isPresent } ?: return@filter false).get()) } }
                )
            )
        }

        @JvmStatic
        @Deprecated("Make sure your list is available at Validation time! (Typically at ModInitializer call or earlier)")
        fun ofList(defaultValue: Identifier, list: List<Identifier>): ValidatedIdentifier{
            val allowableIds = AllowableIdentifiers({ id -> list.contains(id) }, list.supply())
            val validator = strong(allowableIds)
            return ValidatedIdentifier(defaultValue.toString(), allowableIds, validator)
        }

        @Deprecated("Make sure your list is available at Validation time! (Typically at ModInitializer call or earlier)")
        fun<T> List<T>.supply(): Supplier<List<T>>{
            return Supplier { this }
        }
    }
}