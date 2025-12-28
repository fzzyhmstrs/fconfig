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
import me.fzzyhmstrs.fzzy_config.theme.css2.token.TokenType
import me.fzzyhmstrs.fzzy_config.theme.css2.token.Token

abstract class ParseContext {
    abstract fun reader(): StringReader
    abstract fun token(token: Token<*>)

    fun token(type: TokenType<Unit>, line: Int, column: Int) {
        token(Token.unit(type, line, column))
    }

    fun token(type: TokenType<Unit>, line: Int, column: Int, message: String) {
        token(Token.unit(type, line, column, message))
    }

    fun <T: Any> token(type: TokenType<T>, value: T, line: Int, column: Int) {
        token(Token(type, value, line, column))
    }

    fun <T: Any> token(type: TokenType<T>, value: T, line: Int, column: Int, message: String) {
        token(Token(type, value, line, column, message))
    }
}