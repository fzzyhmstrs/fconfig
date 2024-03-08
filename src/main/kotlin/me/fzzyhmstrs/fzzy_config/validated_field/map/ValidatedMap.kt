package me.fzzyhmstrs.fzzy_config.validated_field.map

import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import me.fzzyhmstrs.fzzy_config.api.ValidationResult.Companion.report
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.validated_field.ValidatedField
import me.fzzyhmstrs.fzzy_config.validated_field.entry.Entry
import me.fzzyhmstrs.fzzy_config.validated_field.entry.EntryValidator
import net.minecraft.client.gui.widget.ClickableWidget
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlTableBuilder
import net.peanuuutz.tomlkt.asTomlTable

class ValidatedMap<V: Any>(defaultValue: Map<String,V>, private val keyHandler: Entry<String>, private val valueHandler: Entry<V>): ValidatedField<Map<String,V>>(defaultValue) {
    override fun copyStoredValue(): Map<String, V> {
        return storedValue.toMap()
    }

    override fun instanceEntry(): Entry<Map<String, V>> {
        return ValidatedMap(storedValue, keyHandler, valueHandler)
    }

    override fun widgetEntry(): ClickableWidget {
        TODO("Not yet implemented")
    }

    override fun serialize(input: Map<String, V>): ValidationResult<TomlElement> {
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

    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<Map<String, V>> {
        return try {
            val table = toml.asTomlTable()
            val map: MutableMap<String,V> = mutableMapOf()
            val keyErrors: MutableList<String> = mutableListOf()
            val valueErrors: MutableList<String> = mutableListOf()
            for ((key, el) in table.entries){
                val keyResult = keyHandler.validateEntry(key,EntryValidator.ValidationType.WEAK)
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

}