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
import me.fzzyhmstrs.fzzy_config.theme.css2.test.CssType.CDO
import me.fzzyhmstrs.fzzy_config.theme.css2.test.CssType.DELIM
import me.fzzyhmstrs.fzzy_config.theme.css2.token.TokenProducer

object LessThanProducer: TokenProducer() {

    override fun id(): String {
        return "minus"
    }

    override fun canProduce(reader: StringReader): Boolean {
        return reader.peek() == '-'
    }

    override fun produce(context: ParseContext): Boolean {
        val reader = context.reader()
        val startColumn = reader.getColumn()
        val startLine = reader.getLine()
        val lessThan = reader.read()
        if (reader.canRead(3) && reader.peek() == '!' && reader.peek(1) == '-' && reader.peek(2) == '-') {
            reader.skip()
            reader.skip()
            reader.skip()
            context.token(CDO, startLine, startColumn)
        } else {
            context.token(DELIM, lessThan.toString(), startLine, startColumn, "Delimiter < found")
        }
        return true
    }
}