package me.fzzyhmstrs.fzzy_config.theme.parsing.strategy.builder

import me.fzzyhmstrs.fzzy_config.theme.parsing.css.Errors
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenQueue
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenType
import me.fzzyhmstrs.fzzy_config.util.ValidationResult

open class SingleValueBuilder<T: Any, V: Any>(
    private val type: TokenType<V>,
    private val converter: (V) -> ValidationResult<T?>,
    private val fallback: T?): ValueBuilder<T> {

    override fun build(input: TokenQueue): ValidationResult<T?> {
        if (!input.canPoll()) {
            if (fallback != null) {
                return ValidationResult.success(fallback)
            }
            return Errors.endOfQueue(null)
        }
        val t = input.peek()
        if (t.type != type) {
            if (fallback != null) {
                return ValidationResult.success(fallback)
            }
            return Errors.invalidToken(null, type, t.type)
        }
        val v = t.value(type) as V
        val result = converter(v)
        if (result.isValid()) input.poll() //commit the read on success
        return result
    }

    class Builder<T: Any, V: Any> @Deprecated("Use create method") internal constructor (private val type: TokenType<V>) {
        private var converter: ((V) -> ValidationResult<T?>)? = null
        private var fallback: T? = null

        fun converter(converter: (V) -> ValidationResult<T?>): Builder<T, V> {
            this.converter = converter
            return this
        }

        fun safeConverter(converter: (V) -> T): Builder<T, V> {
            this.converter = {v -> ValidationResult.success(converter(v)) }
            return this
        }

        fun fallback(fallback: T): Builder<T, V> {
            this.fallback = fallback
            return this
        }

        fun build(): SingleValueBuilder<T, V> {
            if (converter == null) throw IllegalStateException("SingleValueBuilder needs a value converter")
            return SingleValueBuilder(type, converter!!, fallback)
        }
    }

    companion object {
        fun <T: Any, V: Any> create(type: TokenType<V>): SingleValueBuilder.Builder<T, V> {
            return Builder(type)
        }
    }
}