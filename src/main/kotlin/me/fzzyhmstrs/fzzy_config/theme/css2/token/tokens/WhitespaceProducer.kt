/*
 * Copyright (c) 2025 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.css2.token.tokens

import me.fzzyhmstrs.fzzy_config.theme.css2.ParseContext
import me.fzzyhmstrs.fzzy_config.theme.css2.parser.StringReader
import me.fzzyhmstrs.fzzy_config.theme.css2.test.CssType.WHITESPACE
import me.fzzyhmstrs.fzzy_config.theme.css2.token.TokenProducer

object WhitespaceProducer: TokenProducer() {

    override fun id(): String {
        return "whitespace"
    }

    override fun canProduce(reader: StringReader): Boolean {
        return reader.peek().isWhitespace()
    }

    override fun produce(context: ParseContext): Boolean {
        val reader = context.reader()
        val startLine = reader.getLine()
        val startColumn = reader.getColumn()
        var whitespace = ""
        while(reader.canRead() && reader.peek().isWhitespace()) {
            whitespace += reader.read()
        }
        context.token(WHITESPACE, whitespace, startLine, startColumn)
        return true
    }
}