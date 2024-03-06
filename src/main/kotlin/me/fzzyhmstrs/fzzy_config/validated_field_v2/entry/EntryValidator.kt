package me.fzzyhmstrs.fzzy_config.validated_field_v2.entry

import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import java.util.function.Predicate

/**
 * Validates individual entries in a complex [ValidatedField].
 *
 * For example, in a [ValidatedList], individual new additions need to be validated, and validation of the entire list will take place as a piece-wise validation of each element, to preserve as much of the valid contents as possible
 *
 * SAM: [validate] takes an input of type T, returns a [ValidationResult]<T>
 * @author fzzyhmstrs
 * @since 0.2.0
 */
fun interface EntryValidator<T> {
    fun validate(input: T, type: ValidationType): ValidationResult<T>

    class Builder<T>{
        private var ifStrong: EntryValidator<T> = EntryValidator{ i, t -> ValidationResult.success(i) }
        private var ifWeak: EntryValidator<T> = EntryValidator{ i, t -> ValidationResult.success(i) }
        fun strong(validator: EntryValidator<T>): Builder<T>{
            ifStrong = validator
            return this
        }
        fun strong(predicate: Predicate<T>, errorMsg: String = "Problem validating Entry!"): Builder<T> {
            ifStrong = EntryValidator { i -> if (predicate.test(i) ValidationResult.success(i) else ValidationResult.error(i, errorMsg) }
            return this
        }
        fun weak(validator: EntryValidator<T>): Builder<T>{
            ifWeak = validator
            return this
        }
        fun weak(predicate: Predicate<T>, errorMsg: String = "Problem validating Entry!"): Builder<T> {
            ifWeak = EntryValidator { i -> if (predicate.test(i) ValidationResult.success(i) else ValidationResult.error(i, errorMsg) }
            return this
        }
        fun build(): EntryValidator<T>{
            return EntryValidator{ i, t -> if(t == ValidationType.WEAK) ifWeak.validate(i,t) else ifStrong(i,t) }
        }
    }
}
