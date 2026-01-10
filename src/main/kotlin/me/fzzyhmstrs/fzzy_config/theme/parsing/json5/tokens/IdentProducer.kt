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
import java.lang.StringBuilder

object IdentProducer: TokenProducer() {

    override fun id(): String {
        return "Boolean"
    }

    override fun canProduce(reader: StringReader): Boolean {
        return Json5Type.isIdentStart(reader) && !Json5Type.isIdentReserved(reader)
    }

    override fun produce(context: ParseContext): Boolean {
        val reader = context.reader()
        val ln = reader.getLine()
        val col = reader.getColumn()
        val builder = StringBuilder()
        builder.append(reader.read()) //consume the ident start
        while (reader.canRead()) {
            if (Json5Type.isIdentPart(reader)) {
                builder.append(reader.read())
            } else if (reader.peek() != ':' && !reader.peek().isWhitespace()) {
                context.token(Token.unit(Json5Type.BAD_IDENT, ln, col, "Illegal identifier character ${reader.peek()}"))
                return true
            } else {
                break
            }
        }
        context.token(Token(Json5Type.IDENT, builder.toString(), ln, col))
        return true
    }

}