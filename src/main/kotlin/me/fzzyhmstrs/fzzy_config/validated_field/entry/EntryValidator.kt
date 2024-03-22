package me.fzzyhmstrs.fzzy_config.validated_field.entry

import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import me.fzzyhmstrs.fzzy_config.validated_field.ValidatedField
import java.util.function.Predicate

/**
 * Validates individual entries in a complex [ValidatedField].
 *
 * For example, in a [ValidatedList], individual new additions need to be validated, and validation of the entire list will take place as a piece-wise validation of each element, to preserve as much of the valid contents as possible
 *
 * SAM: [validateEntry] takes an input of type T, returns a [ValidationResult]<T>
 * @author fzzyhmstrs
 * @since 0.2.0
 */
fun interface EntryValidator<T: Any> {
    fun validateEntry(input: T, type: ValidationType): ValidationResult<T>

    class Builder<T: Any>{
        private var ifStrong: EntryValidator<T> = EntryValidator{ i, t -> ValidationResult.success(i) }
        private var ifWeak: EntryValidator<T> = EntryValidator{ i, t -> ValidationResult.success(i) }
        fun strong(validator: EntryValidator<T>): Builder<T> {
            ifStrong = validator
            return this
        }
        fun strong(predicate: Predicate<T>, errorMsg: String = "Problem validating Entry!"): Builder<T> {
            ifStrong = EntryValidator { i, t -> if (predicate.test(i)) ValidationResult.success(i) else ValidationResult.error(i, errorMsg) }
            return this
        }
        fun weak(validator: EntryValidator<T>): Builder<T> {
            ifWeak = validator
            return this
        }
        fun weak(predicate: Predicate<T>, errorMsg: String = "Problem validating Entry!"): Builder<T> {
            ifWeak = EntryValidator { i, t -> if (predicate.test(i)) ValidationResult.success(i) else ValidationResult.error(i, errorMsg) }
            return this
        }
        fun build(): EntryValidator<T> {
            return EntryValidator{ i, t -> if(t == ValidationType.WEAK) ifWeak.validateEntry(i,t) else ifStrong.validateEntry(i,t) }
        }
    }

    enum class ValidationType{
        WEAK,
        STRONG
    }
}