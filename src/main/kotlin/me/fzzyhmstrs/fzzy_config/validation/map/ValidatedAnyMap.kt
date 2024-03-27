package me.fzzyhmstrs.fzzy_config.validation.map

import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import me.fzzyhmstrs.fzzy_config.api.ValidationResult.Companion.report
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.validation.entry.Entry
import me.fzzyhmstrs.fzzy_config.validation.entry.EntryValidator
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.util.Identifier
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlTableBuilder
import net.peanuuutz.tomlkt.asTomlTable

class ValidatedAnyMap<K: Any,V: Any>(defaultValue: Map<String,V>, private val keyHandler: Entry<K>, private val valueHandler: Entry<V>): ValidatedField<Map<K, V>>(defaultValue) {
    override fun copyStoredValue(): Map<K, V> {
        return storedValue.toMap()
    }

    override fun instanceEntry(): Entry<Map<K, V>> {
        return ValidatedAnyMap(copyStoredValue(), keyHandler, valueHandler)
    }

    override fun widgetEntry(): ClickableWidget {
        TODO("Not yet implemented")
    }

    override fun serialize(input: Map<K, V>): ValidationResult<TomlElement> {
        val mapArray = TomlArrayBuilder(input.size)
        val errors: MutableList<String> = mutableListOf()
        return try {
            for ((key, value) in input) {
                val pairArray = TomlArrayBuilder(2)
                val keyAnnotation = ConfigApiImpl.tomlAnnotations(key::class.java.kotlin)
                val valueAnnotation = ConfigApiImpl.tomlAnnotations(value::class.java.kotlin)
                val keyEl = keyHandler.serializeEntry(key, errors, true)
                val valueEl = valueHandler.serializeEntry(key, errors, true)
                pairArray.element(keyEl, keyAnnotations)
                pairArray.element(valueEl, valueAnnotations)
                mapArray.element(pairArray.build())
            }
            return ValidationResult.predicated(mapArray.build(), errors.isEmpty(), "Errors found while serializing map!")
        } catch (e: Exception){
            ValidationResult.predicated(mapArray.build(), errors.isEmpty(), "Critical exception encountered while serializing map: ${e.localizedMessage}")
        }
    }

    //((?![a-z0-9_-]).) in case I need it...

    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<Map<K, V>> {
        return try {
            val mapArray = toml.asTomlArray()
            val map: MutableMap<K,V> = mutableMapOf()
            val keyErrors: MutableList<String> = mutableListOf()
            val valueErrors: MutableList<String> = mutableListOf()
            for (pairArray in mapArray){
                val key = pairArray[0]
                val keyResult = keyHandler.deserializeEntry(key,keyErrors,"{$fieldName, @key: $key}", true).report(keyErrors)
                if (keyResult.isError()){
                    continue
                }
                val value = pairArray[1]
                val valueResult = valueHandler.deserializeEntry(el,valueErrors,"{$fieldName, @key: $key}", true).report(valueErrors)
                map[keyResult.get()] = valueResult.get()
            }
            ValidationResult.predicated(map,keyErrors.isEmpty() && valueErrors.isEmpty(), "Errors found deserializing map [$fieldName]: key = $keyErrors, value = $valueErrors")
        } catch (e: Exception){
            ValidationResult.error(defaultValue, "Critical exception encountered during map [$fieldName] deserialization, using default map: ${e.localizedMessage}")
        }
    }

    override fun validateEntry(input: Map<K, V>, type: EntryValidator.ValidationType): ValidationResult<Map<K, V>> {
        val keyErrors: MutableList<String> = mutableListOf()
        val valueErrors: MutableList<String> = mutableListOf()
        for ((key, value) in input){
            keyHandler.validateEntry(key,type).report(keyErrors)
            valueHandler.validateEntry(value,type).report(valueErrors)
        }
        return ValidationResult.predicated(input,keyErrors.isEmpty() && valueErrors.isEmpty(), "Map validation had errors: key=${keyErrors}, value=$valueErrors")
    }

    override fun correctEntry(input: Map<K, V>,type: EntryValidator.ValidationType): ValidationResult<Map<K, V>> {
        val map: MutableMap<K,V> = mutableMapOf()
        val keyErrors: MutableList<String> = mutableListOf()
        val valueErrors: MutableList<String> = mutableListOf()
        for ((key, value) in input){
            val keyResult = keyHandler.validateEntry(key,type).report(keyErrors)
            if (keyResult.isError()){
                continue
            }
            map[key] = valueHandler.correctEntry(value,type).report(valueErrors).report(valueErrors).get()
        }
        return ValidationResult.predicated(map.toMap(),keyErrors.isEmpty() && valueErrors.isEmpty(), "Map correction had errors: key = ${keyErrors}, value = $valueErrors")
    }

    /**
     * Builds a ValidatedMap
     *
     * Not strictly necessary, but may make construction cleaner.
     * @param V value type. any non-null type
     * @author fzzyhmstrs
     * @sample me.fzzyhmstrs.fzzy_config.examples.MapBuilders.stringTest
     * @since 0.2.0
     */
    @Suppress("unused")
    class Builder<K: Any,V: Any> {
        /**
         * Defines the [EntryHandler][me.fzzyhmstrs.fzzy_config.validation.entry.EntryHandler] used on map keys
         * @param handler an [Entry] used as a handler for keys.
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun keyHandler(handler: Entry<K>): BuilderWithKey<K,V>{
            return BuilderWithKey<K,V>(handler)
        }

        class BuilderWithKey<K: Any, V: Any> internal constructor(private val keyHandler: Entry<K>) {
            /**
             * Defines the [EntryHandler][me.fzzyhmstrs.fzzy_config.validation.entry.EntryHandler] used on map values
             * @param handler an [Entry] used as a handler for values.
             * @author fzzyhmstrs
             * @since 0.2.0
             */
            fun valueHandler(handler: Entry<V>): BuilderWithValue<K,V>{
                return BuilderWithValue<K,V>(handler, keyHandler)
            }

            class BuilderWithValue<K: Any,V: Any> internal constructor(private val valueHandler: Entry<V>, private val keyHandler: Entry<K>){
                private var defaults: Map<String,V> = mapOf()
                /**
                 * Defines the default map used in the ValidatedMap
                 *
                 * If defaults aren't set, the default map will be empty
                 * @param defaults Map<K,V> of default values
                 * @author fzzyhmstrs
                 * @since 0.2.0
                 */
                fun defaults(defaults: Map<K,V>): BuilderWithValue<K,V>{
                    this.defaults = defaults
                    return this
                }
                /**
                 * Defines the default map used in the ValidatedMap
                 *
                 * If defaults aren't set, the default map will be empty
                 * @param defaults vararg Pair<K,V> of default key-value pairs
                 * @author fzzyhmstrs
                 * @since 0.2.0
                 */
                fun defaults(vararg defaults: Pair<K,V>): BuilderWithValue<K,V>{
                    this.defaults = mapOf(*defaults)
                    return this
                }
                /**
                 * Defines a single default key-value pair
                 *
                 * If defaults aren't set, the default map will be empty
                 * @param default single Pair<K,V> to define a single key-value pair map of defaults
                 * @author fzzyhmstrs
                 * @since 0.2.0
                 */
                fun default(default: Pair<K,V>): BuilderWithValue<K,V>{
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
                fun default(key: K, value: V): BuilderWithValue<K,V>{
                    this.defaults = mapOf(key to value)
                    return this
                fun build(): ValidatedAnyMap<K,V> {
                    return ValidatedAnyMap(defaults,keyHandler,valueHandler)
                }
            }
        }


    }



}
