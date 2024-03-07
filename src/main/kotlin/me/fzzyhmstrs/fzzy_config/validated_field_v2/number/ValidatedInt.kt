package me.fzzyhmstrs.fzzy_config.validated_field_v2.number

import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import me.fzzyhmstrs.fzzy_config.validated_field_v2.entry.Entry
import net.minecraft.client.gui.widget.ClickableWidget
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral
import net.peanuuutz.tomlkt.asTomlLiteral
import net.peanuuutz.tomlkt.toInt

class ValidatedInt(defaultValue: Int, maxValue: Int, minValue: Int): ValidatedNumber<Int>(defaultValue, minValue, maxValue) {

    override fun copyStoredValue(): Int {
        return storedValue
    }

    override fun deserializeEntry(toml: TomlElement, fieldName: String): ValidationResult<Int> {
        return try{
            ValidationResult.success(toml.asTomlLiteral().toInt())
        } catch (e: Exception){
            ValidationResult.error(defaultValue,"Problem deserializing ValidatedInt [$fieldName]: ${e.localizedMessage}")
        }
    }

    override fun serializeEntry(input: Int): TomlElement {
        return TomlLiteral(input)
    }

    override fun instanceEntry(): Entry<Int> {
        return ValidatedInt(defaultValue, maxValue, minValue)
    }

    override fun widgetEntry(): ClickableWidget {
        TODO("Not yet implemented")
    }
}