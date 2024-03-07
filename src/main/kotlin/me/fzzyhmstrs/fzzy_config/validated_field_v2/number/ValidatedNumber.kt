package me.fzzyhmstrs.fzzy_config.validated_field_v2.number

import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import me.fzzyhmstrs.fzzy_config.validated_field_v2.ValidatedField
import me.fzzyhmstrs.fzzy_config.validated_field_v2.entry.EntryValidator

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
}