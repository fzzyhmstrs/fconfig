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
 * For example, in a [ValidatedList][me.fzzyhmstrs.fzzy_config.validation.list.ValidatedList], individual new additions need to be validated, and validation of the entire list will take place as a piece-wise validation of each element, to preserve as much of the valid contents as possible
 *
 * SAM: [validateEntry] takes an input of type T and a [ValidationType], returns a [ValidationResult]<T>
 * @param T the non-null type of the Entry stored value
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@FunctionalInterface
fun interface EntryValidator<T> {
    /**
     * Validates an input per the provided validation type.
     * - [ValidationType.WEAK] the validator should not interact with game state; this validation is being requested before the game is fully set up.
     * - [ValidationType.STRONG] validation is happening in-game, so can make use of game state as needed.
     * @param input Input object to validate
     * @param type The current [ValidationType]
     * @return [ValidationResult]&lt;[T]&gt; the input wrapped with either success or failure plus applicable error message, if any. If validation fails, the error should attempt to indicate how validation can be achieved. For example, in a number range 1 to 10, if the user enters 12 the message could be something like "12 outside the valid range 1 to 10"
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun validateEntry(input: T, type: ValidationType): ValidationResult<T>

    /**
     * Builder subclass for this validator. Builds a validator from predicates for weak and strong validation circumstances.
     * @author fzzyhmstrs
     * @since 0.2.0
     */
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
            ifStrong = EntryValidator { i, _ ->
                if (predicate.test(i)) ValidationResult.success(i) else ValidationResult.error(i, ValidationResult.Errors.BASIC, errorMsg)
            }
            return builder()
        }
        fun weak(validator: EntryValidator<T>): E {
            ifWeak = validator
            return builder()
        }
        fun weak(predicate: Predicate<T>, errorMsg: String = "Problem validating Entry!"): E {
            ifWeak = EntryValidator { i, _ ->
                if (predicate.test(i)) ValidationResult.success(i) else ValidationResult.error(i, ValidationResult.Errors.BASIC, errorMsg)
            }
            return builder()
        }
        fun both(validator: EntryValidator<T>): E {
            ifStrong = validator
            ifWeak = validator
            return builder()
        }
        fun both(predicate: Predicate<T>, errorMsg: String = "Problem validating Entry!"): E {
            ifStrong = EntryValidator { i, _ ->
                if (predicate.test(i)) ValidationResult.success(i) else ValidationResult.error(i, ValidationResult.Errors.BASIC, errorMsg)
            }
            ifWeak = EntryValidator { i, _ ->
                if (predicate.test(i)) ValidationResult.success(i) else ValidationResult.error(i, ValidationResult.Errors.BASIC, errorMsg)
            }
            return builder()
        }
        fun buildValidator(): EntryValidator<T> {
            return EntryValidator{ i, t -> if(t == ValidationType.WEAK) ifWeak.validateEntry(i, t) else ifStrong.validateEntry(i, t) }
        }
    }

    /**
     * Validation strategy relative to game state.
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    enum class ValidationType {
        /**
         * The game is not prepared yet, validation should not interact with game state.
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        WEAK,
        /**
         * The game is running, validation can use game state as needed.
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        STRONG
    }
}