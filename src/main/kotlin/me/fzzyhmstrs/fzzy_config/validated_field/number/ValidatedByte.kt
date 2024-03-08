package me.fzzyhmstrs.fzzy_config.validated_field.number

import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import me.fzzyhmstrs.fzzy_config.validated_field.entry.Entry
import net.minecraft.client.gui.widget.ClickableWidget
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral
import net.peanuuutz.tomlkt.asTomlLiteral
import net.peanuuutz.tomlkt.toByte

class ValidatedByte(defaultValue: Byte, maxValue: Byte, minValue: Byte): ValidatedNumber<Byte>(defaultValue, minValue, maxValue) {

    override fun copyStoredValue(): Byte {
        return storedValue
    }

    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<Byte> {
        return try{
            ValidationResult.success(toml.asTomlLiteral().toByte())
        } catch (e: Exception){
            ValidationResult.error(defaultValue,"Problem deserializing ValidatedByte [$fieldName]: ${e.localizedMessage}")
        }
    }

    override fun serialize(input: Byte): ValidationResult<TomlElement> {
        return ValidationResult.success(TomlLiteral(input))
    }

    override fun instanceEntry(): Entry<Byte> {
        return ValidatedByte(defaultValue, maxValue, minValue)
    }

    override fun widgetEntry(): ClickableWidget {
        TODO("Not yet implemented")
    }
}