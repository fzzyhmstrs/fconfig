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

import me.fzzyhmstrs.fzzy_config.theme.parsing.ParseContext
import me.fzzyhmstrs.fzzy_config.theme.parsing.parser.StringReader

abstract class TokenProducer {
    abstract fun id(): String
    abstract fun canProduce(reader: StringReader): Boolean
    abstract fun produce(context: ParseContext): Boolean //did the production finish

    override fun toString(): String {
        return "Producer(${id()})"
    }

    open class SingleChar(private val type: TokenType<Char>, private val char: Char, private val id: String): TokenProducer() {

        override fun id(): String {
            return id
        }

        override fun canProduce(reader: StringReader): Boolean {
            return reader.peek() == char
        }

        override fun produce(context: ParseContext): Boolean {
            val reader = context.reader()
            val startLine = reader.getLine()
            val startColumn = reader.getColumn()
            reader.skip()
            context.token(type, char, startLine, startColumn)
            return true
        }
    }
}