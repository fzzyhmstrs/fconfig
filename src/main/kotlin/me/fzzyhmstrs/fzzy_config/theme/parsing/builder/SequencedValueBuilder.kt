/*
 * Copyright (c) 2026 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

@file:Suppress("DEPRECATION")

package me.fzzyhmstrs.fzzy_config.theme.parsing.builder

import me.fzzyhmstrs.fzzy_config.theme.parsing.css.Errors
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenQueue
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.map
import java.util.*

class SequencedValueBuilder<T: Any, B: Creator<T>> private constructor(private val sequence: List<Sequence<*, T, B>>, private val oneOf: Boolean) {

    fun applyValue(input: TokenQueue, builder: B): ValidationResult<Optional<B>> {
        val b = builder
        var toGo = input.size()

        for (s in sequence) {
            val result = s.sequence(input, b)
            if (result.isError()) return ValidationResult.error(Optional.empty(), Errors.INVALID_DECL) { b -> b.content("Error with declaration sequence").addError(result) }
            if (oneOf) {
                return ValidationResult.predicated(Optional.of(b), toGo == 0, Errors.INVALID_DECL) { b ->
                    val s = StringBuilder()
                    while (input.canPoll()) {
                        s.append(input.poll().asString())
                        if (input.canPoll()) {
                            s.append(", ")
                        }
                    }
                    b.content("Unconsumed entries in declaration sequence: $s")
                }
            }
            toGo -= result.get()
        }
        return ValidationResult.predicated(Optional.of(b), toGo == 0, Errors.INVALID_DECL) { b ->
            val s = StringBuilder()
            while (input.canPoll()) {
                s.append(input.poll().asString())
                if (input.canPoll()) {
                    s.append(", ")
                }
            }
            b.content("Unconsumed entries in declaration sequence: $s")
        }
    }


    @Deprecated("Internal use only")
    class Sequence<T: Any, B: Any, B1: Creator<B>>(private val canSkip: Boolean, private val valueBuilder: ValueBuilder<T>, private val applier: (T, B1) -> Unit) {
        fun sequence(input: TokenQueue, builder: B1): ValidationResult<Int> {
            val s = input.size()
            val result = valueBuilder.build(input)
            if (result.isError()) {
                return if (canSkip) {
                    ValidationResult.success(0)
                } else {
                    result.map { 0 }
                }
            }
            val t = result.get() ?: return ValidationResult.error(0, ValidationResult.Errors.INVALID, "Unexpected null value")
            applier(t, builder)
            return ValidationResult.success(s - input.size())
        }
    }

    @Deprecated("Internal use only")
    class Builder <T: Any, B: Creator<T>> internal constructor() {
        private val sequence: MutableList<Sequence<*, T, B>> = mutableListOf()
        private var oneOf: Boolean = false

        fun <A: Any> sequenceNoSkip(valueBuilder: ValueBuilder<A>, applier: (A, B) -> Unit): Builder<T, B> {
            sequence.add(Sequence(false, valueBuilder, applier))
            return this
        }

        fun <A: Any> sequence(valueBuilder: ValueBuilder<A>, applier: (A, B) -> Unit): Builder<T, B> {
            sequence.add(Sequence(true, valueBuilder, applier))
            return this
        }

        fun oneOf(): Builder<T, B> {
            oneOf = true
            return this
        }

        fun build(): SequencedValueBuilder<T, B> {
            return SequencedValueBuilder(sequence, oneOf)
        }
    }

    companion object {
        fun <T: Any, B: Creator<T>> create(): Builder<T, B> {
            return Builder()
        }
    }
}