package me.fzzyhmstrs.fzzy_config.validated_field_v2

import me.fzzyhmstrs.fzzy_config.api.StringTranslatable
import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.FcText
import net.minecraft.client.gui.widget.Widget
import net.minecraft.text.Text
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral

class ValidatedExpression(defaultValue: String, private val validator: EntryValidator = EntryValidator{ i, t -> ValidationResult.success(i)}): ValidatedField<String>(defaultValue) {

    private var parsedString = defaultValue
    private var parsedExpression = Expression.parse(defaultValue)
    
    override fun eval(vars: Map<Char,Double>): Double {
        if (parsedString != storedValue) {
            val tryExpression try {
                Expression.parse(storedValue)
            } catch(e: Exception) {
                parsedExpression
            }
            parsedExpression = tryExpression
        }
        return parsedExpression.eval(vars)
    }
  
    override fun deserializeEntry(toml: TomlElement, fieldName: String): ValidationResult<T> {
        return try {
            val string = toml.toString()
            ValidationResult.success(chkId)
        } catch (e: Exception){
            ValidationResult.error(storedValue,"Critical error deserializing math expression [$fieldName]: ${e.localizedMessage}")
        }
    }

    override fun serializeEntry(input: String): TomlElement {
        return TomlLiteral(input)
    }
    override fun validateAndCorrectInputs(input: T, type: ValidationType): ValidationResult<T> {
        val result = validator.validate(input, type)
        return if(result.isError()) {ValidationResult.error(storedValue, "Invalid identifier [$input] found, corrected to [$storedValue]: ${result.getError()}") else result
    }

    override fun validate(input: T, type: ValidationType): ValidationResult<T> {
        return validator.validate(input, type)
    }

    override fun createEntry(name: Text, desc: Text): ConfigEntry {
        TODO("Not yet implemented")
    }
}
