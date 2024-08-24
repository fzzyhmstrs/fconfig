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

import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import java.util.function.Predicate

/**
 * Deserializes individual entries in a complex [Entry]
 *
 * SAM: [correctEntry] takes a TomlElement, returns a deserialized instance of T
 * @param T the type of the Entry stored value
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@FunctionalInterface
fun interface EntryCorrector<T> {
    fun correctEntry(input: T, type: EntryValidator.ValidationType): ValidationResult<T>

    class Builder<T:Any>: AbstractBuilder<T, Builder<T>>() {
        override fun builder(): Builder<T> {
            return this
        }
    }

    abstract class AbstractBuilder<T: Any, E: AbstractBuilder<T, E>> {
        private var ifStrong: EntryCorrector<T> = EntryCorrector{ i, t -> ValidationResult.success(i) }
        private var ifWeak: EntryCorrector<T> = EntryCorrector{ i, t -> ValidationResult.success(i) }
        protected abstract fun builder(): E
        fun strong(corrector: EntryCorrector<T>): E {
            ifStrong = corrector
            return builder()
        }
        fun strong(predicate: Predicate<T>, errorMsg: String = "Problem validating Entry!"): E {
            ifStrong = EntryCorrector { i, t -> if (predicate.test(i)) ValidationResult.success(i) else ValidationResult.error(i, errorMsg) }
            return builder()
        }
        fun weak(corrector: EntryCorrector<T>): E {
            ifWeak = corrector
            return builder()
        }
        fun weak(predicate: Predicate<T>, errorMsg: String = "Problem validating Entry!"): E {
            ifWeak = EntryCorrector { i, t -> if (predicate.test(i)) ValidationResult.success(i) else ValidationResult.error(i, errorMsg) }
            return builder()
        }
        fun both(corrector: EntryCorrector<T>): E {
            ifStrong = corrector
            ifWeak = corrector
            return builder()
        }
        fun both(predicate: Predicate<T>, errorMsg: String = "Problem validating Entry!"): E {
            ifStrong = EntryCorrector { i, t -> if (predicate.test(i)) ValidationResult.success(i) else ValidationResult.error(i, errorMsg) }
            ifWeak = EntryCorrector { i, t -> if (predicate.test(i)) ValidationResult.success(i) else ValidationResult.error(i, errorMsg) }
            return builder()
        }
        fun buildCorrector(): EntryCorrector<T> {
            return EntryCorrector{ i, t -> if(t == EntryValidator.ValidationType.WEAK) ifWeak.correctEntry(i, t) else ifStrong.correctEntry(i, t) }
        }
    }
}