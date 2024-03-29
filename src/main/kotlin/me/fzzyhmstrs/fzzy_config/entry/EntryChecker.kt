package me.fzzyhmstrs.fzzy_config.entry

import me.fzzyhmstrs.fzzy_config.util.ValidationResult

/**
 * wrapper interface for 2 basic validation interfaces
 * - validate updates
 * - correct errors
 * @param T the non-null type of the Entry stored value
 * @author fzzyhmstrs
 * @since 0.2.0
 */
interface EntryChecker<T: Any>: EntryValidator<T>, EntryCorrector<T> {

    class Impl<T: Any>(private val validator: EntryValidator<T>, private val corrector: EntryCorrector<T>):
        EntryChecker<T> {
        override fun validateEntry(input: T, type: EntryValidator.ValidationType): ValidationResult<T> {
            return validator.validateEntry(input, type)
        }
        override fun correctEntry(input: T, type: EntryValidator.ValidationType): ValidationResult<T> {
            return corrector.correctEntry(input, type)
        }
    }

    companion object {
        fun <T: Any> any(): EntryChecker<T> {
            return Impl({ s, _ -> ValidationResult.success(s)}, { s, _ -> ValidationResult.success(s)})
        }
    }


}