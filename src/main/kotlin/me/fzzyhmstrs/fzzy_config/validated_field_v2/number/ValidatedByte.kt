package me.fzzyhmstrs.fzzy_config.validated_field_v2.number

import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import me.fzzyhmstrs.fzzy_config.validated_field_v2.entry.Entry
import net.minecraft.client.gui.widget.ClickableWidget
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral
import net.peanuuutz.tomlkt.asTomlLiteral
import net.peanuuutz.tomlkt.toByte

class ValidatedByte(defaultValue: Byte, maxValue: Byte, minValue: Byte): ValidatedNumber<Byte>(defaultValue, minValue, maxValue) {

    override fun copyStoredValue(): Byte {
        return storedValue
    }

    override fun deserializeEntry(toml: TomlElement, fieldName: String): ValidationResult<Byte> {
        return try{
            ValidationResult.success(toml.asTomlLiteral().toByte())
        } catch (e: Exception){
            ValidationResult.error(defaultValue,"Problem deserializing ValidatedByte [$fieldName]: ${e.localizedMessage}")
        }
    }

    override fun serializeEntry(input: Byte): TomlElement {
        return TomlLiteral(input)
    }

    override fun instanceEntry(): Entry<Byte> {
        return ValidatedByte(defaultValue, maxValue, minValue)
    }

    override fun widgetEntry(): ClickableWidget {
        TODO("Not yet implemented")
    }
}