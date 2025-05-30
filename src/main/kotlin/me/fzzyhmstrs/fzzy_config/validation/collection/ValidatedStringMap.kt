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
import me.fzzyhmstrs.fzzy_config.entry.EntryOpener
import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.screen.decoration.Decorated
import me.fzzyhmstrs.fzzy_config.screen.widget.*
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomButtonWidget
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.attachTo
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedStringMap.Builder
import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.util.Identifier
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral
import net.peanuuutz.tomlkt.TomlTableBuilder
import net.peanuuutz.tomlkt.asTomlTable
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.function.BiFunction

/**
 * A Validated Map with String keys
 *
 * [See the Wiki](https://moddedmc.wiki/en/project/fzzy-config/docs/config-concepts/validation/Collections) for more details and examples.
 * @param V any non-null type with a valid [Entry] for handling
 * @param defaultValue the default map
 * @param keyHandler the key handler, an [Entry]<String>, typically a [me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedString]
 * @param valueHandler the value handler, an [Entry]
 * @see Builder using the builder is recommended
 * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedCollectionExamples.stringMaps
 * @author fzzyhmstrs
 * @since 0.1.0
 */
open class ValidatedStringMap<V>(defaultValue: Map<String, V>, private val keyHandler: Entry<String, *>, private val valueHandler: Entry<V, *>): ValidatedField<Map<String, V>>(defaultValue), Map<String, V>, EntryOpener {

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
    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<Map<String, V>> {
        return try {
            val table = toml.asTomlTable()
            val map: MutableMap<String, V> = mutableMapOf()
            val keyErrors = ValidationResult.createMutable("Skipped keys")
            val valueErrors = ValidationResult.createMutable("Value errors")
            for ((keyToml, el) in table.entries) {
                val field = "{$fieldName, @key: $keyToml}"
                val keyResult = keyHandler.deserializeEntry(TomlLiteral(keyToml), field, 1).attachTo(keyErrors)
                if(!keyResult.isValid()) {
                    continue
                }
                val valueResult = valueHandler.deserializeEntry(el, field, 1).attachTo(valueErrors)
                map[keyResult.get()] = valueResult.get()
            }
            val totalErrors = ValidationResult.createMutable("Errors found deserializing string map [$fieldName]")
            totalErrors.addError(keyErrors)
            totalErrors.addError(valueErrors)
            ValidationResult.ofMutable(map, totalErrors)
        } catch (e: Throwable) {
            ValidationResult.error(defaultValue, ValidationResult.Errors.DESERIALIZATION, "Exception during string map [$fieldName] deserialization, using default map", e)
        }
    }

    @Internal
    @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
    override fun serialize(input: Map<String, V>): ValidationResult<TomlElement> {
        val table = TomlTableBuilder()
        return try {
            val errors = ValidationResult.createMutable("Errors found serializing string map")
            for ((key, value) in input) {
                val annotations = if (value != null)
                    try {
                        ConfigApiImpl.tomlAnnotations(value!!::class)
                    } catch (e: Throwable) {
                        emptyList()
                    }
                else
                    emptyList()
                val el = valueHandler.serializeEntry(value, 1).attachTo(errors)
                table.element(key, el.get(), annotations)
            }
            return ValidationResult.ofMutable(table.build(), errors)
        } catch (e: Throwable) {
            ValidationResult.error(table.build(), ValidationResult.Errors.SERIALIZATION, "Exception while serializing string map", e)
        }
    }

    @Internal
    @Suppress("SafeCastWithReturn", "UNCHECKED_CAST")
    override fun deserializedChanged(old: Any?, new: Any?): Boolean {
        old as? Map<String, V> ?: return true
        new as? Map<String, V> ?: return true
        val checked: MutableList<String> = mutableListOf()
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
    override fun correctEntry(input: Map<String, V>, type: EntryValidator.ValidationType): ValidationResult<Map<String, V>> {
        val map: MutableMap<String, V> = mutableMapOf()
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
    override fun validateEntry(input: Map<String, V>, type: EntryValidator.ValidationType): ValidationResult<Map<String, V>> {
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

    override fun instanceEntry(): ValidatedStringMap<V> {
        return ValidatedStringMap(storedValue, keyHandler, valueHandler)
    }

    @Internal
    @Suppress("UNCHECKED_CAST")
    override fun isValidEntry(input: Any?): Boolean {
        if (input !is Map<*, *>) return false
        return try {
            validateEntry(input as Map<String, V>, EntryValidator.ValidationType.STRONG).isValid()
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Copies the provided input as deeply as possible. For immutables like numbers and booleans, this will simply return the input
     * @param input Map&lt;String, [V]%gt; input to be copied
     * @return copied output
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    override fun copyValue(input: Map<String, V>): Map<String, V> {
        return input.toMap()
    }

    @Internal
    override fun widgetEntry(choicePredicate: ChoiceValidator<Map<String, V>>): ClickableWidget {
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

    @Suppress("UNCHECKED_CAST")
    //client
    private fun openMapEditPopup(xPosition: BiFunction<Int, Int, Int> = PopupWidget.Builder.center(), yPosition: BiFunction<Int, Int, Int> = PopupWidget.Builder.center()) {
        try {
            val map = storedValue.map {
                Pair(
                    (keyHandler.instanceEntry() as Entry<String, *>).also { entry -> entry.accept(it.key) },
                    (valueHandler.instanceEntry() as Entry<V, *>).also { entry -> entry.accept(it.value) }
                )
            }.associate { it }
            val choiceValidator: BiFunction<MapListWidget<String, V>, MapListWidget.MapEntry<String, V>?, ChoiceValidator<String>> = BiFunction{ ll, le ->
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
            FC.LOGGER.error("Unexpected exception caught while opening string map popup", e)
        }
    }

    /////////////// MAP ///////////////////////////

    override val entries: Set<Map.Entry<String, V>>
        get() = get().entries
    override val keys: Set<String>
        get() = get().keys
    override val size: Int
        get() = get().size
    override val values: Collection<V>
        get() = get().values

    override fun isEmpty(): Boolean {
        return get().isEmpty()
    }

    override fun get(key: String): V? {
        return get()[key]
    }

    override fun containsValue(value: V): Boolean {
        return get().containsValue(value)
    }

    override fun containsKey(key: String): Boolean {
        return get().containsKey(key)
    }

    /////////// END MAP ///////////////////////////

    internal companion object {
        @Suppress("UNCHECKED_CAST")
        internal fun<V> tryMake(map: Map<String, V>, keyHandler: Entry<*, *>, valueHandler: Entry<*, *>): ValidatedStringMap<V>? {
            return try {
                ValidatedStringMap(map, keyHandler as Entry<String, *>, valueHandler as Entry<V, *>)
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
    class Builder<V: Any> {
        /**
         * Defines the Entry used to handle validation, serialization, etc. for map keys
         * @param handler an [Entry] used as a handler for keys.
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun keyHandler(handler: Entry<String, *>): BuilderWithKey<V> {
            return BuilderWithKey(handler)
        }

        class BuilderWithKey<V: Any> internal constructor(private val keyHandler: Entry<String, *>) {
            /**
             * Defines the Entry used to handle validation, serialization, etc. for map values
             * @param handler an [Entry] used as a handler for values.
             * @author fzzyhmstrs
             * @since 0.2.0
             */
            fun valueHandler(handler: Entry<V, *>): BuilderWithValue<V> {
                return BuilderWithValue(handler, keyHandler)
            }

            class BuilderWithValue<V: Any> internal constructor(private val valueHandler: Entry<V, *>, private val keyHandler: Entry<String, *>) {
                private var defaults: Map<String, V> = mapOf()
                /**
                 * Defines the default map used in the ValidatedMap
                 *
                 * If defaults aren't set, the default map will be empty
                 * @param defaults Map<String, V> of default values
                 * @author fzzyhmstrs
                 * @since 0.2.0
                 */
                fun defaults(defaults: Map<String, V>): BuilderWithValue<V> {
                    this.defaults = defaults
                    return this
                }
                /**
                 * Defines the default map used in the ValidatedMap
                 *
                 * If defaults aren't set, the default map will be empty
                 * @param defaults vararg Pair<String, V> of default key-value pairs
                 * @author fzzyhmstrs
                 * @since 0.2.0
                 */
                fun defaults(vararg defaults: Pair<String, V>): BuilderWithValue<V> {
                    this.defaults = mapOf(*defaults)
                    return this
                }
                /**
                 * Defines a single default key-value pair
                 *
                 * If defaults aren't set, the default map will be empty
                 * @param default single Pair<String, V> to define a single key-value pair map of defaults
                 * @author fzzyhmstrs
                 * @since 0.2.0
                 */
                fun default(default: Pair<String, V>): BuilderWithValue<V> {
                    this.defaults = mapOf(default)
                    return this
                }
                /**
                 * Defines a single default key-value pair
                 *
                 * If defaults aren't set, the default map will be empty
                 * @param key single String to define the default map key
                 * @param value single V to define the default map value
                 * @author fzzyhmstrs
                 * @since 0.2.0
                 */
                fun default(key: String, value: V): BuilderWithValue<V> {
                    this.defaults = mapOf(key to value)
                    return this
                }
                /**
                 * Defines the default map used in the ValidatedMap
                 *
                 * This map will be converted to a Map<String, V> internally. If defaults aren't set, the default map will be empty
                 * @param defaults Map<Identifier, V> of default values
                 * @author fzzyhmstrs
                 * @since 0.2.0
                 */
                fun defaultIds(defaults: Map<Identifier, V>): BuilderWithValue<V> {
                    this.defaults = defaults.mapKeys { e -> e.key.toString() }
                    return this
                }
                /**
                 * Defines the default map used in the ValidatedMap
                 *
                 * This map will be converted to a Map<String, V> internally. If defaults aren't set, the default map will be empty
                 * @param defaults vararg Pair<Identifier, V> of default key-value pairs
                 * @author fzzyhmstrs
                 * @since 0.2.0
                 */
                fun defaultIds(vararg defaults: Pair<Identifier, V>): BuilderWithValue<V> {
                    this.defaults = (defaults).associate { p -> Pair(p.first.toString(), p.second) }
                    return this
                }
                /**
                 * Defines a single default key-value pair
                 *
                 * This pair will be converted to a Pair<String, V> internally. If defaults aren't set, the default map will be empty
                 * @param default single Pair<Identifier, V> to define a single key-value pair map of defaults
                 * @author fzzyhmstrs
                 * @since 0.2.0
                 */
                fun defaultId(default: Pair<Identifier, V>): BuilderWithValue<V> {
                    this.defaults = mapOf(Pair(default.first.toString(), default.second))
                    return this
                }
                /**
                 * Defines a single default key-value pair
                 *
                 * This pair will be converted to a Pair<String, V> internally. If defaults aren't set, the default map will be empty
                 * @param key single Identifier to define the default map key
                 * @param value single V to define the default map value
                 * @author fzzyhmstrs
                 * @since 0.2.0
                 */
                fun defaultId(key: Identifier, value: V): BuilderWithValue<V> {
                    this.defaults = mapOf(key.toString() to value)
                    return this
                }
                /**
                 * Builds the Builder into a ValidatedMap
                 * @return ValidatedMap based on the builder inputs
                 * @author fzzyhmstrs
                 * @since 0.2.0
                 */
                fun build(): ValidatedStringMap<V> {
                    return ValidatedStringMap(defaults, keyHandler, valueHandler)
                }
            }
        }


    }



}