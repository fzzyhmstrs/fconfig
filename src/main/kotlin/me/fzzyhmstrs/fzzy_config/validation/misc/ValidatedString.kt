package me.fzzyhmstrs.fzzy_config.validation.misc

import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.validation.entry.*
import net.minecraft.client.gui.widget.ClickableWidget
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral
import net.peanuuutz.tomlkt.asTomlLiteral

/**
 * A validated string value
 *
 * Ensure you don't actually want another string-like Validation before use, such as [me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedIdentifier] or [me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedEnum]
 * @param defaultValue String default for the setting
 * @param checker [EntryChecker] defining validation and correction for the string inputs.
 * @see [Builder]
 * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedMiscExamples.validatedString]
 * @author fzzyhmstrs
 * @since 0.2.0
 */
class ValidatedString(defaultValue: String, private val checker: EntryChecker<String>): ValidatedField<String>(defaultValue) {

    /**
     *
     */
    constructor(defaultValue: String): this(defaultValue, EntryChecker.any())

    override fun copyStoredValue(): String {
        return String(storedValue.toCharArray())
    }

    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<String> {
        return try {
            ValidationResult.success(toml.asTomlLiteral().toString())
        } catch (e: Exception){
            ValidationResult.error(storedValue,"Critical error deserializing string [$fieldName]: ${e.localizedMessage}")
        }
    }

    override fun serialize(input: String): ValidationResult<TomlElement> {
        return ValidationResult.success(TomlLiteral(input))
    }

    override fun correctEntry(input: String, type: EntryValidator.ValidationType): ValidationResult<String> {
        return ValidationResult.success(input)
    }

    override fun validateEntry(input: String, type: EntryValidator.ValidationType): ValidationResult<String> {
        return ValidationResult.success(input)
    }

    override fun widgetEntry(): ClickableWidget {
        TODO("Not yet implemented")
    }

    override fun instanceEntry(): Entry<String> {
        return ValidatedString(String(defaultValue.toCharArray()), this.checker)
    }

    /*
     * A validated string builder, integrated with an EntryChecker builder
     * @param defaultValue the default String value
     * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedMiscExamples.validatedString]
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    class Builder(private val defaultValue: String): EntryValidator.AbstractBuilder<String, Builder>() {
        override fun builder(): Builder {
            return this
        }
        fun validator(validator: EntryValidator<String>): BuilderWithValidator{
            return BuilderWithValidator(defaultValue,validator)
        }
        fun withCorrector(): BuilderWithValidator{
            return BuilderWithValidator(defaultValue,this.buildValidator())
        }
        class BuilderWithValidator internal constructor(private val defaultValue: String, private val validator: EntryValidator<String>): EntryCorrector.AbstractBuilder<String,BuilderWithValidator>() {
            override fun builder(): BuilderWithValidator {
                return this
            }
            fun corrector(corrector: EntryCorrector<String>): ValidatedString{
                return ValidatedString(defaultValue,EntryChecker.Impl(validator,corrector))
            }
            fun build(): ValidatedString{
                return ValidatedString(defaultValue,EntryChecker.Impl(validator,this.buildCorrector()))
            }
        }
    }


}