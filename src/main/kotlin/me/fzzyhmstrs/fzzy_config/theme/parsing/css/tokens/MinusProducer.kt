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
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.CssType.CDC
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.CssType.DELIM
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.CssType.isIdentSequenceStart
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.CssType.isValidNumber
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenProducer

object MinusProducer: TokenProducer() {

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
        if (isValidNumber(reader)) {
            DigitProducer.produce(context)
        } else if (reader.canRead(3) && reader.peek(1) == '-' && reader.peek(2) == '>') {
            reader.skip()
            reader.skip()
            reader.skip()
            context.token(CDC, startLine, startColumn)
        } else if (isIdentSequenceStart(reader)) {
            IdentProducer.produce(context)
        } else {
            context.token(DELIM, reader.read().toString(), startLine, startColumn, "Delimiter - found")
        }
        return true
    }
}