/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.entry

import me.fzzyhmstrs.fzzy_config.entry.EntryValidator.ValidationType
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import java.util.function.Predicate

/**
 * Validates individual entries in a complex [Entry].
 *
 * For example, in a [ValidatedList][me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedList], individual new additions need to be validated, and validation of the entire list will take place as a piece-wise validation of each element, to preserve as much of the valid contents as possible
 *
 * SAM: [validateEntry] takes an input of type T and a [ValidationType], returns a [ValidationResult]<T>
 * @param T the non-null type of the Entry stored value
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@FunctionalInterface
fun interface EntryValidator<T> {
    //TODO
    fun validateEntry(input: T, type: ValidationType): ValidationResult<T>

    //TODO
    class Builder<T: Any>: AbstractBuilder<T, Builder<T>>() {
        override fun builder(): Builder<T> {
            return this
        }
    }

    abstract class AbstractBuilder<T: Any, E: AbstractBuilder<T, E>> {
        private var ifStrong: EntryValidator<T> = EntryValidator{ i, _ -> ValidationResult.success(i) }
        private var ifWeak: EntryValidator<T> = EntryValidator{ i, _ -> ValidationResult.success(i) }

        protected abstract fun builder(): E

        fun strong(validator: EntryValidator<T>): E {
            ifStrong = validator
            return builder()
        }
        fun strong(predicate: Predicate<T>, errorMsg: String = "Problem validating Entry!"): E {
            ifStrong = EntryValidator { i, _ -> if (predicate.test(i)) ValidationResult.success(i) else ValidationResult.error(i, errorMsg) }
            return builder()
        }
        fun weak(validator: EntryValidator<T>): E {
            ifWeak = validator
            return builder()
        }
        fun weak(predicate: Predicate<T>, errorMsg: String = "Problem validating Entry!"): E {
            ifWeak = EntryValidator { i, _ -> if (predicate.test(i)) ValidationResult.success(i) else ValidationResult.error(i, errorMsg) }
            return builder()
        }
        fun both(validator: EntryValidator<T>): E {
            ifStrong = validator
            ifWeak = validator
            return builder()
        }
        fun both(predicate: Predicate<T>, errorMsg: String = "Problem validating Entry!"): E {
            ifStrong = EntryValidator { i, _ -> if (predicate.test(i)) ValidationResult.success(i) else ValidationResult.error(i, errorMsg) }
            ifWeak = EntryValidator { i, _ -> if (predicate.test(i)) ValidationResult.success(i) else ValidationResult.error(i, errorMsg) }
            return builder()
        }
        fun buildValidator(): EntryValidator<T> {
            return EntryValidator{ i, t -> if(t == ValidationType.WEAK) ifWeak.validateEntry(i, t) else ifStrong.validateEntry(i, t) }
        }
    }

    enum class ValidationType {
        WEAK,
        STRONG
    }
}