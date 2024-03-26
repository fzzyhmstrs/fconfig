package me.fzzyhmstrs.fzzy_config.validation.misc

import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.validation.entry.Entry
import me.fzzyhmstrs.fzzy_config.validation.entry.EntryValidator
import net.minecraft.client.gui.widget.ClickableWidget
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral
import net.peanuuutz.tomlkt.asTomlLiteral
import net.peanuuutz.tomlkt.toBoolean

/**
 * a validated boolean value
 * Since there is basically nothing to validate on a boolean, this primarily serves to parse and correct issues with de/serialization.
 * @param defaultValue the default boolean state
 * @see [me.fzzyhmstrs.fzzy_config.validation.misc.Shorthand.validated]
 * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedMiscExamples.validatedBool]
 * @author fzzyhmstrs
 * since 0.1.0
 */
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
