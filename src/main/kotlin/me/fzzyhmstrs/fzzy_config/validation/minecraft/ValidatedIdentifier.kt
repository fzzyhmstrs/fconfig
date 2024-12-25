/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.validation.minecraft

import com.google.common.base.Suppliers
import com.mojang.brigadier.suggestion.Suggestions
import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.entry.EntryFlag
import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.networking.DynamicIdsS2CCustomPayload
import me.fzzyhmstrs.fzzy_config.nsId
import me.fzzyhmstrs.fzzy_config.screen.internal.SuggestionWindow
import me.fzzyhmstrs.fzzy_config.screen.internal.SuggestionWindowListener
import me.fzzyhmstrs.fzzy_config.screen.internal.SuggestionWindowProvider
import me.fzzyhmstrs.fzzy_config.screen.widget.LayoutWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.OnClickTextFieldWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.TextureIds
import me.fzzyhmstrs.fzzy_config.simpleId
import me.fzzyhmstrs.fzzy_config.updates.Updatable
import me.fzzyhmstrs.fzzy_config.util.AllowableIdentifiers
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.PortingUtils.anyOptional
import me.fzzyhmstrs.fzzy_config.util.PortingUtils.optional
import me.fzzyhmstrs.fzzy_config.util.PortingUtils.regRefId
import me.fzzyhmstrs.fzzy_config.util.PortingUtils.tagIdList
import me.fzzyhmstrs.fzzy_config.util.RenderUtil.drawTex
import me.fzzyhmstrs.fzzy_config.util.Translatable
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedIdentifier.Companion.ofList
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedIdentifier.Companion.ofRegistry
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedIdentifier.Companion.ofRegistryTags
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedIdentifier.Companion.ofSuppliedList
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedIdentifier.Companion.ofTag
import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.registry.*
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.registry.tag.TagKey
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral
import org.jetbrains.annotations.ApiStatus.Internal
import org.lwjgl.glfw.GLFW
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.*
import kotlin.jvm.optionals.getOrNull

/**
 * A validated Identifier field.
 *
 * There are various shortcut methods available for building ValidatedIdentifiers more easily than with the primary constructor. Check out options in the See Also section
 * @param defaultValue String, the string value of the default identifier
 * @param allowableIds [AllowableIdentifiers] instance. Defines the predicate for valid ids, and the supplier of valid id lists
 * @param validator [EntryValidator]<String> handles validation of individual entries. Defaults to validation based on the predicate provided in allowableIds
 * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedMiscExamples.identifiers
 * @see ofTag
 * @see ofRegistry
 * @see ofRegistryTags
 * @see ofList
 * @see ofSuppliedList

 * @author fzzyhmstrs
 * @since 0.1.2
 */
@Suppress("unused")
open class ValidatedIdentifier @JvmOverloads constructor(defaultValue: Identifier, val allowableIds: AllowableIdentifiers, private val validator: EntryValidator<Identifier> = allowableIds)
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
     * @param defaultValue [Identifier] the default identifier for this validation
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    constructor(defaultValue: Identifier): this(defaultValue, AllowableIdentifiers.ANY)

    /**
     * An unbounded validated identifier constructed from a string
     *
     * Validation will be limited to ensuring inputs are valid identifiers
     * @param defaultValue [String] the default identifier (in string form) for this validation
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    constructor(defaultValue: String): this(defaultValue.simpleId(), AllowableIdentifiers.ANY)

    /**
     * An unbounded validated identifier constructed from namespace and path strings
     *
     * Validation will be limited to ensuring inputs are valid identifiers
     * @param defaultNamespace [String] the default namespace for this validation
     * @param defaultPath [String] the default path for this validation
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    constructor(defaultNamespace: String, defaultPath: String): this(defaultNamespace.nsId(defaultPath), AllowableIdentifiers.ANY)

    /**
     * An unbounded validated identifier with a dummy default value
     *
     * Validation will be limited to ensuring inputs are valid identifiers
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    constructor(): this("c:/c".simpleId(), AllowableIdentifiers.ANY)

    @Internal
    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<Identifier> {
        return try {
            val string = toml.toString()
            val id = Identifier.tryParse(string) ?: return ValidationResult.error(storedValue, "Invalid identifier [$fieldName].")
            ValidationResult.success(id)
        } catch (e: Throwable) {
            ValidationResult.error(storedValue, "Critical error deserializing identifier [$fieldName]: ${e.localizedMessage}")
        }
    }

    @Internal
    override fun serialize(input: Identifier): ValidationResult<TomlElement> {
        return ValidationResult.success(TomlLiteral(input.toString()))
    }



    @Internal
    override fun correctEntry(input: Identifier, type: EntryValidator.ValidationType): ValidationResult<Identifier> {
        val result = validator.validateEntry(input, type)
        return if(result.isError()) {
            ValidationResult.error(storedValue, "Invalid identifier [$input] found, corrected to [$storedValue]: ${result.getError()}")} else result
    }

    @Internal
    override fun validateEntry(input: Identifier, type: EntryValidator.ValidationType): ValidationResult<Identifier> {
        return validator.validateEntry(input, type)
    }

    /**
     * creates a deep copy of this ValidatedIdentifier
     * @return ValidatedIdentifier wrapping a deep copy of the currently stored identifier, as well as this validations validator
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    override fun instanceEntry(): ValidatedIdentifier {
        return ValidatedIdentifier(copyStoredValue(), allowableIds, validator)
    }

    @Internal
    override fun isValidEntry(input: Any?): Boolean {
        return input is Identifier && validateEntry(input, EntryValidator.ValidationType.STRONG).isValid()
    }

    /**
     * Copies the provided input as deeply as possible. For immutables like numbers and booleans, this will simply return the input
     * @param input Identifier input to be copied
     * @return copied output
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    override fun copyValue(input: Identifier): Identifier {
        return input.toString().simpleId()
    }

    @Internal
    //client
    override fun widgetEntry(choicePredicate: ChoiceValidator<Identifier>): ClickableWidget {
        return OnClickTextFieldWidget({ this.get().toString() }, { it, isKb, key, code, mods ->
            val textField = PopupIdentifierTextFieldWidget(170, 20, choicePredicate, this)
            val popup = PopupWidget.Builder(this.translation())
                .add("text_field", textField, LayoutWidget.Position.BELOW)
                .addDoneWidget({ textField.pushChanges(); PopupWidget.pop() })
                .positionX { _, _ -> it.x - 8 }
                .positionY { _, h -> it.y + 28 + 24 - h }
                .build()
            PopupWidget.push(popup)
            PopupWidget.focusElement(textField)
            if (isKb)
                textField.keyPressed(key, code, mods)
        })
    }

    //////////// IDENTIFIER ///////////////////

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

    //////// END IDENTIFIER ///////////////////

    /**
     * @suppress
     */
    override fun toString(): String {
        return storedValue.toString()
    }

    companion object {

        private val keyFilterOffsets: MutableMap<RegistryKey<out Registry<*>>, AtomicInteger> = mutableMapOf()
        private val dynamicRegistrySyncsNeeded: MutableSet<RegistryKey<out Registry<*>>> = mutableSetOf()
        private val filteredDynamicRegistrySyncsNeeded: MutableSet<Triple<RegistryKey<out Registry<*>>, Identifier, Predicate<RegistryEntry<*>>>> = mutableSetOf()

        private fun getOffset(key: RegistryKey<out Registry<*>>): Int {
            return keyFilterOffsets.computeIfAbsent(key) { _ -> AtomicInteger() }.getAndIncrement()
        }

        internal fun createSyncs(manager: RegistryWrapper.WrapperLookup): List<DynamicIdsS2CCustomPayload> {
            return dynamicRegistrySyncsNeeded.mapNotNull { regKey ->
                manager.anyOptional(regKey).getOrNull()?.let {
                    impl -> DynamicIdsS2CCustomPayload(regKey.value, impl.streamKeys().map { key -> key.value }.toList().also { list -> dynamicIds[regKey.value] = list })
                }
            } + filteredDynamicRegistrySyncsNeeded.mapNotNull { (regKey, id, filter) ->
                manager.anyOptional(regKey).getOrNull()?.let {
                        impl -> DynamicIdsS2CCustomPayload(id, impl.streamEntries().filter(filter).map { it.registryKey() }.map { key -> key.value }.toList().also { list -> dynamicIds[regKey.value] = list })
                }
            }
        }

        internal fun createSpSyncs(manager: RegistryWrapper.WrapperLookup) {
            dynamicRegistrySyncsNeeded.mapNotNull { regKey ->
                manager.anyOptional(regKey).getOrNull()?.let {
                        impl -> impl.streamKeys().map { key -> key.value }.toList().also { list -> dynamicIds[regKey.value] = list }
                }
            }
            filteredDynamicRegistrySyncsNeeded.mapNotNull { (regKey, id, filter) ->
                manager.anyOptional(regKey).getOrNull()?.let {
                        impl -> impl.streamEntries().filter(filter).map { it.registryKey() }.map { key -> key.value }.toList().also { list -> dynamicIds[id] = list }
                }
            }
        }

        internal fun receiveSync(payload: DynamicIdsS2CCustomPayload) {
            dynamicIds[payload.key] = payload.ids
        }

        private val dynamicIds: MutableMap<Identifier, List< Identifier>> = mutableMapOf()

        @JvmStatic
        val DEFAULT_WEAK: EntryValidator<Identifier> = EntryValidator { i, _ -> ValidationResult.success(i) }

        /**
         * builds a String EntryValidator with default behavior
         *
         * Use if your identifier list may not be available at load-time (during modInitialization, typically), but will be available during updating (in-game). Lists from a Tag or Registry are easy examples, as the registry may not be fully populated yet, and the tag may not be loaded.
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
         * Use if your identifier list is available both at loading (during modInitialization, typically), and during updating (in-game).
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
         *
         * Allowable identifiers in this validation will NOT be cached. Tag contents can change over time in a game thanks to reloads.
         * @param defaultValue the default value of the ValidatedIdentifier
         * @param tag the tag of allowable values to choose from
         * @return [ValidatedIdentifier] wrapping the provided default and tag
         * @author fzzyhmstrs
         * @since 0.2.0, added non-caching 0.5.0
         */
        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        fun <T> ofTag(defaultValue: Identifier, tag: TagKey<T>): ValidatedIdentifier {
            val maybeRegistry = Registries.REGISTRIES.optional(tag.regRefId())
            if (maybeRegistry.isEmpty) return ValidatedIdentifier(defaultValue, AllowableIdentifiers({ false }, { emptyList() }))
            val registry = maybeRegistry.get() as? Registry<T> ?: return ValidatedIdentifier(defaultValue, AllowableIdentifiers({ false }, { emptyList() }))
            val supplier = Supplier { registry.iterateEntries(tag).mapNotNull { registry.getId(it.value()) } }
            return ValidatedIdentifier(defaultValue, AllowableIdentifiers({ id -> supplier.get().contains(id) }, supplier, false))
        }

        /**
         * Builds a ValidatedIdentifier based on an allowable tag of values
         *
         * Uses "minecraft:air" as the default value.
         *
         * Allowable identifiers in this validation will NOT be cached. Tag contents can change over time in a game thanks to reloads.
         * @param tag the tag of allowable values to choose from
         * @return [ValidatedIdentifier] wrapping the provided tag
         * @author fzzyhmstrs
         * @since 0.2.0, added non-caching 0.5.0
         */
        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        @Deprecated("Only use for validation in a list or map")
        fun <T> ofTag(tag: TagKey<T>): ValidatedIdentifier {
            val maybeRegistry = Registries.REGISTRIES.optional(tag.regRefId())
            if (maybeRegistry.isEmpty) return ValidatedIdentifier("minecraft:air".simpleId(), AllowableIdentifiers({ false }, { emptyList() }))
            val registry = maybeRegistry.get() as? Registry<T> ?: return ValidatedIdentifier("minecraft:air".simpleId(), AllowableIdentifiers({ false }, { emptyList() }))
            val supplier = Supplier { registry.iterateEntries(tag).mapNotNull { registry.getId(it.value()) } }
            return ValidatedIdentifier("minecraft:air".simpleId(), AllowableIdentifiers( { id -> supplier.get().contains(id) }, supplier, false))
        }

        /**
         * Builds a ValidatedIdentifier based on an allowable registry of values
         *
         * Allowable identifiers in this validation will be cached after their first polling. This is typically when suggestions are generated in a screen. Static registries like the ones passed into this method do not change while in game, so caching is beneficial with no downside.
         * @param defaultValue the default value of the ValidatedIdentifier
         * @param registry the registry whose ids are valid for this identifier
         * @return [ValidatedIdentifier] wrapping the provided default and registry
         * @author fzzyhmstrs
         * @since 0.2.0, added caching 0.5.0
         */
        @JvmStatic
        fun <T> ofRegistry(defaultValue: Identifier, registry: Registry<T>): ValidatedIdentifier {
            return ValidatedIdentifier(defaultValue, AllowableIdentifiers({ id -> registry.containsId(id) }, { registry.ids.toList() }, true))
        }

        /**
         * Builds a ValidatedIdentifier based on an allowable registry of values, filtered by the provided predicate
         *
         * Allowable identifiers in this validation will be cached after their first polling. This is typically when suggestions are generated in a screen. Static registries like the ones passed into this method do not change while in game, so caching is beneficial with no downside.
         * @param defaultValue the default value of the ValidatedIdentifier
         * @param registry the registry whose ids are valid for this identifier
         * @param predicate Predicate<RegistryEntry> tests an allowable subset of the registry
         * @return [ValidatedIdentifier] wrapping the provided default and predicated registry
         * @author fzzyhmstrs
         * @since 0.2.0, added caching 0.5.0
         */
        @JvmStatic
        fun <T> ofRegistry(defaultValue: Identifier, registry: Registry<T>, predicate: Predicate<RegistryEntry<T>>): ValidatedIdentifier {
            return ValidatedIdentifier(defaultValue,
                AllowableIdentifiers(
                    { id -> registry.containsId(id) && predicate.test ((registry.getEntry(id).takeIf { it.isPresent } ?: return@AllowableIdentifiers false).get()) },
                    { registry.ids.filter { id -> predicate.test ((registry.getEntry(id).takeIf { it.isPresent } ?: return@filter false).get()) } },
                    true
                )
            )
        }

        /**
         * Builds a ValidatedIdentifier based on an allowable registry of values, filtered by the provided predicate
         *
         * Allowable identifiers in this validation will be cached after their first polling. This is typically when suggestions are generated in a screen. Static registries like the ones passed into this method do not change while in game, so caching is beneficial with no downside.
         * @param defaultValue the default value of the ValidatedIdentifier
         * @param registry the registry whose ids are valid for this identifier
         * @param predicate Predicate<RegistryEntry> tests an allowable subset of the registry
         * @return [ValidatedIdentifier] wrapping the provided default and predicated registry
         * @author fzzyhmstrs
         * @since 0.5.0
         */
        @JvmStatic
        fun <T> ofRegistry(defaultValue: Identifier, registry: Registry<T>, predicate: BiPredicate<Identifier, RegistryEntry<T>>): ValidatedIdentifier {
            return ValidatedIdentifier(defaultValue,
                AllowableIdentifiers(
                    { id -> registry.containsId(id) && predicate.test (id, (registry.getEntry(id).takeIf { it.isPresent } ?: return@AllowableIdentifiers false).get()) },
                    { registry.ids.filter { id -> predicate.test (id, (registry.getEntry(id).takeIf { it.isPresent } ?: return@filter false).get()) } },
                    true
                )
            )
        }

        /**
         * Builds a ValidatedIdentifier based on an allowable registry of values
         *
         * Uses "minecraft:air" as the default value
         *
         * Allowable identifiers in this validation will be cached after their first polling. This is typically when suggestions are generated in a screen. Static registries like the ones passed into this method do not change while in game, so caching is beneficial with no downside.
         * @param registry the registry whose ids are valid for this identifier
         * @return [ValidatedIdentifier] wrapping the provided registry
         * @author fzzyhmstrs
         * @since 0.2.0, added caching 0.5.0
         */
        @JvmStatic
        @Deprecated("Only use for validation in a list or map")
        fun <T> ofRegistry(registry: Registry<T>): ValidatedIdentifier {
            return ValidatedIdentifier("minecraft:air".simpleId(), AllowableIdentifiers({ id -> registry.containsId(id) }, { registry.ids.toList() }, true))
        }

        /**
         * Builds a ValidatedIdentifier based on an allowable registry of values, filtered by the provided predicate
         *
         * Uses "minecraft:air" as the default value
         *
         * Allowable identifiers in this validation will be cached after their first polling. This is typically when suggestions are generated in a screen. Static registries like the ones passed into this method do not change while in game, so caching is beneficial with no downside.
         * @param registry the registry whose ids are valid for this identifier
         * @param predicate [Predicate]<RegistryEntry> tests an allowable subset of the registry
         * @return [ValidatedIdentifier] wrapping the provided predicated registry
         * @author fzzyhmstrs
         * @since 0.2.0, added caching 0.5.0
         */
        @JvmStatic
        @Deprecated("Only use for validation in a list or map")
        fun <T> ofRegistry(registry: Registry<T>, predicate: Predicate<RegistryEntry<T>>): ValidatedIdentifier {
            return ValidatedIdentifier("minecraft:air".simpleId(),
                AllowableIdentifiers(
                    { id -> registry.containsId(id) && predicate.test((registry.getEntry(id).takeIf { it.isPresent } ?: return@AllowableIdentifiers false).get()) },
                    { registry.ids.filter { id -> predicate.test((registry.getEntry(id).takeIf { it.isPresent } ?: return@filter false).get()) } },
                    true
                )
            )
        }

        /**
         * Builds a ValidatedIdentifier based on an allowable registry of values, filtered by the provided predicate
         *
         * Uses "minecraft:air" as the default value
         *
         * Allowable identifiers in this validation will be cached after their first polling. This is typically when suggestions are generated in a screen. Static registries like the ones passed into this method do not change while in game, so caching is beneficial with no downside.
         * @param registry the registry whose ids are valid for this identifier
         * @param predicate [BiPredicate]&lt;Identifier, RegistryEntry&gt; tests an allowable subset of the registry
         * @return [ValidatedIdentifier] wrapping the provided predicated registry
         * @author fzzyhmstrs
         * @since 0.2.0, added caching 0.5.0
         */
        @JvmStatic
        @Deprecated("Only use for validation in a list or map")
        fun <T> ofRegistry(registry: Registry<T>, predicate: BiPredicate<Identifier, RegistryEntry<T>>): ValidatedIdentifier {
            return ValidatedIdentifier("minecraft:air".simpleId(),
                AllowableIdentifiers(
                    { id -> registry.containsId(id) && predicate.test (id, (registry.getEntry(id).takeIf { it.isPresent } ?: return@AllowableIdentifiers false).get()) },
                    { registry.ids.filter { id -> predicate.test (id, (registry.getEntry(id).takeIf { it.isPresent } ?: return@filter false).get()) } },
                    true
                )
            )
        }

        /**
         * Builds a ValidatedIdentifier based on an allowable registry of values, defined from a RegistryKey. This can find both static and dynamic registries
         *
         * Uses "minecraft:air" as the default value
         *
         * Allowable identifiers in this validation will be cached after their first polling if the registry is a static; dynamic registry validation will NOT be cached as those registries can change while in game.
         * @param defaultValue the default value of the ValidatedIdentifier
         * @param key [RegistryKey] for the registry whose ids are valid for this identifier
         * @return [ValidatedIdentifier] wrapping the provided registry
         * @throws IllegalStateException if the registry can't be found based on the provided key
         * @author fzzyhmstrs
         * @since 0.2.0, added dynamic registry lookup and caching 0.5.0
         */
        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        @Deprecated("Only use for validation in a list or map")
        fun <T> ofRegistryKey(defaultValue: Identifier, key: RegistryKey<out Registry<T>>): ValidatedIdentifier {
            val maybeRegistry = Registries.REGISTRIES.optional(key.value)

            if (maybeRegistry.isPresent) {
                val registry = maybeRegistry.get() as? Registry<T> ?: throw IllegalStateException("Couldn't find registry based on passed key")
                return ofRegistry(defaultValue, registry)
            } else {
                if (RegistryLoader.SYNCED_REGISTRIES.any { entry -> entry.key == key }) {
                    val maybeRegistry2 = ConfigApiImpl.getWrapperLookup().optional(key)
                    if (maybeRegistry2.isPresent) {
                        val registry2 = maybeRegistry2.get() as? RegistryWrapper.Impl<T> ?: throw IllegalStateException("Couldn't find registry based on passed key")
                        val predicate2: Predicate<Identifier> = Predicate { id -> registry2.streamKeys().anyMatch { key -> key.value == id } }
                        val supplier2: Supplier<List<Identifier>> = Supplier { registry2.streamKeys().map { it.value }.toList() }
                        return ValidatedIdentifier(defaultValue, AllowableIdentifiers(predicate2, supplier2, false)).withFlag(EntryFlag.Flag.REQUIRES_WORLD)
                    }
                }
                dynamicRegistrySyncsNeeded.add(key)
                val predicate3: Predicate<Identifier> = Predicate { id -> dynamicIds[key.value]?.contains(id) == true}
                val supplier3: Supplier<List<Identifier>> = Supplier { dynamicIds[key.value] ?: emptyList() }
                return ValidatedIdentifier(defaultValue, AllowableIdentifiers(predicate3, supplier3, false)).withFlag(EntryFlag.Flag.REQUIRES_WORLD)
            }
        }

        /**
         * Builds a ValidatedIdentifier based on an allowable registry of values, defined from a RegistryKey
         * @param defaultValue the default value of the ValidatedIdentifier
         * @param key [RegistryKey] for the registry whose ids are valid for this identifier
         * @param predicate [Predicate]<RegistryEntry> tests an allowable subset of the registry
         * @return [ValidatedIdentifier] wrapping the provided registry
         * @author fzzyhmstrs
         * @since 0.2.0, added dynamic registry lookup and caching 0.5.0
         */
        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        fun <T> ofRegistryKey(defaultValue: Identifier, key: RegistryKey<out Registry<T>>, predicate: Predicate<RegistryEntry<T>>): ValidatedIdentifier {
            val maybeRegistry = Registries.REGISTRIES.optional(key.value)
            if (maybeRegistry.isPresent) {
                val registry = maybeRegistry.get() as? Registry<T> ?: throw IllegalStateException("Couldn't find registry based on passed key")
                return ofRegistry(defaultValue, registry, predicate)
            } else {
                if (RegistryLoader.SYNCED_REGISTRIES.any { entry -> entry.key == key }) {
                    val maybeRegistry2 = ConfigApiImpl.getWrapperLookup().optional(key)
                    if (maybeRegistry2.isPresent) {
                        val registry2 = maybeRegistry2.get() as? RegistryWrapper.Impl<T> ?: throw IllegalStateException("Couldn't find registry based on passed key")
                        val predicate2: Predicate<Identifier> = Predicate {
                                id -> registry2.streamKeys().anyMatch { key -> key.value == id } && predicate.test((registry2.getOptional(RegistryKey.of(key, id)).takeIf { it.isPresent } ?: return@Predicate false).get())
                        }
                        val supplier2: Supplier<List<Identifier>> = Supplier {
                            registry2.streamKeys().filter { key -> predicate.test((registry2.getOptional(key).takeIf { it.isPresent } ?: return@filter false).get()) }.map { it.value }.toList()
                        }
                        return ValidatedIdentifier(defaultValue, AllowableIdentifiers(predicate2, supplier2, false)).withFlag(EntryFlag.Flag.REQUIRES_WORLD)
                    }
                }
                FC.LOGGER.warn("Method ofRegistryKey with Predicate is deprecated for registry $key; use ofDynamicKey instead")
                val predicateId = key.value.withSuffixedPath(getOffset(key).toString())
                filteredDynamicRegistrySyncsNeeded.add(Triple((key as RegistryKey<Registry<*>>), predicateId, (predicate as Predicate<RegistryEntry<*>>)))
                val predicate3: Predicate<Identifier> = Predicate { id -> dynamicIds[predicateId]?.contains(id) == true }
                val supplier3: Supplier<List<Identifier>> = Supplier { dynamicIds[predicateId] ?: emptyList() }
                return ValidatedIdentifier(defaultValue, AllowableIdentifiers(predicate3, supplier3, false)).withFlag(EntryFlag.Flag.REQUIRES_WORLD)
            }
        }

        /**
         * Builds a ValidatedIdentifier based on an allowable registry of values, defined from a RegistryKey
         * @param defaultValue the default value of the ValidatedIdentifier
         * @param key [RegistryKey] for the registry whose ids are valid for this identifier
         * @param predicate [Predicate]<RegistryEntry> tests an allowable subset of the registry
         * @return [ValidatedIdentifier] wrapping the provided registry
         * @author fzzyhmstrs
         * @since 0.5.0
         */
        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        fun <T> ofRegistryKey(defaultValue: Identifier, key: RegistryKey<out Registry<T>>, predicate: BiPredicate<Identifier, RegistryEntry<T>>): ValidatedIdentifier {
            val maybeRegistry = Registries.REGISTRIES.optional(key.value)
            if (maybeRegistry.isPresent) {
                val registry = maybeRegistry.get() as? Registry<T> ?: throw IllegalStateException("Couldn't find registry based on passed key")
                return ofRegistry(defaultValue, registry, predicate)
            } else {
                if (RegistryLoader.SYNCED_REGISTRIES.any { entry -> entry.key == key }) {
                    val maybeRegistry2 = ConfigApiImpl.getWrapperLookup().optional(key)
                    if (maybeRegistry2.isPresent) {
                        val registry2 = maybeRegistry2.get() as? RegistryWrapper.Impl<T> ?: throw IllegalStateException("Couldn't find registry based on passed key")
                        val predicate2: Predicate<Identifier> = Predicate { id ->
                            registry2.streamKeys().anyMatch { key -> key.value == id } && predicate.test(
                                id,
                                (registry2.getOptional(RegistryKey.of(key, id)).takeIf { it.isPresent } ?: return@Predicate false).get())
                        }
                        val supplier2: Supplier<List<Identifier>> = Supplier { registry2.streamKeys().map { it.value }.toList() }
                        return ValidatedIdentifier(defaultValue, AllowableIdentifiers(predicate2, supplier2, false)).withFlag(EntryFlag.Flag.REQUIRES_WORLD)
                    }
                }
                FC.LOGGER.warn("Method ofRegistryKey with BiPredicate is deprecated for registry $key; use ofDynamicKey instead")
                val predicateId = key.value.withSuffixedPath(getOffset(key).toString())
                filteredDynamicRegistrySyncsNeeded.add(Triple((key as RegistryKey<Registry<*>>), predicateId, (Predicate { re: RegistryEntry<T> -> predicate.test(re.key?.map { it.value }?.orElse("minecraft:air".simpleId()) ?: "minecraft:air".simpleId(), re) } as Predicate<RegistryEntry<*>>)))
                val predicate3: Predicate<Identifier> = Predicate { id -> dynamicIds[predicateId]?.contains(id) == true }
                val supplier3: Supplier<List<Identifier>> = Supplier { dynamicIds[predicateId] ?: emptyList() }
                return ValidatedIdentifier(defaultValue, AllowableIdentifiers(predicate3, supplier3, false)).withFlag(EntryFlag.Flag.REQUIRES_WORLD)
            }
        }

        /**
         * Builds a ValidatedIdentifier based on an allowable registry of values, defined from a RegistryKey
         *
         * Uses "minecraft:air" as the default value
         * @param key [RegistryKey] for the registry whose ids are valid for this identifier
         * @return [ValidatedIdentifier] wrapping the provided registry
         * @author fzzyhmstrs
         * @since 0.2.0, added dynamic registry lookup and caching 0.5.0
         */
        @JvmStatic
        @Suppress("DeprecatedCallableAddReplaceWith", "DEPRECATION")
        @Deprecated("Only use for validation in a list or map")
        fun <T> ofRegistryKey(key: RegistryKey<out Registry<T>>): ValidatedIdentifier {
            return ofRegistryKey("minecraft:air".simpleId(), key)
        }

        /**
         * Builds a ValidatedIdentifier based on an allowable registry of values, defined from a RegistryKey
         *
         * Uses "minecraft:air" as the default value
         * @param key [RegistryKey] for the registry whose ids are valid for this identifier
         * @param predicate [BiPredicate]<RegistryEntry> tests an allowable subset of the registry
         * @return [ValidatedIdentifier] wrapping the provided registry
         * @author fzzyhmstrs
         * @since 0.2.0, added dynamic registry lookup and caching 0.5.0
         */
        @JvmStatic
        @Suppress("UNCHECKED_CAST", "DEPRECATION")
        fun <T> ofRegistryKey(key: RegistryKey<out Registry<T>>, predicate: BiPredicate<Identifier, RegistryEntry<T>>): ValidatedIdentifier {
            val maybeRegistry = Registries.REGISTRIES.optional(key.value)
            if (maybeRegistry.isPresent) {
                val registry = maybeRegistry.get() as? Registry<T> ?: throw IllegalStateException("Couldn't find registry based on passed key")
                return ofRegistry(registry, predicate)
            } else {
                if (RegistryLoader.SYNCED_REGISTRIES.any { entry -> entry.key == key }) {
                    val maybeRegistry2 = ConfigApiImpl.getWrapperLookup().optional(key)
                    if (maybeRegistry2.isPresent) {
                        val registry2 = maybeRegistry2.get() as? RegistryWrapper.Impl<T> ?: throw IllegalStateException("Couldn't find registry based on passed key")
                        val predicate2: Predicate<Identifier> = Predicate { id ->
                            registry2.streamKeys().anyMatch { key -> key.value == id } && predicate.test(
                                id,
                                (registry2.getOptional(RegistryKey.of(key, id)).takeIf { it.isPresent } ?: return@Predicate false).get())
                        }
                        val supplier2: Supplier<List<Identifier>> = Supplier { registry2.streamKeys().map { it.value }.toList() }
                        return ValidatedIdentifier("minecraft:air".simpleId(), AllowableIdentifiers(predicate2, supplier2, false)).withFlag(EntryFlag.Flag.REQUIRES_WORLD)
                    }
                }
                FC.LOGGER.warn("Method ofRegistryKey is deprecated for registry $key; use ofDynamicKey instead")
                val predicateId = key.value.withSuffixedPath(getOffset(key).toString())
                filteredDynamicRegistrySyncsNeeded.add(Triple((key as RegistryKey<Registry<*>>), predicateId, (Predicate { re: RegistryEntry<T> -> predicate.test(re.key?.map { it.value }?.orElse("minecraft:air".simpleId()) ?: "minecraft:air".simpleId(), re) } as Predicate<RegistryEntry<*>>)))
                val predicate3: Predicate<Identifier> = Predicate { id -> dynamicIds[predicateId]?.contains(id) == true }
                val supplier3: Supplier<List<Identifier>> = Supplier { dynamicIds[predicateId] ?: emptyList() }
                return ValidatedIdentifier("minecraft:air".simpleId(), AllowableIdentifiers(predicate3, supplier3, false)).withFlag(EntryFlag.Flag.REQUIRES_WORLD)
            }
        }

        /**
         * Builds a ValidatedIdentifier based on an allowable registry of values, defined from a RegistryKey
         *
         * Used primarily for dynamic registries that aren't synced to clients (Loot registries, primarily)
         * @param defaultValue the default value of the ValidatedIdentifier
         * @param key [RegistryKey] for the registry whose ids are valid for this identifier
         * @param predicateId String unique id for the predicate provided; used to properly sync ids that this predicate cares about
         * @param predicate [Predicate]<RegistryEntry> tests an allowable subset of the registry
         * @return [ValidatedIdentifier] wrapping the provided registry
         * @author fzzyhmstrs
         * @since 0.5.6
         */
        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        fun <T> ofDynamicKey(defaultValue: Identifier, key: RegistryKey<out Registry<T>>, predicateId: String, predicate: Predicate<RegistryEntry<T>>): ValidatedIdentifier {
            val maybeRegistry = Registries.REGISTRIES.optional(key.value)
            if (maybeRegistry.isPresent) {
                val registry = maybeRegistry.get() as? Registry<T> ?: throw IllegalStateException("Couldn't find registry based on passed key")
                return ofRegistry(defaultValue, registry, predicate)
            } else {
                if (RegistryLoader.SYNCED_REGISTRIES.any { entry -> entry.key == key }) {
                    val maybeRegistry2 = ConfigApiImpl.getWrapperLookup().optional(key)
                    if (maybeRegistry2.isPresent) {
                        val registry2 = maybeRegistry2.get() as? RegistryWrapper.Impl<T> ?: throw IllegalStateException("Couldn't find registry based on passed key")
                        val predicate2: Predicate<Identifier> = Predicate {
                                id -> registry2.streamKeys().anyMatch { key -> key.value == id } && predicate.test((registry2.getOptional(RegistryKey.of(key, id)).takeIf { it.isPresent } ?: return@Predicate false).get())
                        }
                        val supplier2: Supplier<List<Identifier>> = Supplier {
                            registry2.streamKeys().filter { key -> predicate.test((registry2.getOptional(key).takeIf { it.isPresent } ?: return@filter false).get()) }.map { it.value }.toList()
                        }
                        return ValidatedIdentifier(defaultValue, AllowableIdentifiers(predicate2, supplier2, false)).withFlag(EntryFlag.Flag.REQUIRES_WORLD)
                    }
                }
                val pId = key.value.withSuffixedPath(predicateId)
                filteredDynamicRegistrySyncsNeeded.add(Triple((key as RegistryKey<Registry<*>>), pId, (predicate as Predicate<RegistryEntry<*>>)))
                val predicate3: Predicate<Identifier> = Predicate { id -> dynamicIds[pId]?.contains(id) == true }
                val supplier3: Supplier<List<Identifier>> = Supplier { dynamicIds[pId] ?: emptyList() }
                return ValidatedIdentifier(defaultValue, AllowableIdentifiers(predicate3, supplier3, false)).withFlag(EntryFlag.Flag.REQUIRES_WORLD)
            }
        }

        /**
         * Builds a ValidatedIdentifier based on an allowable registry of values, defined from a RegistryKey
         *
         * Used primarily for dynamic registries that aren't synced to clients (Loot registries, primarily)
         * @param defaultValue the default value of the ValidatedIdentifier
         * @param key [RegistryKey] for the registry whose ids are valid for this identifier
         * @param predicateId String unique id for the predicate provided; used to properly sync ids that this predicate cares about
         * @param predicate [Predicate]<RegistryEntry> tests an allowable subset of the registry
         * @return [ValidatedIdentifier] wrapping the provided registry
         * @author fzzyhmstrs
         * @since 0.5.6
         */
        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        fun <T> ofDynamicKey(defaultValue: Identifier, key: RegistryKey<out Registry<T>>, predicateId: String, predicate: BiPredicate<Identifier, RegistryEntry<T>>): ValidatedIdentifier {
            val maybeRegistry = Registries.REGISTRIES.optional(key.value)
            if (maybeRegistry.isPresent) {
                val registry = maybeRegistry.get() as? Registry<T> ?: throw IllegalStateException("Couldn't find registry based on passed key")
                return ofRegistry(defaultValue, registry, predicate)
            } else {
                if (RegistryLoader.SYNCED_REGISTRIES.any { entry -> entry.key == key }) {
                    val maybeRegistry2 = ConfigApiImpl.getWrapperLookup().optional(key)
                    if (maybeRegistry2.isPresent) {
                        val registry2 = maybeRegistry2.get() as? RegistryWrapper.Impl<T> ?: throw IllegalStateException("Couldn't find registry based on passed key")
                        val predicate2: Predicate<Identifier> = Predicate { id ->
                            registry2.streamKeys().anyMatch { key -> key.value == id } && predicate.test(
                                id,
                                (registry2.getOptional(RegistryKey.of(key, id)).takeIf { it.isPresent } ?: return@Predicate false).get())
                        }
                        val supplier2: Supplier<List<Identifier>> = Supplier { registry2.streamKeys().map { it.value }.toList() }
                        return ValidatedIdentifier(defaultValue, AllowableIdentifiers(predicate2, supplier2, false)).withFlag(EntryFlag.Flag.REQUIRES_WORLD)
                    }
                }
                val pId = key.value.withSuffixedPath(predicateId)
                filteredDynamicRegistrySyncsNeeded.add(Triple((key as RegistryKey<Registry<*>>), pId, (Predicate { re: RegistryEntry<T> -> predicate.test(re.key?.map { it.value }?.orElse("minecraft:air".simpleId()) ?: "minecraft:air".simpleId(), re) } as Predicate<RegistryEntry<*>>)))
                val predicate3: Predicate<Identifier> = Predicate { id -> dynamicIds[pId]?.contains(id) == true }
                val supplier3: Supplier<List<Identifier>> = Supplier { dynamicIds[pId] ?: emptyList() }
                return ValidatedIdentifier(defaultValue, AllowableIdentifiers(predicate3, supplier3, false)).withFlag(EntryFlag.Flag.REQUIRES_WORLD)
            }
        }

        /**
         * Builds a ValidatedIdentifier based on an allowable registry of values, defined from a RegistryKey
         *
         * Used primarily for dynamic registries that aren't synced to clients (Loot registries, primarily)
         *
         * Uses "minecraft:air" as the default value
         * @param key [RegistryKey] for the registry whose ids are valid for this identifier
         * @param predicateId String unique id for the predicate provided; used to properly sync ids that this predicate cares about
         * @param predicate [BiPredicate]<RegistryEntry> tests an allowable subset of the registry
         * @return [ValidatedIdentifier] wrapping the provided registry
         * @author fzzyhmstrs
         * @since 0.5.6
         */
        @JvmStatic
        @Suppress("UNCHECKED_CAST", "DEPRECATION")
        fun <T> ofDynamicKey(key: RegistryKey<out Registry<T>>, predicateId: String, predicate: BiPredicate<Identifier, RegistryEntry<T>>): ValidatedIdentifier {
            val maybeRegistry = Registries.REGISTRIES.optional(key.value)
            if (maybeRegistry.isPresent) {
                val registry = maybeRegistry.get() as? Registry<T> ?: throw IllegalStateException("Couldn't find registry based on passed key")
                return ofRegistry(registry, predicate)
            } else {
                if (RegistryLoader.SYNCED_REGISTRIES.any { entry -> entry.key == key }) {
                    val maybeRegistry2 = ConfigApiImpl.getWrapperLookup().optional(key)
                    if (maybeRegistry2.isPresent) {
                        val registry2 = maybeRegistry2.get() as? RegistryWrapper.Impl<T> ?: throw IllegalStateException("Couldn't find registry based on passed key")
                        val predicate2: Predicate<Identifier> = Predicate { id ->
                            registry2.streamKeys().anyMatch { key -> key.value == id } && predicate.test(
                                id,
                                (registry2.getOptional(RegistryKey.of(key, id)).takeIf { it.isPresent } ?: return@Predicate false).get())
                        }
                        val supplier2: Supplier<List<Identifier>> = Supplier { registry2.streamKeys().map { it.value }.toList() }
                        return ValidatedIdentifier("minecraft:air".simpleId(), AllowableIdentifiers(predicate2, supplier2, false)).withFlag(EntryFlag.Flag.REQUIRES_WORLD)
                    }
                }
                val pId = key.value.withSuffixedPath(predicateId)
                filteredDynamicRegistrySyncsNeeded.add(Triple((key as RegistryKey<Registry<*>>), pId, (Predicate { re: RegistryEntry<T> -> predicate.test(re.key?.map { it.value }?.orElse("minecraft:air".simpleId()) ?: "minecraft:air".simpleId(), re) } as Predicate<RegistryEntry<*>>)))
                val predicate3: Predicate<Identifier> = Predicate { id -> dynamicIds[pId]?.contains(id) == true }
                val supplier3: Supplier<List<Identifier>> = Supplier { dynamicIds[pId] ?: emptyList() }
                return ValidatedIdentifier("minecraft:air".simpleId(), AllowableIdentifiers(predicate3, supplier3, false)).withFlag(EntryFlag.Flag.REQUIRES_WORLD)
            }
        }

        /**
         * Builds a ValidatedIdentifier based on the existing [TagKey] stream from the registry defined by the supplied RegistryKey
         *
         * Uses "c:dummy" as the default TagKey id
         * @param T the TagKey type
         * @param key [RegistryKey] for the registry whose ids are valid for this identifier
         * @return [ValidatedIdentifier] wrapping the TagKeys of the provided registry
         * @author fzzyhmstrs
         * @since 0.2.0, added dynamic registry lookup and caching 0.5.0
         */
        fun <T: Any> ofRegistryTags(key: RegistryKey<out Registry<T>>): ValidatedIdentifier {
            val maybeRegistry = Registries.REGISTRIES.optional(key.value)
            if (maybeRegistry.isPresent) {
                //memoize to cache the predicate test get also
                val supplier: Supplier<List<Identifier>> = Suppliers.memoize { maybeRegistry.get().tagIdList() }
                val ids = AllowableIdentifiers({ id -> supplier.get().contains(id) }, supplier, false)
                return ValidatedIdentifier("c:dummy".simpleId(), ids)
            } else {
                val maybeRegistry2 = ConfigApiImpl.getWrapperLookup().optional(key)
                if (maybeRegistry2.isEmpty) throw IllegalStateException("Couldn't find registry based on passed key")
                //no memoization, this registry is dynamic
                val supplier: Supplier<List<Identifier>> = Supplier { maybeRegistry2.get().tagIdList() }
                val ids = AllowableIdentifiers({ id -> supplier.get().contains(id) }, supplier, false)
                return ValidatedIdentifier("c:dummy".simpleId(), ids)
            }
        }

        /**
         * Builds a ValidatedIdentifier based on the existing [TagKey] stream from the registry defined by the supplied RegistryKey, and predicated by the provided predicate
         *
         * Uses "c:dummy" as the default TagKey id
         * @param T the TagKey type
         * @param key [RegistryKey] for the registry whose ids are valid for this identifier
         * @param predicate [Predicate]<Identifier> tests an allowable subset of the TagKeys
         * @return [ValidatedIdentifier] wrapping the TagKeys of the provided registry
         * @author fzzyhmstrs
         * @since 0.2.0, added dynamic registry lookup and caching 0.5.0
         */
        fun <T: Any> ofRegistryTags(key: RegistryKey<out Registry<T>>, predicate: Predicate<Identifier>): ValidatedIdentifier {
            val maybeRegistry = Registries.REGISTRIES.optional(key.value)
            if (maybeRegistry.isPresent) {
                //memoize to cache the predicate test get also
                val supplier: Supplier<List<Identifier>> = Suppliers.memoize { maybeRegistry.get().tagIdList(predicate) }
                val ids = AllowableIdentifiers({ id -> supplier.get().contains(id) }, supplier, false)
                return ValidatedIdentifier("c:dummy".simpleId(), ids)
            } else {
                val maybeRegistry2 = ConfigApiImpl.getWrapperLookup().optional(key)
                if (maybeRegistry2.isEmpty) throw IllegalStateException("Couldn't find registry based on passed key")
                //no memoization, this registry is dynamic
                val supplier: Supplier<List<Identifier>> = Supplier { maybeRegistry2.get().tagIdList(predicate) }
                val ids = AllowableIdentifiers({ id -> supplier.get().contains(id) }, supplier, false)
                return ValidatedIdentifier("c:dummy".simpleId(), ids)
            }
        }

        /**
         * Builds a ValidatedIdentifier based on the existing [TagKey] stream from the registry defined by the supplied RegistryKey
         * @param T the TagKey type
         * @param default [TagKey] the default TagKey value to get an identifier from
         * @param key [RegistryKey] for the registry whose ids are valid for this identifier
         * @return [ValidatedIdentifier] wrapping the TagKeys of the provided registry
         * @author fzzyhmstrs
         * @since 0.2.0, added dynamic registry lookup and caching 0.5.0
         */
        fun <T: Any> ofRegistryTags(default: TagKey<T>, key: RegistryKey<out Registry<T>>): ValidatedIdentifier {
            val maybeRegistry = Registries.REGISTRIES.optional(key.value)
            if (maybeRegistry.isPresent) {
                //memoize to cache the predicate test get also
                val supplier: Supplier<List<Identifier>> = Suppliers.memoize { maybeRegistry.get().tagIdList() }
                val ids = AllowableIdentifiers({ id -> supplier.get().contains(id) }, supplier, false)
                return ValidatedIdentifier(default.id, ids)
            } else {
                val maybeRegistry2 = ConfigApiImpl.getWrapperLookup().optional(key)
                if (maybeRegistry2.isEmpty) throw IllegalStateException("Couldn't find registry based on passed key")
                //no memoization, this registry is dynamic
                val supplier: Supplier<List<Identifier>> = Supplier { maybeRegistry2.get().tagIdList() }
                val ids = AllowableIdentifiers({ id -> supplier.get().contains(id) }, supplier, false)
                return ValidatedIdentifier(default.id, ids)
            }
        }

        /**
         * Builds a ValidatedIdentifier based on the existing [TagKey] stream from the registry defined by the supplied RegistryKey, and predicated by the provided predicate
         * @param T the TagKey type
         * @param default [TagKey] the default TagKey value to get an identifier from
         * @param key [RegistryKey] for the registry whose ids are valid for this identifier
         * @param predicate [Predicate]<Identifier> tests an allowable subset of the TagKeys
         * @return [ValidatedIdentifier] wrapping the TagKeys of the provided registry
         * @author fzzyhmstrs
         * @since 0.2.0, added dynamic registry lookup and caching 0.5.0
         */
        fun <T: Any> ofRegistryTags(default: TagKey<T>, key: RegistryKey<out Registry<T>>, predicate: Predicate<Identifier>): ValidatedIdentifier {
            val maybeRegistry = Registries.REGISTRIES.optional(key.value)
            if (maybeRegistry.isPresent) {
                //memoize to cache the predicate test get also
                val supplier: Supplier<List<Identifier>> = Suppliers.memoize { maybeRegistry.get().tagIdList(predicate) }
                val ids = AllowableIdentifiers({ id -> supplier.get().contains(id) }, supplier, false)
                return ValidatedIdentifier(default.id, ids)
            } else {
                val maybeRegistry2 = ConfigApiImpl.getWrapperLookup().optional(key)
                if (maybeRegistry2.isEmpty) throw IllegalStateException("Couldn't find registry based on passed key")
                //no memoization, this registry is dynamic
                val supplier: Supplier<List<Identifier>> = Supplier { maybeRegistry2.get().tagIdList(predicate) }
                val ids = AllowableIdentifiers({ id -> supplier.get().contains(id) }, supplier, false)
                return ValidatedIdentifier(default.id, ids)
            }
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
        fun ofList(defaultValue: Identifier, list: List<Identifier>): ValidatedIdentifier {
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
        fun ofList(list: List<Identifier>): ValidatedIdentifier {
            val allowableIds = AllowableIdentifiers({ id -> list.contains(id) }, list.supply())
            val validator = strong(allowableIds)
            return ValidatedIdentifier("minecraft:air".simpleId(), allowableIds, validator)
        }

        /**
         * Builds a ValidatedIdentifier based on an allowable list of values
         *
         * This list does not have to be complete at validation time.
         * @param defaultValue the default value of the ValidatedIdentifier
         * @param listSupplier Supplier of the list whose entries are valid for this identifier
         * @return [ValidatedIdentifier] wrapping the provided default and list supplier
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofSuppliedList(defaultValue: Identifier, listSupplier: Supplier<List<Identifier>>): ValidatedIdentifier {
            val allowableIds = AllowableIdentifiers({ id -> listSupplier.get().contains(id) }, listSupplier)
            return ValidatedIdentifier(defaultValue, allowableIds)
        }

        /**
         * Builds a ValidatedIdentifier based on an allowable list of values
         *
         * This list does not have to be complete at validation time.
         *
         * uses "minecraft:air" as the default value
         * @param listSupplier Supplier of the list whose entries are valid for this identifier
         * @return [ValidatedIdentifier] wrapping the provided list supplier
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofSuppliedList(listSupplier: Supplier<List<Identifier>>): ValidatedIdentifier {
            val allowableIds = AllowableIdentifiers({ id -> listSupplier.get().contains(id) }, listSupplier)
            return ValidatedIdentifier("minecraft:air".simpleId(), allowableIds)
        }

        /**
         * wraps a list in a [Supplier]
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @Suppress("MemberVisibilityCanBePrivate")
        fun<T> List<T>.supply(): Supplier<List<T>> {
            return Supplier { this }
        }
    }

    //client
    private class PopupIdentifierTextFieldWidget(
        width: Int,
        height: Int,
        private val choiceValidator: ChoiceValidator<Identifier>,
        private val validatedIdentifier: ValidatedIdentifier)
        :
        TextFieldWidget(MinecraftClient.getInstance().textRenderer, 0, 0, width, height, FcText.EMPTY),
        SuggestionWindowProvider
    {

        private var cachedWrappedValue = validatedIdentifier.get()
        private var storedValue = validatedIdentifier.get()
        private var lastChangedTime: Long = 0L
        private var isValid = true
        private var pendingSuggestions: CompletableFuture<Suggestions>? = null
        private var lastSuggestionText = ""
        private var shownText = ""
        private var window: SuggestionWindow? = null
        private var closeWindow = false
        private var needsUpdating = false
        private var suggestionWindowListener: SuggestionWindowListener? = null

        override fun addListener(listener: SuggestionWindowListener) {
            this.suggestionWindowListener = listener
        }

        private fun isValidTest(s: String): Boolean {
            if (s != lastSuggestionText) {
                pendingSuggestions = validatedIdentifier.allowableIds.getSuggestions(s, this.cursor, choiceValidator)
                lastSuggestionText = s
            }
            val id = Identifier.tryParse(s)
            if (id == null || !s.contains(":")) {
                setEditableColor(Formatting.RED.colorValue ?: 0xFFFFFF)
                return false
            }
            return if (validatedIdentifier.validateEntry(id, EntryValidator.ValidationType.STRONG).isValid()) {
                val result = choiceValidator.validateEntry(id, EntryValidator.ValidationType.STRONG)
                if (result.isValid()) {
                    storedValue = result.get()
                    lastChangedTime = System.currentTimeMillis()
                    setEditableColor(0xFFFFFF)
                    true
                } else {
                    setEditableColor(Formatting.RED.colorValue ?: 0xFFFFFF)
                    false
                }
            } else {
                setEditableColor(Formatting.RED.colorValue ?: 0xFFFFFF)
                false
            }
        }

        override fun getInnerWidth(): Int {
            return super.getInnerWidth() - 11
        }

        private fun isChanged(): Boolean {
            return storedValue != validatedIdentifier.get()
        }

        private fun ongoingChanges(): Boolean {
            return System.currentTimeMillis() - lastChangedTime <= 350L
        }

        fun pushChanges() {
            if(isChanged() && !needsUpdating) {
                validatedIdentifier.accept(storedValue)
            }
        }

        override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
            val testValue = validatedIdentifier.get()
            if (cachedWrappedValue != testValue || needsUpdating) {
                needsUpdating = false
                this.storedValue = testValue
                this.cachedWrappedValue = testValue
                this.text = this.storedValue.toString()
            }
            if(isChanged()) {
                if (lastChangedTime != 0L && !ongoingChanges() && isValid)
                    validatedIdentifier.accept(storedValue)
            }
            super.renderWidget(context, mouseX, mouseY, delta)
            if(isValid) {
                if (ongoingChanges())
                    context.drawTex(TextureIds.ENTRY_ONGOING, x + width - 20, y, 20, 20)
                else
                    context.drawTex(TextureIds.ENTRY_OK, x + width - 20, y, 20, 20)
            } else {
                context.drawTex(TextureIds.ENTRY_ERROR, x + width - 20, y, 20, 20)
            }
            if (pendingSuggestions?.isDone == true) {
                val suggestions = pendingSuggestions?.get()
                if (suggestions != null && !suggestions.isEmpty && shownText != lastSuggestionText) {
                    shownText = lastSuggestionText
                    addSuggestionWindow(suggestions)
                }
            }
            window?.render(context, mouseX, mouseY, delta)
        }

        override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
            val bl = window?.mouseClicked(mouseX.toInt(), mouseY.toInt(), button) ?: super.mouseClicked(mouseX, mouseY, button)
            if (closeWindow) {
                pendingSuggestions = null
                window = null
                suggestionWindowListener?.setSuggestionWindowElement(null)
                closeWindow = false
            }
            return if(bl) true else super.mouseClicked(mouseX, mouseY, button)
        }

        override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
            return window?.mouseScrolled(mouseX.toInt(), mouseY.toInt(), verticalAmount) ?: super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
        }

        override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
            return super.isMouseOver(mouseX, mouseY) || window?.isMouseOver(mouseX.toInt(), mouseY.toInt()) == true
        }

        override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
            val bl = window?.keyPressed(keyCode, scanCode, modifiers) ?: super.keyPressed(keyCode, scanCode, modifiers)
            if (closeWindow) {
                pendingSuggestions = null
                window = null
                suggestionWindowListener?.setSuggestionWindowElement(null)
                closeWindow = false
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                pushChanges()
                PopupWidget.pop()
            }
            return if(bl) true else super.keyPressed(keyCode, scanCode, modifiers)
        }

        init {
            setMaxLength(1000)
            text = validatedIdentifier.get().toString()
            setChangedListener { s -> isValid = isValidTest(s) }
        }

        private fun addSuggestionWindow(suggestions: Suggestions) {
            val applier: Consumer<String> = Consumer { s ->
                try {
                    validatedIdentifier.accept(s.simpleId())
                    needsUpdating = true
                } catch (e: Throwable) {
                    //
                }
            }
            val closer: Consumer<SuggestionWindow> = Consumer { closeWindow = true }
            this.window = SuggestionWindow.createSuggestionWindow(this.x, this.y, suggestions, this.text, this.cursor, applier, closer)
            suggestionWindowListener?.setSuggestionWindowElement(this)
        }
    }
}