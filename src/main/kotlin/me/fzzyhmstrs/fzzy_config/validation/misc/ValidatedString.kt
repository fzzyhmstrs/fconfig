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
import me.fzzyhmstrs.fzzy_config.screen.widget.SuggestionBackedTextFieldWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.ValidationBackedTextFieldWidget
import me.fzzyhmstrs.fzzy_config.util.AllowableStrings
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.wrap
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedString.Builder
import net.minecraft.client.gui.widget.ClickableWidget
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral
import net.peanuuutz.tomlkt.asTomlLiteral
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.function.Supplier

/**
 * A validated string value
 *
 * Ensure you don't actually want another string-like Validation, such as
 * - [me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedIdentifier]
 * - [me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedEnum]
 * - [me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedChoice]
 *
 * [See the Wiki](https://moddedmc.wiki/en/project/fzzy-config/docs/config-concepts/validation/Strings) for more details and examples.
 * @param defaultValue String default for the setting
 * @param checker [EntryChecker] defining validation and correction for the string inputs. If the provided checker is an AllowableStrings, this will show suggestions in its text field widget.
 * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedMiscExamples.strings
 * @see Builder
 * @author fzzyhmstrs
 * @since 0.2.0
 */
open class ValidatedString(defaultValue: String, private val checker: EntryChecker<String>): ValidatedField<String>(defaultValue) {

    /**
     * A validated string value validated with Regex
     * @param defaultValue String - the default string for this setting
     * @param regex String - the Regex pattern to match against
     * @throws IllegalStateException If the regex can't match to the default input
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    constructor(defaultValue: String, regex: String): this(defaultValue, object : EntryChecker<String> {
        private val re = Regex(regex)
        init {
            if (!re.matches(defaultValue)) throw IllegalStateException("Default value [$defaultValue] doesn't match to supplied regex [$regex]")
        }
        override fun correctEntry(input: String, type: EntryValidator.ValidationType): ValidationResult<String> {
            val newVal = re.findAll(input).map { it.value }.joinToString("")
            return validateEntry(input, type).wrap(newVal)
        }
        override fun validateEntry(input: String, type: EntryValidator.ValidationType): ValidationResult<String> {
            return ValidationResult.predicated(input, re.matches(input), "String doesn't meet regex [$regex]")
        }

        override fun toString(): String {
            return "RegexChecker[pattern=$regex]"
        }
    })

    /**
     * An unbounded validated string value
     *
     * Any string value will be permissible, so this ValidatedField will primarily validate de/serialization.
     * @param defaultValue String, the default string for this setting
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

    @Internal
    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<String> {
        return try {
            ValidationResult.success(toml.asTomlLiteral().toString())
        } catch (e: Throwable) {
            ValidationResult.error(storedValue, "Critical error deserializing string [$fieldName]: ${e.localizedMessage}")
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
    //client
    override fun widgetEntry(choicePredicate: ChoiceValidator<String>): ClickableWidget {
        return if (checker !is AllowableStrings)
            ValidationBackedTextFieldWidget(110, 20, this, choicePredicate, this, this)
        else
            try {
                SuggestionBackedTextFieldWidget(110, 20, this, choicePredicate, this, this, { s, cursor, choiceValidator -> checker.getSuggestions(s, cursor, choiceValidator) }, false)
            } catch (e: Throwable) {
                throw IllegalStateException("Entry Checker provided to Validated String [${getEntryKey()}] is a EntrySuggester of type other than String")
            }
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
        return input is String && validateEntry(input, EntryValidator.ValidationType.STRONG).isValid()
    }

    /**
     * Copies the provided input as deeply as possible. For immutables like numbers and booleans, this will simply return the input
     * @param input String input to be copied
     * @return copied output
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    override fun copyValue(input: String): String {
        return String(input.toCharArray())
    }

    /**
     * @suppress
     */
    override fun toString(): String {
        return "Validated String[value=$storedValue, validation=$checker]"
    }

    companion object {
        /**
         * Validated string based on a list of allowable strings
         *
         * Default value will be the first string in the list.
         * @param strings [List]$lt;String&gt; - the list of allowable string values, can't be empty.
         * @throws IllegalStateException Passed list can't be empty.
         * @author fzzyhmstrs
         * @since 0.2.6
         */
        @JvmStatic
        fun fromList(strings: List<String>): ValidatedString {
            return ValidatedString(try{ strings[0] } catch (e: Throwable) { throw IllegalStateException("List passed to ValidatedString can't be empty.") }, AllowableStrings({s -> strings.contains(s)}, { strings }))
        }

        /**
         * Validated string based on a list of allowable strings, with a provided default value
         * @param defaultValue String - the default string value, must be in the strings list
         * @param strings [List]$lt;String&gt; - the list of allowable string values, can't be empty.
         * @throws IllegalStateException Passed list can't be empty; default value must be in the strings list.
         * @author fzzyhmstrs
         * @since 0.2.6
         */
        @JvmStatic
        fun fromList(defaultValue: String, strings: List<String>): ValidatedString {
            if (!strings.contains(defaultValue)) throw IllegalStateException("List passed to ValidatedString doesn't contain the default value [$defaultValue].")
            if (strings.isEmpty()) throw IllegalStateException("List passed to ValidatedString can't be empty.")
            return ValidatedString(defaultValue, AllowableStrings({s -> strings.contains(s)}, { strings }))
        }

        /**
         * Validated string based on a list of allowable strings, with a provided default value
         *
         * NOTE: the default value provided should be present in the supplied list at some point (doesn't have to be at launch). Otherwise, the default value shown will be immediately invalid on use. Since the list is supplied, this can't be checked up front when list validation may be weak.
         * @param defaultValue String - the default string value, must be in the strings list
         * @param strings [List]$lt;String&gt; - the list of allowable string values, can't be empty.
         * @author fzzyhmstrs
         * @since 0.2.6
         */
        @JvmStatic
        fun fromList(defaultValue: String, strings: Supplier<List<String>>): ValidatedString {
            return ValidatedString(defaultValue, AllowableStrings({s -> strings.get().contains(s)}, strings))
        }

        /**
         * Validated string based on a collection of allowable strings
         *
         * Default value will be the first string in the collection.
         * @param strings vararg String - the collection of allowable string values. Can be empty; will return a blank validation in this case that has no default and accepts any valid string.
         * @author fzzyhmstrs
         * @since 0.4.0
         */
        @JvmStatic
        fun fromValues(vararg strings: String): ValidatedString {
            if (strings.isEmpty()) return ValidatedString()
            val list = strings.toList()
            return fromList(list)
        }
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
        fun validator(validator: EntryValidator<String>): BuilderWithValidator {
            return BuilderWithValidator(defaultValue, validator)
        }
        fun withCorrector(): BuilderWithValidator {
            return BuilderWithValidator(defaultValue, this.buildValidator())
        }
        class BuilderWithValidator internal constructor(private val defaultValue: String, private val validator: EntryValidator<String>): EntryCorrector.AbstractBuilder<String, BuilderWithValidator>() {
            override fun builder(): BuilderWithValidator {
                return this
            }
            fun corrector(corrector: EntryCorrector<String>): ValidatedString {
                return ValidatedString(defaultValue, EntryChecker.Impl(validator, corrector))
            }
            fun build(): ValidatedString {
                val corrector = this.buildCorrector()
                return ValidatedString(corrector.correctEntry(defaultValue, EntryValidator.ValidationType.STRONG).get(), EntryChecker.Impl(validator, corrector))
            }
        }
    }




}