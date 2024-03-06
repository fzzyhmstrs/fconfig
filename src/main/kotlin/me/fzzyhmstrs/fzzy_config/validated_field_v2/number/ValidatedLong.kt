package me.fzzyhmstrs.fzzy_config.validated_field_v2.number

import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import net.minecraft.client.gui.widget.Widget
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral
import net.peanuuutz.tomlkt.asTomlLiteral
import net.peanuuutz.tomlkt.toLong

class ValidatedLong(defaultValue: Long, maxValue: Long, minValue: Long): ValidatedNumber<Long>(defaultValue, minValue, maxValue) {

    override fun copyStoredValue(): Long {
        return storedValue
    }

    override fun deserializeHeldValue(toml: TomlElement, fieldName: String): ValidationResult<Long> {
        return try{
            ValidationResult.success(toml.asTomlLiteral().toLong())
        } catch (e: Exception){
            ValidationResult.error(defaultValue,"Problem deserializing ValidatedInt [$fieldName]: ${e.localizedMessage}")
        }
    }

    override fun serializeHeldValue(): TomlElement {
        return TomlLiteral(storedValue)
    }

    override fun createWidget(): Widget {
        TODO("Not yet implemented")
    }

    override fun translationKey(): String {
        return "validated.fallback.long"
    }

    override fun descriptionKey(): String {
        return "validated.fallback.long.desc"
    }


}