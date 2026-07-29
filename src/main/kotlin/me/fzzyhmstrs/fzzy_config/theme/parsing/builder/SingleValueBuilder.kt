/*
 * Copyright (c) 2026 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.parsing.builder

import me.fzzyhmstrs.fzzy_config.theme.parsing.css.Errors
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenQueue
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenType
import me.fzzyhmstrs.fzzy_config.util.ValidationResult

open class SingleValueBuilder<T: Any, V: Any>(
    private val type: List<TokenType<V>>,
    private val converter: (V) -> ValidationResult<T?>,
    private val fallback: T?,
    private val length: Int): ValueBuilder<T> {

    override fun build(input: TokenQueue): ValidationResult<T?> {
        if (!input.canPoll()) {
            if (fallback != null) {
                return ValidationResult.success(fallback)
            }
            return Errors.endOfQueue(null)
        }
        val t = input.peek()
        if (type.none { typ -> typ == t.type }) {
            if (fallback != null) {
                return ValidationResult.success(fallback)
            }
            return Errors.invalidToken(null, type, t.type)
        }
        @Suppress("UNCHECKED_CAST")
        val v = t.value as V
        val result = converter(v)
        if (result.isValid()) input.poll() //commit the read on success
        return result
    }

    class Builder<T: Any, V: Any> @Deprecated("Use create method") internal constructor (vararg type: TokenType<V>) {
        private val types: MutableList<TokenType<V>> = mutableListOf(*type)
        private var converter: ((V) -> ValidationResult<T?>)? = null
        private var fallback: T? = null
        private var length: Int = 1

        fun converter(converter: (V) -> ValidationResult<T?>): Builder<T, V> {
            this.converter = converter
            return this
        }

        fun safeConverter(converter: (V) -> T): Builder<T, V> {
            this.converter = {v -> ValidationResult.success(converter(v)) }
            return this
        }

        fun fallback(fallback: T?): Builder<T, V> {
            this.fallback = fallback
            return this
        }

        fun length(length: Int): Builder<T, V> {
            this.length = length
            return this
        }

        fun build(): SingleValueBuilder<T, V> {
            if (converter == null) throw IllegalStateException("SingleValueBuilder needs a value converter")
            return SingleValueBuilder(types, converter!!, fallback, length)
        }
    }

    override fun length(): Int {
        return length
    }

    companion object {
        fun <T: Any, V: Any> create(vararg type: TokenType<V>): Builder<T, V> {
            @Suppress("DEPRECATION")
            return Builder(*type)
        }
    }
}