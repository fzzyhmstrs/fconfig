package me.fzzyhmstrs.fzzy_config.validated_field.number

import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import me.fzzyhmstrs.fzzy_config.validated_field.entry.Entry
import net.minecraft.client.gui.widget.ClickableWidget
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral
import net.peanuuutz.tomlkt.asTomlLiteral
import net.peanuuutz.tomlkt.toFloat

class ValidatedFloat(defaultValue: Float, maxValue: Float, minValue: Float): ValidatedNumber<Float>(defaultValue, minValue, maxValue) {

    override fun copyStoredValue(): Float {
        return storedValue
    }

    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<Float> {
        return try{
            ValidationResult.success(toml.asTomlLiteral().toFloat())
        } catch (e: Exception){
            ValidationResult.error(defaultValue,"Problem deserializing ValidatedInt [$fieldName]: ${e.localizedMessage}")
        }
    }

    override fun serialize(input: Float): ValidationResult<TomlElement> {
        return ValidationResult.success(TomlLiteral(input))
    }

    override fun instanceEntry(): Entry<Float> {
        return ValidatedFloat(defaultValue, maxValue, minValue)
    }

    override fun widgetEntry(): ClickableWidget {
        TODO("Not yet implemented")
    }
}