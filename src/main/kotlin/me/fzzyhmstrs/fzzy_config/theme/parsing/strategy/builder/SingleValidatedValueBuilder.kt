package me.fzzyhmstrs.fzzy_config.theme.parsing.strategy.builder

import me.fzzyhmstrs.fzzy_config.theme.parsing.css.Errors
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenQueue
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenType
import me.fzzyhmstrs.fzzy_config.util.ValidationResult

open class SingleValidatedValueBuilder<T: Any, V: Any>(private val type: TokenType<V>, private val converter: (V) -> ValidationResult<T?>): ValueBuilder<T> {

    override fun build(input: TokenQueue): ValidationResult<T?> {
        if (!input.canPoll()) return Errors.endOfQueue(null)
        val t = input.peek()
        if (t.type != type) {
            return Errors.invalidToken(null, type, t.type)
        }
        val v = t.value(type) as V
        val result = converter(v)
        if (result.isValid()) input.poll() //commit the read on success
        return result
    }
}