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
import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.screen.widget.ActiveButtonWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.DecoratedActiveButtonWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.TextureIds
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.report
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedMap.Builder
import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator
import net.minecraft.client.gui.widget.ClickableWidget
import net.peanuuutz.tomlkt.TomlArrayBuilder
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.asTomlArray
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.function.BiFunction
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

/**
 * A validated Map of arbitrary (Validated) keys and values
 *
 * NOTE: This construct is handled as an array of array pairs in TOML. It may not look like a traditional TOML map.
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
open class ValidatedMap<K, V>(defaultValue: Map<K, V>, private val keyHandler: Entry<K, *>, private val valueHandler: Entry<V, *>): ValidatedField<Map<K, V>>(defaultValue), Map<K, V> {

    init {
        for((key, value) in defaultValue) {
            if (keyHandler.validateEntry(key, EntryValidator.ValidationType.WEAK).isError())
                throw IllegalStateException("Default Map key [$key] not valid per keyHandler provided")
            if (valueHandler.validateEntry(value, EntryValidator.ValidationType.WEAK).isError())
                throw IllegalStateException("Default Map value [$value] not valid per valueHandler provided")
        }
    }

    /**
     * Creates a deep copy of the stored value and returns it
     * @return Map&lt;K, V&gt; - deep copy of the currently stored map
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    override fun copyStoredValue(): Map<K, V> {
        return storedValue.toMap()
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
    //client
    override fun widgetEntry(choicePredicate: ChoiceValidator<Map<K, V>>): ClickableWidget {
        return DecoratedActiveButtonWidget("fc.validated_field.map".translate(), 110, 20, TextureIds.DECO_MAP, {true}, { b: ActiveButtonWidget -> openMapEditPopup(b) })
    }

    @Suppress("UNCHECKED_CAST")
    //client
    private fun openMapEditPopup(b: ActiveButtonWidget) {
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
                .addElement("map", mapWidget, PopupWidget.Builder.Position.BELOW, PopupWidget.Builder.Position.ALIGN_LEFT)
                .addDoneWidget()
                .onClose { this.setAndUpdate(mapWidget.getMap()) }
                .positionX(PopupWidget.Builder.popupContext { w -> b.x + b.width/2 - w/2 })
                .positionY(PopupWidget.Builder.popupContext { h -> b.y + b.height/2 - h/2 })
                .build()
            PopupWidget.push(popup)
        } catch (e: Throwable) {
            FC.LOGGER.error("Unexpected exception caught while opening map popup")
            e.printStackTrace()
        }
    }
    @Internal
    override fun serialize(input: Map<K, V>): ValidationResult<TomlElement> {
        val mapArray = TomlArrayBuilder(input.size)
        val errors: MutableList<String> = mutableListOf()
        return try {
            for ((key, value) in input) {
                val pairArray = TomlArrayBuilder(2)
                val keyAnnotations = if (value != null)
                    try {
                        ConfigApiImpl.tomlAnnotations(value!!::class)
                    } catch (e: Throwable) {
                        listOf()
                    }
                else
                    listOf()
                val valueAnnotations = if (value != null)
                    try {
                        ConfigApiImpl.tomlAnnotations(value!!::class)
                    } catch (e: Throwable) {
                        listOf()
                    }
                else
                    listOf()
                val keyEl = keyHandler.serializeEntry(key, errors, 1)
                val valueEl = valueHandler.serializeEntry(value, errors, 1)
                pairArray.element(keyEl, keyAnnotations)
                pairArray.element(valueEl, valueAnnotations)
                mapArray.element(pairArray.build())
            }
            return ValidationResult.predicated(mapArray.build(), errors.isEmpty(), "Errors found while serializing map!")
        } catch (e: Throwable) {
            ValidationResult.predicated(mapArray.build(), errors.isEmpty(), "Critical exception encountered while serializing map: ${e.localizedMessage}")
        }
    }

    //((?![a-z0-9_-]).) in case I need it...
    @Internal
    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<Map<K, V>> {
        return try {
            val mapArray = toml.asTomlArray()
            val map: MutableMap<K, V> = mutableMapOf()
            val keyErrors: MutableList<String> = mutableListOf()
            val valueErrors: MutableList<String> = mutableListOf()
            for (pairEl in mapArray) {
                val pairArray = pairEl.asTomlArray()
                val key = pairArray[0]
                val keyResult = keyHandler.deserializeEntry(key, keyErrors, "{$fieldName, @key: $key}", 1).report(keyErrors)
                if (keyResult.isError()) {
                    continue
                }
                val value = pairArray[1]
                val valueResult = valueHandler.deserializeEntry(value, valueErrors, "{$fieldName, @key: $key}", 1).report(valueErrors)
                map[keyResult.get()] = valueResult.get()
            }
            ValidationResult.predicated(map, keyErrors.isEmpty() && valueErrors.isEmpty(), "Errors found deserializing map [$fieldName]: key = $keyErrors, value = $valueErrors")
        } catch (e: Throwable) {
            ValidationResult.error(defaultValue, "Critical exception encountered during map [$fieldName] deserialization, using default map: ${e.localizedMessage}")
        }
    }
    @Internal
    override fun validateEntry(input: Map<K, V>, type: EntryValidator.ValidationType): ValidationResult<Map<K, V>> {
        val keyErrors: MutableList<String> = mutableListOf()
        val valueErrors: MutableList<String> = mutableListOf()
        for ((key, value) in input) {
            keyHandler.validateEntry(key, type).report(keyErrors)
            valueHandler.validateEntry(value, type).report(valueErrors)
        }
        return ValidationResult.predicated(input, keyErrors.isEmpty() && valueErrors.isEmpty(), "Map validation had errors: key=${keyErrors}, value=$valueErrors")
    }
    @Internal
    override fun correctEntry(input: Map<K, V>, type: EntryValidator.ValidationType): ValidationResult<Map<K, V>> {
        val map: MutableMap<K, V> = mutableMapOf()
        val keyErrors: MutableList<String> = mutableListOf()
        val valueErrors: MutableList<String> = mutableListOf()
        for ((key, value) in input) {
            val keyResult = keyHandler.validateEntry(key, type).report(keyErrors)
            if (keyResult.isError()) {
                continue
            }
            map[key] = valueHandler.correctEntry(value, type).report(valueErrors).report(valueErrors).get()
        }
        return ValidationResult.predicated(map.toMap(), keyErrors.isEmpty() && valueErrors.isEmpty(), "Map correction had errors: key = ${keyErrors}, value = $valueErrors")
    }
    @Internal
    override fun isValidEntry(input: Any?): Boolean {
        if (input !is Map<*, *>) return false
        return try {
            validateEntry(input as Map<K, V>, EntryValidator.ValidationType.STRONG).isValid()
        } catch (e: Throwable) {
            false
        }
    }

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

    /**
     * @suppress
     */
     override fun toString(): String {
         return "Validated Map[value=$storedValue, keyHandler=$keyHandler, valueHandler=$valueHandler]"
     }

    companion object {
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
            return BuilderWithKey<K, V>(handler)
        }

        class BuilderWithKey<K: Any, V: Any> internal constructor(private val keyHandler: Entry<K, *>) {
            /**
             * Defines the Entry used to handle validation, serialization, etc. for map values
             * @param handler an [Entry] used as a handler for values.
             * @author fzzyhmstrs
             * @since 0.2.0
             */
            fun valueHandler(handler: Entry<V, *>): BuilderWithValue<K, V> {
                return BuilderWithValue<K, V>(handler, keyHandler)
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