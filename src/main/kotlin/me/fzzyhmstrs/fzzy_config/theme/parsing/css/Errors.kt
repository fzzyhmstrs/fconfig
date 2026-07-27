/*
 * Copyright (c) 2026 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.parsing.css

import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenType
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.ErrorEntry.Type

object Errors {
    val CRITICAL = Type<String>("Fatal Parsing Error")
    val UNKNOWN_RULE = Type<String>("Unknown or Errored Input")
    val INVALID_AT = Type<String>("At-rule in Invalid Context")
    val INVALID_DECL = Type<String>("Invalid or Unknown Declaration")
    val END_OF_QUEUE = Type<String>("Unexpected End of Token Queue")
    val INVALID_TOKEN = Type<String>("Invalid Token Type")
    val UPPERCASE = Type<String>("Identifier with Improper Uppercase Letters")

    fun <T> endOfQueue(value: T, message: String = ""): ValidationResult<T> {
        return ValidationResult.error(value, END_OF_QUEUE, message)
    }

    fun <T> invalidToken(value: T, expected: TokenType<*>, got: TokenType<*>): ValidationResult<T> {
        return ValidationResult.error(value, INVALID_TOKEN, "Expected $expected, got $got")
    }

    fun <T> invalidToken(value: T, expected: List<TokenType<*>>, got: TokenType<*>): ValidationResult<T> {
        return ValidationResult.error(value, INVALID_TOKEN, "Expected one of $expected, got $got")
    }

    fun <T> uppercase(value: T, ident: String): ValidationResult<T> {
        return ValidationResult.error(value, UPPERCASE, ident)
    }

    fun ValidationResult<*>.cssCritical(): Boolean {
        return this.has(CRITICAL)
    }
}