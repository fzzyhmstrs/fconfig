package me.fzzyhmstrs.fzzy_config.validated_field_v2.map

import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import me.fzzyhmstrs.fzzy_config.api.ValidationResult.Companion.also
import me.fzzyhmstrs.fzzy_config.validated_field_v2.ValidatedField
import me.fzzyhmstrs.fzzy_config.validated_field_v2.entry.Entry
import me.fzzyhmstrs.fzzy_config.validated_field_v2.entry.EntryCorrector
import me.fzzyhmstrs.fzzy_config.validated_field_v2.entry.EntryValidator
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.asTomlTable
import java.util.regex.Pattern

class ValidatedMap<V: Any>(defaultValue: Map<String,V>, private val keyHandler: Entry<String>, private val valueHandler: Entry<V>): ValidatedField<Map<String,V>>(defaultValue) {

    private val keyValidator: EntryValidator<String> = EntryValidator { input, type ->
        keyHandler.validateEntry(input, type).also(validatorPattern.matcher(input).matches(),"Illegal characters in Map Key, needs to be [a-z0-9_-]")
    }

    private val keyCorrector: EntryCorrector<String> = EntryCorrector { input, type ->
        val firstTest = validatorPattern.matcher(input).matches()
        var characterError = ""
        if (!firstTest){
            val updatedInput = input.replace(replacerRegex,"_")
            characterError = "Invalid characters found. [$input] corrected to []"
        }
        keyHandler.validateEntry(input, type).also(validatorPattern.matcher(input).matches(),"Illegal characters in Map Key, needs to be [a-z0-9_-]")
    }
    val validatorPattern = Pattern.compile("[a-z0-9_-]")
    val replacerRegex = Pattern.compile("((?![a-z0-9_-]).)").toRegex()

    override fun deserializeEntry(toml: TomlElement, fieldName: String): ValidationResult<Map<String, V>> {
        return try {
            val table = toml.asTomlTable()
            val map: MutableMap<String,V> = mutableMapOf()
            val keyErrors: MutableList<String> = mutableListOf()
            val valueErrors: MutableList<String> = mutableListOf()
            for ((key, el) in table.entries){
                val keyResult = keyHandler.validateEntry()
            }
        } catch (e: Exception){

        }
    }

}