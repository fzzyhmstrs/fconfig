package me.fzzyhmstrs.fzzy_config.validated_field_v2.number

import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import net.minecraft.client.gui.widget.Widget
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

    override fun serializeEntry(input: T): TomlElement {
        return TomlLiteral(input)
    }

    override fun createWidget(): Widget {
        TODO("Not yet implemented")
    }

    override fun translationKey(): String {
        return "validated.fallback.byte"
    }

    override fun descriptionKey(): String {
        return "validated.fallback.byte.desc"
    }

}
