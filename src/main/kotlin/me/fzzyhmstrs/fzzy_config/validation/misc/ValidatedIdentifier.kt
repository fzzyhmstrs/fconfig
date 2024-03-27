package me.fzzyhmstrs.fzzy_config.validation.misc

import me.fzzyhmstrs.fzzy_config.api.Translatable
import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import me.fzzyhmstrs.fzzy_config.updates.Updatable
import me.fzzyhmstrs.fzzy_config.util.AllowableIdentifiers
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.validation.entry.Entry
import me.fzzyhmstrs.fzzy_config.validation.entry.EntryValidator
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.registry.tag.TagKey
import net.minecraft.util.Identifier
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral
import java.util.function.BiPredicate
import java.util.function.Predicate
import java.util.function.Supplier
import java.util.function.UnaryOperator

/**
 * A validated Identifier field.
 *
 * NOTE: The base handler of this validated field is actually string. As such, usage in, for example, a [ValidatedList][me.fzzyhmstrs.fzzy_config.validation.list.ValidatedList] will yield a List<String>
 *
 * There are various shortcut methods available for building ValidatedIdentifiers more easily than with the primary constructor. Check out options in the See Also section
 * @param defaultValue String, the string value of the default identifier
 * @param allowableIds [AllowableIdentifiers] instance. Defines the predicate for valid ids, and the supplier of valid id lists
 * @param validator [EntryValidator]<String> handles validation of individual entries. Defaults to validation based on the predicate provided in allowableIds
 * @see [me.fzzyhmstrs.fzzy_config.validation.list.ValidatedIdentifierList]
 * @see [me.fzzyhmstrs.fzzy_config.validation.map.ValidatedIdentifierMap]
 * @see [ofTag]
 * @see [ofRegistry]
 * @see [ofList]
 * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedMiscExamples.validatedIdentifier]
 * @author fzzyhmstrs
 * @since 0.1.2
 */
@Suppress("unused")
class ValidatedIdentifier @JvmOverloads constructor(defaultValue: Identifier, private val allowableIds: AllowableIdentifiers, private val validator: EntryValidator<Identifier> = default(allowableIds))
    :
    ValidatedField<Identifier>(defaultValue),
    Updatable,
    Translatable,
    Comparable<Identifier>
{
    /**
     * An unbounded validated identifier
     *
     * Validation will be limited to ensuring inputs are valid identifiers
     * @param defaultValue [Identifier] the default identifier for this valdiation
     * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedMiscExamples.unboundedIdentifier]
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    constructor(defaultValue: Identifier): this(defaultValue, AllowableIdentifiers.ANY)

    /**
     * An unbounded validated identifier constructed from a string
     *
     * Validation will be limited to ensuring inputs are valid identifiers
     * @param defaultValue [Identifier] the default identifier for this valdiation
     * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedMiscExamples.stringIdentifier]
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    constructor(defaultValue: String): this(Identifier(defaultValue), AllowableIdentifiers.ANY)

    /**
     * An unbounded validated identifier constructed from namespace and path strings
     *
     * Validation will be limited to ensuring inputs are valid identifiers
     * @param defaultValue [Identifier] the default identifier for this valdiation
     * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedMiscExamples.stringStringIdentifier]
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    constructor(defaultNamespace: String, defaultPath: String): this(Identifier(defaultNamespace, defaultPath), AllowableIdentifiers.ANY)

    override fun copyStoredValue(): Identifier {
        return Identifier(storedValue.toString())
    }

    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<Identifier> {
        return try {
            val string = toml.toString()
            val id = Identifier.tryParse(string) ?: return ValidationResult.error(storedValue,"Invalid identifier [$fieldName].")
            ValidationResult.success(id)
        } catch (e: Exception){
            ValidationResult.error(storedValue,"Critical error deserializing identifier [$fieldName]: ${e.localizedMessage}")
        }
    }

    override fun serialize(input: Identifier): ValidationResult<TomlElement> {
        return ValidationResult.success(TomlLiteral(input.toString()))
    }

    override fun correctEntry(input: Identifier, type: EntryValidator.ValidationType): ValidationResult<Identifier> {
        val result = validator.validateEntry(input, type)
        return if(result.isError()) {ValidationResult.error(storedValue, "Invalid identifier [$input] found, corrected to [$storedValue]: ${result.getError()}")} else result
    }

    override fun validateEntry(input: Identifier, type: EntryValidator.ValidationType): ValidationResult<Identifier> {
        return validator.validateEntry(input, type)
    }

    override fun instanceEntry(): Entry<Identifier> {
        return ValidatedIdentifier(copyStoredValue(), allowableIds, validator)
    }

    override fun widgetEntry(): ClickableWidget {
        TODO("Not yet implemented")
    }

    ////////////////////////

    /**
     * @return the path of the cached Identifier
     */
    fun getPath(): String {
        return storedValue.path
    }
    /**
     * @return the namespace of the cached Identifier
     */
    fun getNamespace(): String {
        return storedValue.namespace
    }

    fun withPath(path: String?): Identifier {
        return storedValue.withPath(path)
    }

    fun withPath(pathFunction: UnaryOperator<String?>): Identifier {
        return storedValue.withPath(pathFunction)
    }

    fun withPrefixedPath(prefix: String): Identifier {
        return storedValue.withPrefixedPath(prefix)
    }

    fun withSuffixedPath(suffix: String): Identifier {
        return storedValue.withSuffixedPath(suffix)
    }

    override fun toString(): String {
        return storedValue.toString()
    }

    override fun translationKey(): String {
        return getUpdateKey()
    }

    override fun descriptionKey(): String {
        return getUpdateKey() + ".desc"
    }

    override fun equals(other: Any?): Boolean {
        return storedValue == other
    }

    override fun hashCode(): Int {
        return storedValue.hashCode()
    }

    override fun compareTo(other: Identifier): Int {
        return storedValue.compareTo(other)
    }

    fun toUnderscoreSeparatedString(): String {
        return storedValue.toUnderscoreSeparatedString()
    }

    fun toTranslationKey(): String {
        return storedValue.toTranslationKey()
    }

    fun toShortTranslationKey(): String {
        return storedValue.toShortTranslationKey()
    }

    fun toTranslationKey(prefix: String): String {
        return storedValue.toTranslationKey(prefix)
    }

    fun toTranslationKey(prefix: String, suffix: String): String? {
        return storedValue.toTranslationKey(prefix, suffix)
    }

    @Suppress("DeprecatedCallableAddReplaceWith")
    companion object{

        @JvmStatic
        val DEFAULT_WEAK: EntryValidator<Identifier> = EntryValidator { i, _ -> ValidationResult.success(i) }

        /**
         * builds a String EntryValidator with default behavior
         *
         * Use if your identifier list may not be available at load-time (during modInitializtion, typically), but will be available during updating (in-game). Lists from a Tag or Registry are easy examples, as the registry may not be fully populated yet, and the tag may not be loaded.
         * @param allowableIds an [AllowableIdentifiers] instance.
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun default(allowableIds: AllowableIdentifiers): EntryValidator<Identifier> {
            return EntryValidator.Builder<Identifier>()
                .weak(DEFAULT_WEAK)
                .strong { i, _ -> ValidationResult.predicated(i, allowableIds.test(i), "Identifier invalid or not allowed") }
                .buildValidator()
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
        fun strong(allowableIds: AllowableIdentifiers): EntryValidator<Identifier> {
            return EntryValidator.Builder<Identifier>()
                .weak { i, _ -> ValidationResult.predicated(i, allowableIds.test(i), "Identifier invalid or not allowed") }
                .strong { i, _ -> ValidationResult.predicated(i, allowableIds.test(i), "Identifier invalid or not allowed") }
                .buildValidator()
        }

        /**
         * Builds a ValidatedIdentifier based on an allowable tag of values
         * @param defaultValue the default value of the ValidatedIdentifier
         * @param tag the tag of allowable values to choose from
         * @return [ValidatedIdentifier] wrapping the provided default and tag
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        fun <T> ofTag(defaultValue: Identifier, tag: TagKey<T>): ValidatedIdentifier{
            val maybeRegistry = Registries.REGISTRIES.getOrEmpty(tag.registry().value)
            if (maybeRegistry.isEmpty) return ValidatedIdentifier(defaultValue, AllowableIdentifiers({ false }, { listOf() }))
            val registry = maybeRegistry.get() as? Registry<T> ?: return ValidatedIdentifier(defaultValue, AllowableIdentifiers({ false }, { listOf() }))
            val supplier = Supplier { registry.iterateEntries(tag).mapNotNull { registry.getId(it.value()) } }
            return ValidatedIdentifier(defaultValue, AllowableIdentifiers({ id -> supplier.get().contains(id) }, supplier))
        }

        /**
         * Builds a ValidatedIdentifier based on an allowable tag of values
         *
         * Uses "minecraft:air" as the default value.
         * @param tag the tag of allowable values to choose from
         * @return [ValidatedIdentifier] wrapping the provided tag
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        @Deprecated("Only use for validation in a list or map")
        fun <T> ofTag(tag: TagKey<T>): ValidatedIdentifier{
            val maybeRegistry = Registries.REGISTRIES.getOrEmpty(tag.registry().value)
            if (maybeRegistry.isEmpty) return ValidatedIdentifier(Identifier("minecraft:air"), AllowableIdentifiers({ false }, { listOf() }))
            val registry = maybeRegistry.get() as? Registry<T> ?: return ValidatedIdentifier(Identifier("minecraft:air"), AllowableIdentifiers({ false }, { listOf() }))
            val supplier = Supplier { registry.iterateEntries(tag).mapNotNull { registry.getId(it.value()) } }
            return ValidatedIdentifier(Identifier("minecraft:air"), AllowableIdentifiers({ id -> supplier.get().contains(id) }, supplier))
        }
        /**
         * Builds a ValidatedIdentifier based on an allowable registry of values
         * @param defaultValue the default value of the ValidatedIdentifier
         * @param registry the registry whose ids are valid for this identifier
         * @return [ValidatedIdentifier] wrapping the provided default and registry
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun <T> ofRegistry(defaultValue: Identifier, registry: Registry<T>): ValidatedIdentifier {
            return ValidatedIdentifier(defaultValue, AllowableIdentifiers({ id -> registry.containsId(id) }, { registry.ids.toList() }))
        }

        /**
         * Builds a ValidatedIdentifier based on an allowable registry of values, filtered by the provided predicate
         * @param defaultValue the default value of the ValidatedIdentifier
         * @param registry the registry whose ids are valid for this identifier
         * @param predicate Predicate<RegistryEntry> tests an allowable subset of the registry
         * @return [ValidatedIdentifier] wrapping the provided default and predicated registry
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun <T> ofRegistry(defaultValue: Identifier, registry: Registry<T>, predicate: Predicate<RegistryEntry<T>>): ValidatedIdentifier {
            return ValidatedIdentifier(defaultValue,
                AllowableIdentifiers(
                    { id -> registry.containsId(id) && predicate.test ((registry.getEntry(id).takeIf { it.isPresent } ?: return@AllowableIdentifiers false).get()) },
                    { registry.ids.filter { id -> predicate.test ((registry.getEntry(id).takeIf { it.isPresent } ?: return@filter false).get()) } }
                )
            )
        }

        /**
         * Builds a ValidatedIdentifier based on an allowable registry of values
         *
         * Uses "minecraft:air" as the default value
         * @param registry the registry whose ids are valid for this identifier
         * @return [ValidatedIdentifier] wrapping the provided registry
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        @Deprecated("Only use for validation in a list or map")
        fun <T> ofRegistry(registry: Registry<T>): ValidatedIdentifier {
            return ValidatedIdentifier(Identifier("minecraft:air"), AllowableIdentifiers({ id -> registry.containsId(id) }, { registry.ids.toList() }))
        }

        /**
         * Builds a ValidatedIdentifier based on an allowable registry of values, filtered by the provided predicate
         *
         * Uses "minecraft:air" as the default value
         * @param registry the registry whose ids are valid for this identifier
         * @param predicate [BiPredicate]<RegistryEntry> tests an allowable subset of the registry
         * @return [ValidatedIdentifier] wrapping the provided predicated registry
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        @Deprecated("Only use for validation in a list or map")
        fun <T> ofRegistry(registry: Registry<T>, predicate: BiPredicate<Identifier,RegistryEntry<T>>): ValidatedIdentifier {
            return ValidatedIdentifier(Identifier("minecraft:air"),
                AllowableIdentifiers(
                    { id -> registry.containsId(id) && predicate.test (id, (registry.getEntry(id).takeIf { it.isPresent } ?: return@AllowableIdentifiers false).get()) },
                    { registry.ids.filter { id -> predicate.test (id, (registry.getEntry(id).takeIf { it.isPresent } ?: return@filter false).get()) } }
                )
            )
        }
        /**
         * Builds a ValidatedIdentifier based on an allowable list of values
         *
         * This list should be available and complete at validation time
         * @param defaultValue the default value of the ValidatedIdentifier
         * @param list the list whose entries are valid for this identifier
         * @return [ValidatedIdentifier] wrapping the provided default and list
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        @Deprecated("Make sure your list is available at Validation time! (Typically at ModInitializer call or earlier)")
        fun ofList(defaultValue: Identifier, list: List<Identifier>): ValidatedIdentifier{
            val allowableIds = AllowableIdentifiers({ id -> list.contains(id) }, list.supply())
            val validator = strong(allowableIds)
            return ValidatedIdentifier(defaultValue, allowableIds, validator)
        }

        /**
         * Builds a ValidatedIdentifier based on an allowable list of values
         *
         * This list should be available and complete at validation time
         *
         * uses "minecraft:air" as the default value
         * @param list the list whose entries are valid for this identifier
         * @return [ValidatedIdentifier] wrapping the provided list
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        @Deprecated("Use only for validation of a list or map. Make sure your list is available at Validation time! (Typically at ModInitializer call or earlier)")
        fun ofList(list: List<Identifier>): ValidatedIdentifier{
            val allowableIds = AllowableIdentifiers({ id -> list.contains(id) }, list.supply())
            val validator = strong(allowableIds)
            return ValidatedIdentifier(Identifier("minecraft:air"), allowableIds, validator)
        }

        /**
         * wraps a list in a [Supplier]
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @Deprecated("Make sure your list is available at Validation time! (Typically at ModInitializer call or earlier)")
        fun<T> List<T>.supply(): Supplier<List<T>>{
            return Supplier { this }
        }
    }
}
