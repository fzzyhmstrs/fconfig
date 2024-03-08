package me.fzzyhmstrs.fzzy_config.validated_field.misc

import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import me.fzzyhmstrs.fzzy_config.validated_field.ValidatedField
import me.fzzyhmstrs.fzzy_config.validated_field.entry.Entry
import me.fzzyhmstrs.fzzy_config.validated_field.entry.EntryValidator
import net.minecraft.client.gui.widget.ClickableWidget
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral
import net.peanuuutz.tomlkt.asTomlLiteral
import net.peanuuutz.tomlkt.toBoolean

class ValidatedBoolean(defaultValue: Boolean): ValidatedField<Boolean>(defaultValue) {

    override fun copyStoredValue(): Boolean {
        return storedValue
    }

    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<Boolean> {
        return try {
            ValidationResult.success(toml.asTomlLiteral().toBoolean())
        } catch (e: Exception){
            ValidationResult.error(storedValue,"Critical error deserializing boolean [$fieldName]: ${e.localizedMessage}")
        }
    }

    override fun serialize(input: Boolean): ValidationResult<TomlElement> {
        return ValidationResult.success(TomlLiteral(input))
    }

    override fun correctEntry(input: Boolean, type: EntryValidator.ValidationType): ValidationResult<Boolean> {
        return ValidationResult.success(input)
    }

    override fun validateEntry(input: Boolean, type: EntryValidator.ValidationType): ValidationResult<Boolean> {
        return ValidationResult.success(input)
    }

    override fun widgetEntry(): ClickableWidget {
        TODO("Not yet implemented")
    }

    override fun instanceEntry(): Entry<Boolean> {
        return ValidatedBoolean(this.defaultValue)
    }
}