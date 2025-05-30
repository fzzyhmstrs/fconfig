/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.validation.collection

import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.entry.Entry
import me.fzzyhmstrs.fzzy_config.entry.EntryCreator
import me.fzzyhmstrs.fzzy_config.entry.EntryOpener
import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.screen.context.ContextAction
import me.fzzyhmstrs.fzzy_config.screen.context.ContextResultBuilder
import me.fzzyhmstrs.fzzy_config.screen.context.ContextType
import me.fzzyhmstrs.fzzy_config.screen.decoration.Decorated
import me.fzzyhmstrs.fzzy_config.screen.widget.*
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomButtonWidget
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.attachTo
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedMap.Builder
import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator
import net.minecraft.client.gui.widget.ClickableWidget
import net.peanuuutz.tomlkt.*
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.function.BiFunction
import java.util.function.Supplier
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

/**
 * A validated Map of arbitrary (Validated) keys and values
 *
 * NOTE: This construct is handled as an array of array pairs in TOML. It may not look like a traditional TOML map.
 *
 * [See the Wiki](https://moddedmc.wiki/en/project/fzzy-config/docs/config-concepts/validation/Collections) for more details and examples.
 * @param K Any non-null type with an associated [Entry] to handle key-related tasks
 * @param V Any non-null type with an associated [Entry] to handle value-related tasks
 * @param defaultValue Map<K, V> the default map for this validation
 * @param keyHandler [Entry] for handling keys
 * @param valueHandler [Entry] for handling values
 * @see [Builder] Using the builder is recommended for more clear construction
 * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedCollectionExamples.maps
 * @author fzzyhmstrs
 * @since 0.2.0
 */
open class ValidatedMap<K, V>(defaultValue: Map<K, V>, private val keyHandler: Entry<K, *>, private val valueHandler: Entry<V, *>): ValidatedField<Map<K, V>>(defaultValue), Map<K, V>, EntryOpener {

    init {
        for((key, value) in defaultValue) {
            if (keyHandler.validateEntry(key, EntryValidator.ValidationType.WEAK).isError())
                throw IllegalStateException("Default Map key [$key] not valid per keyHandler provided")
            if (valueHandler.validateEntry(value, EntryValidator.ValidationType.WEAK).isError())
                throw IllegalStateException("Default Map value [$value] not valid per valueHandler provided")
        }
        compositeFlags(keyHandler)
        compositeFlags(valueHandler)
    }


    //((?![a-z0-9_-]).) in case I need it...
    @Internal
    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<Map<K, V>> {
        return try {
            val table = toml.asTomlArray()
            val map: MutableMap<K, V> = mutableMapOf()
            val keyErrors = ValidationResult.createMutable("Skipped keys")
            val valueErrors = ValidationResult.createMutable("Value errors")
            for (el in table.content) {
                val pairEl = el.asTomlArray()
                val keyToml = pairEl[0]
                val valueToml = pairEl[1]
                val field = "{$fieldName, @key: $keyToml}"
                val keyResult = keyHandler.deserializeEntry(keyToml, field, 1).attachTo(keyErrors)
                if(!keyResult.isValid()) {
                    continue
                }
                val valueResult = valueHandler.deserializeEntry(valueToml, field, 1).attachTo(valueErrors)
                map[keyResult.get()] = valueResult.get()
            }
            val totalErrors = ValidationResult.createMutable("Errors found deserializing map [$fieldName]")
            totalErrors.addError(keyErrors)
            totalErrors.addError(valueErrors)
            ValidationResult.ofMutable(map, totalErrors)
        } catch (e: Throwable) {
            ValidationResult.error(defaultValue, ValidationResult.Errors.DESERIALIZATION, "Exception during map [$fieldName] deserialization, using default map", e)
        }
    }

    @Internal
    @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
    override fun serialize(input: Map<K, V>): ValidationResult<TomlElement> {
        val mapArray = TomlArrayBuilder(input.size)
        val keyErrors = ValidationResult.createMutable("Skipped keys")
        val valueErrors = ValidationResult.createMutable("Value errors")
        return try {
            for ((key, value) in input) {
                val keyEl = keyHandler.serializeEntry(key, 1).attachTo(keyErrors)
                if (!keyEl.isValid()) {
                    continue //skip adding a pair that has an errored key, would be skipped in deserialize anyway
                }
                val valueEl = valueHandler.serializeEntry(value, 1).attachTo(valueErrors)
                val keyAnnotations = if (key != null)
                    try {
                        ConfigApiImpl.tomlAnnotations(key!!::class)
                    } catch (e: Throwable) {
                        emptyList()
                    }
                else
                    emptyList()
                val valueAnnotations = if (value != null)
                    try {
                        ConfigApiImpl.tomlAnnotations(value!!::class)
                    } catch (e: Throwable) {
                        emptyList()
                    }
                else
                    emptyList()
                val pairArray = TomlArrayBuilder(2)
                pairArray.element(keyEl.get(), keyAnnotations)
                pairArray.element(valueEl.get(), valueAnnotations)
                mapArray.element(pairArray.build())
            }
            val totalErrors = ValidationResult.createMutable("Errors found serializing map")
            totalErrors.addError(keyErrors)
            totalErrors.addError(valueErrors)
            return ValidationResult.ofMutable(mapArray.build(), totalErrors)
        } catch (e: Throwable) {
            ValidationResult.error(mapArray.build(), ValidationResult.Errors.SERIALIZATION, "Exception encountered serializing map", e)
        }
    }

    @Internal
    @Suppress("SafeCastWithReturn", "UNCHECKED_CAST")
    override fun deserializedChanged(old: Any?, new: Any?): Boolean {
        old as? Map<K, V> ?: return true
        new as? Map<K, V> ?: return true
        val checked: MutableList<K> = mutableListOf()
        for ((k, v) in old) {
            if (!new.containsKey(k)) return true
            if (valueHandler.deserializedChanged(v, new[k])) return true
            checked.add(k)
        }
        for ((k, _) in new) {
            if (checked.contains(k)) continue
            return true
        }
        return false
    }

    @Internal
    override fun correctEntry(input: Map<K, V>, type: EntryValidator.ValidationType): ValidationResult<Map<K, V>> {
        val map: MutableMap<K, V> = mutableMapOf()
        val keyErrors = ValidationResult.createMutable("Key errors")
        val valueErrors = ValidationResult.createMutable("Value errors")
        for ((key, value) in input) {
            val keyResult = keyHandler.validateEntry(key, type).attachTo(keyErrors)
            if (keyResult.isError()) {
                continue
            }
            map[key] = valueHandler.correctEntry(value, type).attachTo(valueErrors).get()
        }
        val totalErrors = ValidationResult.createMutable("Map correction found errors")
        totalErrors.addError(keyErrors)
        totalErrors.addError(valueErrors)
        return ValidationResult.ofMutable(map, totalErrors)
    }

    @Internal
    override fun validateEntry(input: Map<K, V>, type: EntryValidator.ValidationType): ValidationResult<Map<K, V>> {
        val keyErrors = ValidationResult.createMutable("Key errors")
        val valueErrors = ValidationResult.createMutable("Value errors")
        for ((key, value) in input) {
            keyHandler.validateEntry(key, type).attachTo(keyErrors)
            valueHandler.validateEntry(value, type).attachTo(valueErrors)
        }
        val totalErrors = ValidationResult.createMutable("Map validation found errors")
        totalErrors.addError(keyErrors)
        totalErrors.addError(valueErrors)
        return ValidationResult.ofMutable(input, totalErrors)
    }

    /**
     * creates a deep copy of this ValidatedMap
     * return ValidatedMap wrapping a deep copy of the currently stored map, as well as passing keyHandler and valueHandler
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    override fun instanceEntry(): ValidatedMap<K, V> {
        return ValidatedMap(copyStoredValue(), keyHandler, valueHandler)
    }

    @Internal
    @Suppress("UNCHECKED_CAST")
    override fun isValidEntry(input: Any?): Boolean {
        if (input !is Map<*, *>) return false
        return try {
            validateEntry(input as Map<K, V>, EntryValidator.ValidationType.STRONG).isValid()
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Copies the provided input as deeply as possible. For immutables like numbers and booleans, this will simply return the input
     * @param input Map&lt;[K], [V]%gt; input to be copied
     * @return copied output
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    override fun copyValue(input: Map<K, V>): Map<K, V> {
        return input.toMap()
    }

    @Internal
    //client
    override fun widgetEntry(choicePredicate: ChoiceValidator<Map<K, V>>): ClickableWidget {
        return CustomButtonWidget.builder(TextureIds.MAP_LANG) { b: CustomButtonWidget ->
            openMapEditPopup(PopupWidget.Builder.popupContext { w -> b.x + b.width/2 - w/2 }, PopupWidget.Builder.popupContext { h -> b.y + b.height/2 - h/2 })
        }.size(110, 20).build()
    }

    @Internal
    override fun open(args: List<String>) {
        openMapEditPopup()
    }

    @Internal
    override fun entryDeco(): Decorated.DecoratedOffset? {
        return Decorated.DecoratedOffset(TextureDeco.DECO_MAP, 2, 2)
    }

    @Internal
    override fun contextActionBuilder(context: EntryCreator.CreatorContext): MutableMap<String, MutableMap<ContextType, ContextAction.Builder>> {
        val map = super.contextActionBuilder(context)
        val clear = ContextAction.Builder("fc.validated_field.map.clear".translate()) { p ->
            Popups.openConfirmPopup(p, "fc.validated_field.map.clear.desc".translate()) { this.accept(emptyMap()) }
            true }
            .withActive { s -> Supplier { s.get() && this.isNotEmpty() } }
        map[ContextResultBuilder.COLLECTION] = mutableMapOf(ContextType.CLEAR to clear)
        return map
    }

    @Suppress("UNCHECKED_CAST")
    //client
    private fun openMapEditPopup(xPosition: BiFunction<Int, Int, Int> = PopupWidget.Builder.center(), yPosition: BiFunction<Int, Int, Int> = PopupWidget.Builder.center()) {
        try {
            val map = storedValue.map {
                Pair(
                    (keyHandler.instanceEntry() as Entry<K, *>).also { entry -> entry.accept(it.key) },
                    (valueHandler.instanceEntry() as Entry<V, *>).also { entry -> entry.accept(it.value) }
                )
            }.associate { it }
            val choiceValidator: BiFunction<MapListWidget<K, V>, MapListWidget.MapEntry<K, V>?, ChoiceValidator<K>> = BiFunction{ ll, le ->
                MapListWidget.ExcludeSelfChoiceValidator(le) { self -> ll.getRawMap(self) }
            }
            val mapWidget = MapListWidget(map, keyHandler, valueHandler, choiceValidator)
            val popup = PopupWidget.Builder(this.translation())
                .add("map", mapWidget, LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_LEFT)
                .addDoneWidget()
                .onClose { this.setAndUpdate(mapWidget.getMap()) }
                .positionX(xPosition)
                .positionY(yPosition)
                .build()
            PopupWidget.push(popup)
        } catch (e: Throwable) {
            FC.LOGGER.error("Unexpected exception caught while opening map popup", e)
        }
    }

    /////////////// MAP ///////////////////////////

    override val entries: Set<Map.Entry<K, V>>
        get() = get().entries
    override val keys: Set<K>
        get() = get().keys
    override val size: Int
        get() = get().size
    override val values: Collection<V>
        get() = get().values

    override fun isEmpty(): Boolean {
        return get().isEmpty()
    }

    override fun get(key: K): V? {
        return get()[key]
    }

    override fun containsValue(value: V): Boolean {
        return get().containsValue(value)
    }

    override fun containsKey(key: K): Boolean {
        return get().containsKey(key)
    }

    /////////// END MAP ///////////////////////////

    /**
     * @suppress
     */
     override fun toString(): String {
         return "Validated Map[value=$storedValue, keyHandler=$keyHandler, valueHandler=$valueHandler]"
     }

    internal companion object {
        @Suppress("UNCHECKED_CAST")
        internal fun<K, V> tryMake(map: Map<K, V>, keyHandler: Entry<*, *>, valueHandler: Entry<*, *>): ValidatedMap<K, V>? {
            return try {
                ValidatedMap(map, keyHandler as Entry<K, *>, valueHandler as Entry<V, *>)
            } catch (e: Throwable) {
                return null
            }
        }
    }

    /**
     * Builds a ValidatedMap
     *
     * Not strictly necessary, but may make construction cleaner.
     * @param V value type. any non-null type
     * @author fzzyhmstrs
     * @sample me.fzzyhmstrs.fzzy_config.examples.MapBuilders.stringMap
     * @since 0.2.0
     */
    @Suppress("unused")
    class Builder<K: Any, V: Any> {
        /**
         * Defines the Entry used to handle validation, serialization, etc. for map keys
         * @param handler an [Entry] used as a handler for keys.
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun keyHandler(handler: Entry<K, *>): BuilderWithKey<K, V> {
            return BuilderWithKey(handler)
        }

        class BuilderWithKey<K: Any, V: Any> internal constructor(private val keyHandler: Entry<K, *>) {
            /**
             * Defines the Entry used to handle validation, serialization, etc. for map values
             * @param handler an [Entry] used as a handler for values.
             * @author fzzyhmstrs
             * @since 0.2.0
             */
            fun valueHandler(handler: Entry<V, *>): BuilderWithValue<K, V> {
                return BuilderWithValue(handler, keyHandler)
            }

            class BuilderWithValue<K: Any, V: Any> internal constructor(private val valueHandler: Entry<V, *>, private val keyHandler: Entry<K, *>) {
                private var defaults: Map<K, V> = mapOf()
                /**
                 * Defines the default map used in the ValidatedMap
                 *
                 * If defaults aren't set, the default map will be empty
                 * @param defaults Map<K, V> of default values
                 * @author fzzyhmstrs
                 * @since 0.2.0
                 */
                fun defaults(defaults: Map<K, V>): BuilderWithValue<K, V> {
                    this.defaults = defaults
                    return this
                }
                /**
                 * Defines the default map used in the ValidatedMap
                 *
                 * If defaults aren't set, the default map will be empty
                 * @param defaults vararg Pair<K, V> of default key-value pairs
                 * @author fzzyhmstrs
                 * @since 0.2.0
                 */
                fun defaults(vararg defaults: Pair<K, V>): BuilderWithValue<K, V> {
                    this.defaults = mapOf(*defaults)
                    return this
                }
                /**
                 * Defines a single default key-value pair
                 *
                 * If defaults aren't set, the default map will be empty
                 * @param default single Pair<K, V> to define a single key-value pair map of defaults
                 * @author fzzyhmstrs
                 * @since 0.2.0
                 */
                fun default(default: Pair<K, V>): BuilderWithValue<K, V> {
                    this.defaults = mapOf(default)
                    return this
                }
                /**
                 * Defines a single default key-value pair
                 *
                 * If defaults aren't set, the default map will be empty
                 * @param key single K to define the default map key
                 * @param value single V to define the default map value
                 * @author fzzyhmstrs
                 * @since 0.2.0
                 */
                fun default(key: K, value: V): BuilderWithValue<K, V> {
                    this.defaults = mapOf(key to value)
                    return this
                }
                fun build(): ValidatedMap<K, V> {
                    return ValidatedMap(defaults, keyHandler, valueHandler)
                }
            }
        }


    }



}