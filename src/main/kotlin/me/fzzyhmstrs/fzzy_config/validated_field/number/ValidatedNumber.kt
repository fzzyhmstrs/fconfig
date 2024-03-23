package me.fzzyhmstrs.fzzy_config.validated_field.number

import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import me.fzzyhmstrs.fzzy_config.validated_field.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.validated_field.ValidatedField

sealed class ValidatedNumber<T>(defaultValue: T, protected val minValue: T, protected val maxValue: T): ValidatedField<T>(defaultValue) where T: Number, T:Comparable<T> {

    override fun correctEntry(input: T, type: EntryValidator.ValidationType): ValidationResult<T> {
        if(input < minValue)
            return ValidationResult.error(minValue, "Validated number [$input] below the valid range [$minValue] to [$maxValue]")
        else if(input < minValue)
            return ValidationResult.error(maxValue, "Validated number [$input] above the valid range [$minValue] to [$maxValue]")
        return ValidationResult.success(input)
    }

    override fun validateEntry(input: T, type: EntryValidator.ValidationType): ValidationResult<T> {
        if(input < minValue)
            return ValidationResult.error(input, "Validated number [$input] below the valid range [$minValue] to [$maxValue]")
        else if(input < minValue)
            return ValidationResult.error(input, "Validated number [$input] above the valid range [$minValue] to [$maxValue]")
        return ValidationResult.success(input)
    }

    /**
     * Determines which type of selector widget will be used for the Number selection
     * @sample [SLIDER]
     * @sample [TEXTBOX]
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    enum class WidgetType{
        /**
         * An MC-style slider widget bounded between min and max.
         */
        SLIDER,

        /**
         * A textbox-style widget that lets you enter the number directly, throwing error if outside of range
         *
         * Will be automatically used if the "simple" constructor is used to make an unbounded number
         */
        TEXTBOX
    }
}