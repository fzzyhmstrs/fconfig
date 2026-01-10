/*
 * Copyright (c) 2025 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.parsing.json5.tokens

import me.fzzyhmstrs.fzzy_config.theme.parsing.ParseContext
import me.fzzyhmstrs.fzzy_config.theme.parsing.json5.Json5Type
import me.fzzyhmstrs.fzzy_config.theme.parsing.parser.StringReader
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.Token
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenProducer

object NullProducer: TokenProducer() {

    override fun id(): String {
        return "Null"
    }

    override fun canProduce(reader: StringReader): Boolean {
        return reader.peekFor("null")
    }

    override fun produce(context: ParseContext): Boolean {
        val ln = context.reader().getLine()
        val col = context.reader().getColumn()
        context.reader().skip(4)
        context.token(Token.unit(Json5Type.NULL, ln, col))
        return true
    }
}