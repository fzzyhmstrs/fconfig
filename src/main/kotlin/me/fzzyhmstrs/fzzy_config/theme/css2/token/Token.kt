/*
 * Copyright (c) 2025 Fzzyhmstrs
 *
 * This file is part of Imbued Sorcery, a mod made for minecraft; as such it falls under the license of Imbued Sorcery.
 *
 * Imbued Sorcery is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.css2.token

import me.fzzyhmstrs.fzzy_config.theme.css2.test.Parser.NOTHING_VALUE

/**
 * A single "unit" of proto-parsing
 * @param type this defines "what" token it is, without having to have 5000 inherited ProtoToken subclasses. This is used by token producers to ask for certain types in some order
 * @param valueType defines the type that will be returned by this pt. used in concert with [type] to a) get the proper token "class" and b) make sure it's got the value type you need. This would be mostly primitives and so on, NUMBER, STRING, BOOLEAN, etc., but also JSON, etc.
 * @param value the "bit" of data the pt is storing.
 * @author fzzyhmstrs
 * @since ?.?.?
 */
data class Token<T: Any>(val type: TokenType, val valueType: TokenValue<T>, val value: T, val error: ProtoTokenError = ProtoTokenError.EMPTY) {
    constructor(type: TokenType, valueType: TokenValue<T>, value: T, line: Int, column: Int, message: String): this(type, valueType, value, ProtoTokenError(line, column, message))

    override fun toString(): String {
        return if (value == Unit) {
            if (error != ProtoTokenError.EMPTY) {
                "Token($type | $error)"
            } else {
                "Token($type)"
            }
        } else {
            if (error != ProtoTokenError.EMPTY) {
                "Token($type > $valueType : $value | $error)"
            } else {
                "Token($type > $valueType : $value)"
            }
        }
    }

    companion object {
        fun unit(type: TokenType): Token<Unit> {
            return Token(type, NOTHING_VALUE, Unit)
        }
    }
}