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

import me.fzzyhmstrs.fzzy_config.theme.css2.ParseContext
import me.fzzyhmstrs.fzzy_config.theme.css2.parser.StringReader
import me.fzzyhmstrs.fzzy_config.theme.css2.test.Parser.NOTHING_VALUE

abstract class TokenProducer {
    abstract fun id(): String
    abstract fun canProduce(reader: StringReader): Boolean
    abstract fun produce(context: ParseContext)

    override fun toString(): String {
        return "Producer(${id()})"
    }

    open class SingleChar(private val type: TokenType, private val char: Char, private val id: String): TokenProducer() {

        override fun id(): String {
            return id
        }

        override fun canProduce(reader: StringReader): Boolean {
            return reader.peek() == char
        }

        override fun produce(context: ParseContext) {
            context.reader().skip()
            context.token(type, NOTHING_VALUE, Unit)
        }
    }
}