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

/**
 * wrapper interface for 2 basic validation interfaces
 * - validate updates
 * - correct errors
 * @param T the non-null type of the Entry stored value
 * @author fzzyhmstrs
 * @since 0.2.0
 */
interface EntryChecker<T>: EntryValidator<T>, EntryCorrector<T> {

    class Impl<T>(private val validator: EntryValidator<T>, private val corrector: EntryCorrector<T>):
        EntryChecker<T> {
        override fun validateEntry(input: T, type: EntryValidator.ValidationType): ValidationResult<T> {
            return validator.validateEntry(input, type)
        }
        override fun correctEntry(input: T, type: EntryValidator.ValidationType): ValidationResult<T> {
            return corrector.correctEntry(input, type)
        }
    }

    companion object {
        fun <T> any(): EntryChecker<T> {
            return Impl({ s, _ -> ValidationResult.success(s)}, { s, _ -> ValidationResult.success(s)})
        }
    }


}