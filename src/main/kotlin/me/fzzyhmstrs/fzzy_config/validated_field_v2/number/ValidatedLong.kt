package me.fzzyhmstrs.fzzy_config.validated_field_v2.number

import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import me.fzzyhmstrs.fzzy_config.validated_field_v2.entry.Entry
import net.minecraft.client.gui.widget.ClickableWidget
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral
import net.peanuuutz.tomlkt.asTomlLiteral
import net.peanuuutz.tomlkt.toLong

class ValidatedLong(defaultValue: Long, maxValue: Long, minValue: Long): ValidatedNumber<Long>(defaultValue, minValue, maxValue) {

    override fun copyStoredValue(): Long {
        return storedValue
    }

    override fun deserializeEntry(toml: TomlElement, fieldName: String): ValidationResult<Long> {
        return try{
            ValidationResult.success(toml.asTomlLiteral().toLong())
        } catch (e: Exception){
            ValidationResult.error(defaultValue,"Problem deserializing ValidatedLong [$fieldName]: ${e.localizedMessage}")
        }
    }

    override fun serializeEntry(input: Long): TomlElement {
        return TomlLiteral(input)
    }

    override fun instanceEntry(): Entry<Long> {
        return ValidatedLong(defaultValue, maxValue, minValue)
    }

    override fun widgetEntry(): ClickableWidget {
        TODO("Not yet implemented")
    }
}