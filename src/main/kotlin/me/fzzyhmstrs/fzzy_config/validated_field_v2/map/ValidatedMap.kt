package me.fzzyhmstrs.fzzy_config.validated_field_v2.map

import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import me.fzzyhmstrs.fzzy_config.api.ValidationResult.Companion.also
import me.fzzyhmstrs.fzzy_config.validated_field_v2.ValidatedField
import me.fzzyhmstrs.fzzy_config.validated_field_v2.entry.Entry
import me.fzzyhmstrs.fzzy_config.validated_field_v2.entry.EntryCorrector
import me.fzzyhmstrs.fzzy_config.validated_field_v2.entry.EntryValidator
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.asTomlTable

class ValidatedMap<V: Any>(defaultValue: Map<String,V>, private val keyHandler: Entry<String>, private val valueHandler: Entry<V>): ValidatedField<Map<String,V>>(defaultValue) {

    //((?![a-z0-9_-]).) in case I need it...

    override fun deserializeEntry(toml: TomlElement, fieldName: String): ValidationResult<Map<String, V>> {
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
                val valueResult = valueHandler.deserializeEntry(el,"{$fieldName, @key: $key}")
                if(valueResult.isError()){
                    valueErrors.add(valueResult.getError())
                }
                map[keyResult.get()] = valueResult.get()
            }
            return ValidationResult.predicated(map,keyErrors.isEmpty() & valueErrors.isEmpty(), "Errors found deserializing map [$fieldName]: Key Errors = $keyErrors, Value Errors = $valueErrors"
        } catch (e: Exception){
            return ValidationResult.error(defaultValue, "Critical exception encountered during map [$fieldName] deserialization, using default map: ${e.localizedMessage}"
        }
    }

}
