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

import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator.ValuesPredicate
import java.util.function.Function
import java.util.function.Predicate

/**
 * a validated set of choices.
 *
 * This is only an EntryValidator, used in Lists and Maps to define the valid new choices you can make
 * @param predicate a [ValuesPredicate] that defines the valid choices the user can make
 * @author fzzyhmstrs
 * since 0.2.0
 */
open class ChoiceValidator<T>(private val  predicate: ValuesPredicate<T>): EntryValidator<T> {

    override fun validateEntry(input: T, type: EntryValidator.ValidationType): ValidationResult<T> {
        return ValidationResult.predicated(input, predicate.test(input), "Value not allowed")
    }

    fun<N> convert(disallowedConverter: Function<T, N>?, allowableConverter: Function<T, N>?): ChoiceValidator<N> {
        return ChoiceValidator(predicate.convert(disallowedConverter, allowableConverter))
    }

    class ValuesPredicate<T>(private val disallowedValues: List<T>?, private val allowableValues: List<T>?): Predicate<T> {
        override fun test(t: T): Boolean {
            return if(disallowedValues != null) {
                if (allowableValues != null) {
                    !disallowedValues.contains(t) && allowableValues.contains(t)
                } else {
                    !disallowedValues.contains(t)
                }
            } else allowableValues?.contains(t) ?: true
        }
        fun<N> convert(disallowedConverter: Function<T, N>?, allowableConverter: Function<T, N>?): ValuesPredicate<N> {
            return if(disallowedValues == null) {
                if(allowableValues == null) {
                    ValuesPredicate(null, null)
                } else {
                    if (allowableConverter == null) throw IllegalStateException("Allowable converter null")
                    ValuesPredicate(null, allowableValues.map { allowableConverter.apply(it) })
                }
            } else {
                if(allowableValues == null) {
                    if (disallowedConverter == null) throw IllegalStateException("Disallowed converter null")
                    ValuesPredicate(disallowedValues.map { disallowedConverter.apply(it) }, null)
                } else {
                    if (allowableConverter == null) throw IllegalStateException("Allowable converter null")
                    if (disallowedConverter == null) throw IllegalStateException("Disallowed converter null")
                    ValuesPredicate(disallowedValues.map { disallowedConverter.apply(it) }, allowableValues.map { allowableConverter.apply(it) })
                }
            }
        }
    }

    companion object {
        fun <T> any(): ChoiceValidator<T> {
            return ChoiceValidator(ValuesPredicate(null, null))
        }
    }
}