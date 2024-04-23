/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.validation.misc

import me.fzzyhmstrs.fzzy_config.entry.EntryChecker
import me.fzzyhmstrs.fzzy_config.entry.EntryCorrector
import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.screen.widget.ValidationBackedTextFieldWidget
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.wrap
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedString.Builder
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.widget.ClickableWidget
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral
import net.peanuuutz.tomlkt.asTomlLiteral
import org.jetbrains.annotations.ApiStatus.Internal

/**
 * A validated string value
 *
 * Ensure you don't actually want another string-like Validation, such as
 * - [me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedIdentifier]
 * - [me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedEnum]
 * - [me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedChoice]
 * @param defaultValue String default for the setting
 * @param checker [EntryChecker] defining validation and correction for the string inputs.
 * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedMiscExamples.strings
 * @see Builder
 * @author fzzyhmstrs
 * @since 0.2.0
 */
open class ValidatedString(defaultValue: String, private val checker: EntryChecker<String>): ValidatedField<String>(defaultValue) {

    /**
     * A validated string value validated with Regex
     *
     * Any string value will be permissible, so this ValidatedField will primarily validate de/serialization.
     * @param defaultValue String, the default string for this setting
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    constructor(defaultValue: String, regex: String): this(defaultValue, object : EntryChecker<String>{
        private val re = Regex(regex)
        override fun correctEntry(input: String, type: EntryValidator.ValidationType): ValidationResult<String> {
            val newVal = re.findAll(input).map { it.value }.joinToString("")
            return validateEntry(input, type).wrap(newVal)
        }
        override fun validateEntry(input: String, type: EntryValidator.ValidationType): ValidationResult<String> {
            return ValidationResult.predicated(input,re.matches(input),"String doesn't meet regex [$regex]")
        }

        override fun toString(): String {
            return "RegexChecker[pattern=$regex]"
        }
    })

    /**
     * An unbounded validated string value
     *
     * Any string value will be permissible, so this ValidatedField will primarily validate de/serialization.
     * @param defaultValue String, the efault string for this setting
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    constructor(defaultValue: String): this(defaultValue, EntryChecker.any())

    /**
     * An unbounded validated string value with empty default value
     *
     * Any string value will be permissible, so this ValidatedField will primarily validate de/serialization.
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    constructor(): this("", EntryChecker.any())

    /**
     * Creates a deep copy of the stored value and returns it
     * @return String - deep copy of the currently stored value
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    override fun copyStoredValue(): String {
        return String(storedValue.toCharArray())
    }
    @Internal
    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<String> {
        return try {
            ValidationResult.success(toml.asTomlLiteral().toString())
        } catch (e: Exception){
            ValidationResult.error(storedValue,"Critical error deserializing string [$fieldName]: ${e.localizedMessage}")
        }
    }
    @Internal
    override fun serialize(input: String): ValidationResult<TomlElement> {
        return ValidationResult.success(TomlLiteral(input))
    }
    @Internal
    override fun correctEntry(input: String, type: EntryValidator.ValidationType): ValidationResult<String> {
        return checker.correctEntry(input, type)
    }
    @Internal
    override fun validateEntry(input: String, type: EntryValidator.ValidationType): ValidationResult<String> {
        return checker.validateEntry(input, type)
    }
    @Internal
    @Environment(EnvType.CLIENT)
    override fun widgetEntry(choicePredicate: ChoiceValidator<String>): ClickableWidget {
        return ValidationBackedTextFieldWidget(110,20,this, choicePredicate,this,this)
    }

    /**
     * creates a deep copy of this ValidatedString
     * @return ValidatedString wrapping a deep copy of the currently stored string and this validations checker.
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    override fun instanceEntry(): ValidatedString {
        return ValidatedString(copyStoredValue(), this.checker)
    }
    @Internal
    override fun isValidEntry(input: Any?): Boolean {
        return input is String && validateEntry(input,EntryValidator.ValidationType.STRONG).isValid()
    }

    /**
     * @suppress
     */
    override fun toString(): String {
        return "Validated String[value=$storedValue, validation=$checker]"
    }

    /**
     * A validated string builder, integrated with an [EntryChecker] builder
     * @param defaultValue the default String value
     * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedMiscExamples.strings
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
                return ValidatedString(defaultValue, EntryChecker.Impl(validator,corrector))
            }
            fun build(): ValidatedString{
                return ValidatedString(defaultValue, EntryChecker.Impl(validator,this.buildCorrector()))
            }
        }
    }




}