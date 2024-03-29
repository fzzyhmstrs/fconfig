package me.fzzyhmstrs.fzzy_config.validation.map

import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.report
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator
import me.fzzyhmstrs.fzzy_config.validation.entry.Entry
import me.fzzyhmstrs.fzzy_config.validation.entry.EntryValidator
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.util.Identifier
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlTableBuilder
import net.peanuuutz.tomlkt.asTomlTable

class ValidatedStringMap<V: Any>(defaultValue: Map<String,V>, private val keyHandler: Entry<String>, private val valueHandler: Entry<V>): ValidatedField<Map<String, V>>(defaultValue) {
    override fun copyStoredValue(): Map<String, V> {
        return storedValue.toMap()
    }

    override fun instanceEntry(): Entry<Map<String, V>> {
        return ValidatedStringMap(storedValue, keyHandler, valueHandler)
    }

    override fun widgetEntry(choicePredicate: ChoiceValidator<Map<String, V>>): ClickableWidget {
        TODO("Not yet implemented")
    }

    override fun serialize(input: Map<String, V>): ValidationResult<TomlElement> {
        val table = TomlTableBuilder()
        val errors: MutableList<String> = mutableListOf()
        return try {
            for ((key, value) in input) {
                val annotations = ConfigApiImpl.tomlAnnotations(value::class.java.kotlin)
                val el = valueHandler.serializeEntry(value, errors, true)
                table.element(key, el, annotations)
            }
            return ValidationResult.predicated(table.build(), errors.isEmpty(), "Errors found while serializing map!")
        } catch (e: Exception){
            ValidationResult.predicated(table.build(), errors.isEmpty(), "Critical exception encountered while serializing map: ${e.localizedMessage}")
        }
    }

    //((?![a-z0-9_-]).) in case I need it...

    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<Map<String, V>> {
        return try {
            val table = toml.asTomlTable()
            val map: MutableMap<String,V> = mutableMapOf()
            val keyErrors: MutableList<String> = mutableListOf()
            val valueErrors: MutableList<String> = mutableListOf()
            for ((key, el) in table.entries){
                val keyResult = keyHandler.validateEntry(key, EntryValidator.ValidationType.WEAK)
                if(keyResult.isError()){
                    keyErrors.add("Skipping key!: ${keyResult.getError()}")
                    continue
                }
                val valueResult = valueHandler.deserializeEntry(el,valueErrors,"{$fieldName, @key: $key}", true).report(valueErrors)
                map[keyResult.get()] = valueResult.get()
            }
            ValidationResult.predicated(map,keyErrors.isEmpty() && valueErrors.isEmpty(), "Errors found deserializing map [$fieldName]: Key Errors = $keyErrors, Value Errors = $valueErrors")
        } catch (e: Exception){
            ValidationResult.error(defaultValue, "Critical exception encountered during map [$fieldName] deserialization, using default map: ${e.localizedMessage}")
        }
    }

    override fun validateEntry(input: Map<String, V>, type: EntryValidator.ValidationType): ValidationResult<Map<String, V>> {
        val keyErrors: MutableList<String> = mutableListOf()
        val valueErrors: MutableList<String> = mutableListOf()
        for ((key, value) in input){
            keyHandler.validateEntry(key,type).report(keyErrors)
            valueHandler.validateEntry(value,type).report(valueErrors)
        }
        return ValidationResult.predicated(input,keyErrors.isEmpty() && valueErrors.isEmpty(), "Map validation had errors: key=${keyErrors}, value=$valueErrors")
    }

    override fun correctEntry(
        input: Map<String, V>,
        type: EntryValidator.ValidationType
    ): ValidationResult<Map<String, V>> {
        val map: MutableMap<String,V> = mutableMapOf()
        val keyErrors: MutableList<String> = mutableListOf()
        val valueErrors: MutableList<String> = mutableListOf()
        for ((key, value) in input){
            val keyResult = keyHandler.validateEntry(key,type).report(keyErrors)
            if (keyResult.isError()){
                continue
            }
            map[key] = valueHandler.correctEntry(value,type).report(valueErrors).report(valueErrors).get()
        }
        return ValidationResult.predicated(map.toMap(),keyErrors.isEmpty() && valueErrors.isEmpty(), "Map correction had errors: key=${keyErrors}, value=$valueErrors")
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
    class Builder<V: Any> {
        /**
         * Defines the [EntryHandler][me.fzzyhmstrs.fzzy_config.validation.entry.EntryHandler] used on map keys
         * @param handler an [Entry] used as a handler for keys.
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun keyHandler(handler: Entry<String>): BuilderWithKey<V>{
            return BuilderWithKey<V>(handler)
        }

        class BuilderWithKey<V: Any> internal constructor(private val keyHandler: Entry<String>) {
            /**
             * Defines the [EntryHandler][me.fzzyhmstrs.fzzy_config.validation.entry.EntryHandler] used on map values
             * @param handler an [Entry] used as a handler for values.
             * @author fzzyhmstrs
             * @since 0.2.0
             */
            fun valueHandler(handler: Entry<V>): BuilderWithValue<V>{
                return BuilderWithValue(handler, keyHandler)
            }

            class BuilderWithValue<V: Any> internal constructor(private val valueHandler: Entry<V>, private val keyHandler: Entry<String>){
                private var defaults: Map<String,V> = mapOf()
                /**
                 * Defines the default map used in the ValidatedMap
                 *
                 * If defaults aren't set, the default map will be empty
                 * @param defaults Map<String,V> of default values
                 * @author fzzyhmstrs
                 * @since 0.2.0
                 */
                fun defaults(defaults: Map<String,V>): BuilderWithValue<V>{
                    this.defaults = defaults
                    return this
                }
                /**
                 * Defines the default map used in the ValidatedMap
                 *
                 * If defaults aren't set, the default map will be empty
                 * @param defaults vararg Pair<String,V> of default key-value pairs
                 * @author fzzyhmstrs
                 * @since 0.2.0
                 */
                fun defaults(vararg defaults: Pair<String,V>): BuilderWithValue<V>{
                    this.defaults = mapOf(*defaults)
                    return this
                }
                /**
                 * Defines a single default key-value pair
                 *
                 * If defaults aren't set, the default map will be empty
                 * @param default single Pair<String,V> to define a single key-value pair map of defaults
                 * @author fzzyhmstrs
                 * @since 0.2.0
                 */
                fun default(default: Pair<String,V>): BuilderWithValue<V>{
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
                fun default(key: String, value: V): BuilderWithValue<V>{
                    this.defaults = mapOf(key to value)
                    return this
                }
                /**
                 * Defines the default map used in the ValidatedMap
                 *
                 * This map will be converted to a Map<String,V> internally. If defaults aren't set, the default map will be empty
                 * @param defaults Map<Identifier,V> of default values
                 * @author fzzyhmstrs
                 * @since 0.2.0
                 */
                fun defaultIds(defaults: Map<Identifier,V>): BuilderWithValue<V>{
                    this.defaults = defaults.mapKeys { e -> e.key.toString() }
                    return this
                }
                /**
                 * Defines the default map used in the ValidatedMap
                 *
                 * This map will be converted to a Map<String,V> internally. If defaults aren't set, the default map will be empty
                 * @param defaults vararg Pair<Identifier,V> of default key-value pairs
                 * @author fzzyhmstrs
                 * @since 0.2.0
                 */
                fun defaultIds(vararg defaults: Pair<Identifier,V>): BuilderWithValue<V>{
                    this.defaults = (defaults).associate { p -> Pair(p.first.toString(),p.second) }
                    return this
                }
                /**
                 * Defines a single default key-value pair
                 *
                 * This pair will be converted to a Pair<String,V> internally. If defaults aren't set, the default map will be empty
                 * @param default single Pair<Identifier,V> to define a single key-value pair map of defaults
                 * @author fzzyhmstrs
                 * @since 0.2.0
                 */
                fun defaultId(default: Pair<Identifier,V>): BuilderWithValue<V>{
                    this.defaults = mapOf(Pair(default.first.toString(),default.second))
                    return this
                }
                /**
                 * Defines a single default key-value pair
                 *
                 * This pair will be converted to a Pair<String,V> internally. If defaults aren't set, the default map will be empty
                 * @param key single Identifier to define the default map key
                 * @param value single V to define the default map value
                 * @author fzzyhmstrs
                 * @since 0.2.0
                 */
                fun defaultId(key: Identifier, value: V): BuilderWithValue<V>{
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
                    return ValidatedStringMap(defaults,keyHandler,valueHandler)
                }
            }
        }


    }



}