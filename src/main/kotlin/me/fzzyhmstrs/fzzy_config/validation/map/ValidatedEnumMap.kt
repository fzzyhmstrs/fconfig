package me.fzzyhmstrs.fzzy_config.validation.map

import me.fzzyhmstrs.fzzy_config.api.Translatable
import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import me.fzzyhmstrs.fzzy_config.api.ValidationResult.Companion.report
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.validation.entry.Entry
import me.fzzyhmstrs.fzzy_config.validation.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedEnum
import net.minecraft.client.gui.widget.ClickableWidget
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral
import net.peanuuutz.tomlkt.TomlTableBuilder
import net.peanuuutz.tomlkt.asTomlTable

class ValidatedEnumMap<K,V: Any>(defaultValue: Map<K,V>, private val keyHandler: Entry<K>, private val valueHandler: Entry<V>): ValidatedField<Map<K, V>>(defaultValue) where K:Enum<K>, K: Translatable{
    override fun copyStoredValue(): Map<K, V> {
        return storedValue.toMap()
    }

    override fun instanceEntry(): Entry<Map<K, V>> {
        return ValidatedEnumMap(storedValue, keyHandler, valueHandler)
    }

    override fun widgetEntry(): ClickableWidget {
        TODO("Not yet implemented")
    }

    override fun serialize(input: Map<K, V>): ValidationResult<TomlElement> {
        val table = TomlTableBuilder()
        val errors: MutableList<String> = mutableListOf()
        return try {
            for ((key, value) in storedValue) {
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

    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<Map<K, V>> {
        return try {
            val table = toml.asTomlTable()
            val map: MutableMap<K,V> = mutableMapOf()
            val keyErrors: MutableList<String> = mutableListOf()
            val valueErrors: MutableList<String> = mutableListOf()
            for ((keyToml, el) in table.entries){
                val keyResult = keyHandler.deserializeEntry(TomlLiteral(keyToml), keyErrors, "{$fieldName, @key: $keyToml}", true)
                if(keyResult.isError()){
                    keyErrors.add("Skipping key!: ${keyResult.getError()}")
                    continue
                }
                val valueResult = valueHandler.deserializeEntry(el, valueErrors,"{$fieldName, @key: $keyToml}", true).report(valueErrors)
                map[keyResult.get()] = valueResult.get()
            }
            ValidationResult.predicated(map,keyErrors.isEmpty() && valueErrors.isEmpty(), "Errors found deserializing map [$fieldName]: Key Errors = $keyErrors, Value Errors = $valueErrors")
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

    override fun correctEntry(input: Map<K, V>, type: EntryValidator.ValidationType): ValidationResult<Map<K, V>> {
        val map: MutableMap<K,V> = mutableMapOf()
        val keyErrors: MutableList<String> = mutableListOf()
        val valueErrors: MutableList<String> = mutableListOf()
        for ((key, value) in input){
            val keyResult = keyHandler.validateEntry(key,type).report(keyErrors)
            if (keyResult.isError()){
                continue
            }
            map[key] = valueHandler.correctEntry(value,type).report(valueErrors).get()
        }
        return ValidationResult.predicated(map.toMap(),keyErrors.isEmpty() && valueErrors.isEmpty(), "Map correction had errors: key=${keyErrors}, value=$valueErrors")
    }

    /**
     * Builds a ValidatedEnumMap
     *
     * Not strictly necessary, but may make construction cleaner.
     * @param K key type, needs to be [Enum] and [Translatable]
     * @param V value type. any non-null type.
     * @author fzzyhmstrs
     * @sample me.fzzyhmstrs.fzzy_config.examples.MapBuilders.TestEnum
     * @sample me.fzzyhmstrs.fzzy_config.examples.MapBuilders.enumTest
     * @since 0.2.0
     */
    @Suppress("unused", "DeprecatedCallableAddReplaceWith")
    class Builder<K,V: Any> where K: Enum<K>, K: Translatable {
        /**
         * Defines the [EntryHandler][me.fzzyhmstrs.fzzy_config.validation.entry.EntryHandler] used on map keys
         * @param handler an [Entry] used as a handler for keys. Typically a [ValidatedEnum]
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @Deprecated("For basic ValidatedEnum implementation, see keyHandler(defaultValue: K)")
        fun keyHandler(handler: Entry<K>): BuilderWithKey<K,V>{
            return BuilderWithKey<K,V>(handler)
        }
        /**
         * Defines the [EntryHandler][me.fzzyhmstrs.fzzy_config.validation.entry.EntryHandler] used on map keys
         *
         * The builder will internally create a [ValidatedEnum] wrapping the default value.
         * @param defaultValue the default value of [K]
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun keyHandler(defaultValue: K): BuilderWithKey<K,V>{
            return BuilderWithKey<K,V>(ValidatedEnum(defaultValue))
        }
    }

    class BuilderWithKey<K,V: Any> internal constructor (private val keyHandler: Entry<K>)where K: Enum<K>, K: Translatable{
        /**
         * Defines the [EntryHandler][me.fzzyhmstrs.fzzy_config.validation.entry.EntryHandler] used on map values
         * @param valueHandler an [Entry] used as a handler for values.
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun valueHandler(valueHandler: Entry<V>): BuilderWithValue<K,V>{
            return BuilderWithValue(valueHandler,keyHandler)
        }
    }

    class BuilderWithValue<K,V: Any>internal constructor (private val valueHandler: Entry<V>, private val keyHandler: Entry<K>)where K: Enum<K>, K: Translatable{
        private var defaults: Map<K,V> = mapOf()

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
        }
        /**
         * Builds the Builder into a ValidatedEnumMap
         * @return ValidatedEnumMap based on the builder inputs
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun build(): ValidatedEnumMap<K,V>{
            return ValidatedEnumMap(defaults,keyHandler,valueHandler)
        }
    }

}