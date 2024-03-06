package me.fzzyhmstrs.fzzy_config.validated_field_v2.number

import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import net.minecraft.client.gui.widget.Widget
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral
import net.peanuuutz.tomlkt.asTomlLiteral
import net.peanuuutz.tomlkt.toFloat

class ValidatedFloat(defaultValue: Float, maxValue: Float, minValue: Float): ValidatedNumber<Float>(defaultValue, minValue, maxValue) {

    override fun copyStoredValue(): Float {
        return storedValue
    }

    override fun deserializeEntry(toml: TomlElement, fieldName: String): ValidationResult<Float> {
        return try{
            ValidationResult.success(toml.asTomlLiteral().toFloat())
        } catch (e: Exception){
            ValidationResult.error(defaultValue,"Problem deserializing ValidatedInt [$fieldName]: ${e.localizedMessage}")
        }
    }

    override fun serializeEntry(input: T): TomlElement {
        return TomlLiteral(input)
    }

    override fun createWidget(): Widget {
        TODO("Not yet implemented")
    }
}
