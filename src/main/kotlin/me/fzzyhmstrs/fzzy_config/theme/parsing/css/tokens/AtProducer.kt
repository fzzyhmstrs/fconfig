/*
 * Copyright (c) 2025 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.parsing.css.tokens

import me.fzzyhmstrs.fzzy_config.theme.parsing.ParseContext
import me.fzzyhmstrs.fzzy_config.theme.parsing.parser.StringReader
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.CssType.AT
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.CssType.DELIM
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.CssType.consumeIdent
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.CssType.isIdentSequenceStart
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenProducer

object AtProducer: TokenProducer() {

    override fun id(): String {
        return "at"
    }

    override fun canProduce(reader: StringReader): Boolean {
        return reader.peek() == '@'
    }

    override fun produce(context: ParseContext): Boolean {
        val reader = context.reader()
        val startColumn = reader.getColumn()
        val startLine = reader.getLine()
        val at = reader.read()
        if (isIdentSequenceStart(reader)) {
            val ident = consumeIdent(reader)
            context.token(AT, ident, startLine, startColumn)
        } else {
            context.token(DELIM, at.toString(), startLine, startColumn, "Delimiter @ found")
        }
        return true
    }
}