/*
 * Copyright (c) 2025 Fzzyhmstrs
 *
 * This file is part of Imbued Sorcery, a mod made for minecraft; as such it falls under the license of Imbued Sorcery.
 *
 * Imbued Sorcery is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.parsing.token

import me.fzzyhmstrs.fzzy_config.theme.parsing.ParsePrinter
import java.util.function.Consumer

/**
 * A single "unit" of proto-parsing
 * @param type this defines "what" token it is, without having to have 5000 inherited ProtoToken subclasses. This is used by token producers to ask for certain types in some order
 * @param valueType defines the type that will be returned by this pt. used in concert with [type] to a) get the proper token "class" and b) make sure it's got the value type you need. This would be mostly primitives and so on, NUMBER, STRING, BOOLEAN, etc., but also JSON, etc.
 * @param value the "bit" of data the pt is storing.
 * @author fzzyhmstrs
 * @since ?.?.?
 */
class Token<T: Any> private constructor(val type: TokenType<T>, val value: T, private val info: TokenInfo = TokenInfo.EMPTY): ParsePrinter {
    constructor(type: TokenType<T>, value: T, line: Int, column: Int): this(type, value, TokenInfo(line, column, ""))
    constructor(type: TokenType<T>, value: T, line: Int, column: Int, message: String): this(type, value, TokenInfo(line, column, message))

    fun line(): Int {
        return info.line
    }

    fun column(): Int {
        return info.column
    }

    fun message(): String {
        return info.error
    }

    fun asString(): String {
        return type.createValue(if (value == Unit) type.raw() else value.toString())
    }

    fun <V: Any> value(type: TokenType<V>): V? {
        return if (type == this.type) value as? V else null
    }

    override fun toString(): String {
        return if (value == Unit) {
            if (info != TokenInfo.EMPTY) {
                "Token($type | $info)"
            } else {
                "Token($type)"
            }
        } else {
            if (info != TokenInfo.EMPTY) {
                "Token($type : $value | $info)"
            } else {
                "Token($type : $value)"
            }
        }
    }

    override fun print(printer: Consumer<String>) {
        printer.accept("Token($type | $info)")
        if (value is ParsePrinter) {
            value.print { s -> printer.accept("  $s") }
        } else if (value != Unit && asString().isNotBlank()) {
            printer.accept(value.toString())
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Token<*>) return false

        if (type != other.type) return false
        if (value != other.value) return false
        if (info != other.info) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + value.hashCode()
        result = 31 * result + info.hashCode()
        return result
    }

    companion object {
        fun unit(type: TokenType<Unit>, line: Int, column: Int): Token<Unit> {
            return Token(type, Unit, line, column)
        }

        fun unit(type: TokenType<Unit>, line: Int, column: Int, message: String): Token<Unit> {
            return Token(type, Unit, line, column, message)
        }
    }
}