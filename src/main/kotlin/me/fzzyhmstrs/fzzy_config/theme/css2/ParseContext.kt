/*
 * Copyright (c) 2025 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.css2

import me.fzzyhmstrs.fzzy_config.theme.css2.parser.StringReader
import me.fzzyhmstrs.fzzy_config.theme.css2.token.TokenValue
import me.fzzyhmstrs.fzzy_config.theme.css2.token.TokenType
import me.fzzyhmstrs.fzzy_config.theme.css2.token.Token

abstract class ParseContext {
    abstract fun reader(): StringReader
    abstract fun token(token: Token<*>)

    fun token(type: TokenType) {
        token(Token.unit(type))
    }

    fun <T: Any> token(type: TokenType, valueType: TokenValue<T>, value: T) {
        token(Token(type, valueType, value))
    }

    fun <T: Any> token(type: TokenType, valueType: TokenValue<T>, value: T, line: Int, column: Int, message: String) {
        token(Token(type, valueType, value, line, column, message))
    }
}