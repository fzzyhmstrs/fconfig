package me.fzzyhmstrs.fzzy_config.validated_field_v2.number

import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import me.fzzyhmstrs.fzzy_config.validated_field_v2.entry.Entry
import net.minecraft.client.gui.widget.ClickableWidget
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral
import net.peanuuutz.tomlkt.asTomlLiteral
import net.peanuuutz.tomlkt.toDouble

class ValidatedDouble(defaultValue: Double, maxValue: Double, minValue: Double): ValidatedNumber<Double>(defaultValue, minValue, maxValue) {

    override fun copyStoredValue(): Double {
        return storedValue
    }

    override fun deserializeEntry(toml: TomlElement, fieldName: String): ValidationResult<Double> {
        return try{
            ValidationResult.success(toml.asTomlLiteral().toDouble())
        } catch (e: Exception){
            ValidationResult.error(defaultValue,"Problem deserializing ValidatedDouble [$fieldName]: ${e.localizedMessage}")
        }
    }

    override fun serializeEntry(input: Double): TomlElement {
        return TomlLiteral(input)
    }

    override fun instanceEntry(): Entry<Double> {
        return ValidatedDouble(defaultValue, maxValue, minValue)
    }

    override fun widgetEntry(): ClickableWidget {
        TODO("Not yet implemented")
    }
}