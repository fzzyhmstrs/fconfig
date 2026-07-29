package me.fzzyhmstrs.fzzy_config.theme.parsing.builder

import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenQueue
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.map

interface ValueBuilder<T> {
    fun build(input: TokenQueue): ValidationResult<T?>
    fun length(): Int = 1

    fun <N: Any> map(mapper: (T?) -> N?): ValueBuilder<N> {
        return object : ValueBuilder<N> {
            override fun build(input: TokenQueue): ValidationResult<N?> {
                return this@ValueBuilder.build(input).map(mapper)
            }
        }
    }

    fun or(other: ValueBuilder<T>): ValueBuilder<T> {
        return OrValueBuilder(this, other)
    }
}