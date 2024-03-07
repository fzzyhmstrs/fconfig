package me.fzzyhmstrs.fzzy_config.validated_field_v2.number

import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import me.fzzyhmstrs.fzzy_config.validated_field_v2.entry.Entry
import net.minecraft.client.gui.widget.ClickableWidget
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral
import net.peanuuutz.tomlkt.asTomlLiteral
import net.peanuuutz.tomlkt.toShort

class ValidatedShort(defaultValue: Short, maxValue: Short, minValue: Short): ValidatedNumber<Short>(defaultValue, minValue, maxValue) {

    override fun copyStoredValue(): Short {
        return storedValue
    }

    override fun deserializeEntry(toml: TomlElement, fieldName: String): ValidationResult<Short> {
        return try{
            ValidationResult.success(toml.asTomlLiteral().toShort())
        } catch (e: Exception){
            ValidationResult.error(defaultValue,"Problem deserializing ValidatedShort [$fieldName]: ${e.localizedMessage}")
        }
    }

    override fun serializeEntry(input: Short): TomlElement {
        return TomlLiteral(input)
    }

    override fun instanceEntry(): Entry<Short> {
        return ValidatedShort(defaultValue, maxValue, minValue)
    }

    override fun widgetEntry(): ClickableWidget {
        TODO("Not yet implemented")
    }
}