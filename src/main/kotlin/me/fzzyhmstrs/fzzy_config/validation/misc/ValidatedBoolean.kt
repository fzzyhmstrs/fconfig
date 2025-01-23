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

import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomButtonWidget
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.validation.Shorthand.validated
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import net.minecraft.client.gui.widget.ClickableWidget
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral
import net.peanuuutz.tomlkt.asTomlLiteral
import net.peanuuutz.tomlkt.toBoolean
import org.jetbrains.annotations.ApiStatus.Internal

/**
 * a validated boolean value
 *
 * Since there is basically nothing to validate on a boolean, this primarily serves to parse and correct issues with de/serialization.
 * @param defaultValue the default boolean state
 * @see me.fzzyhmstrs.fzzy_config.validation.Shorthand.validated
 * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedMiscExamples.booleans
 * @author fzzyhmstrs
 * since 0.1.0
 */
open class ValidatedBoolean(defaultValue: Boolean): ValidatedField<Boolean>(defaultValue) {

    /**
     * A validated boolean value wth  default 'true' value
     *
     * Since there is basically nothing to validate on a boolean, this primarily serves to parse and correct issues with de/serialization.
     * @see [me.fzzyhmstrs.fzzy_config.validation.Shorthand.validated]
     * @author fzzyhmstrs
     * since 0.2.0
     */
    constructor(): this(true)

    @Internal
    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<Boolean> {
        return try {
            ValidationResult.success(toml.asTomlLiteral().toBoolean())
        } catch (e: Throwable) {
            ValidationResult.error(storedValue, "Critical error deserializing boolean [$fieldName]: ${e.localizedMessage}")
        }
    }

    @Internal
    override fun serialize(input: Boolean): ValidationResult<TomlElement> {
        return ValidationResult.success(TomlLiteral(input))
    }

    /**
     * creates a deep copy of this ValidatedBoolean
     * return ValidatedBoolean wrapping the current boolean value
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    override fun instanceEntry(): ValidatedBoolean {
        return ValidatedBoolean(copyStoredValue())
    }

    @Internal
    override fun isValidEntry(input: Any?): Boolean {
        return input is Boolean
    }

    @Internal
    //client
    override fun widgetEntry(choicePredicate: ChoiceValidator<Boolean>): ClickableWidget {
        return CustomButtonWidget.builder { setAndUpdate(!get()) }.size(110, 20).messageSupplier { if(get()) "fc.validated_field.boolean.true".translate() else "fc.validated_field.boolean.false".translate() }.build()
    }

    /**
     * @suppress
     */
    override fun toString(): String {
        return "Validated Boolean[value=$storedValue, validation=true or false]"
    }
}