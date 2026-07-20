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
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.CssType
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.CssType.BAD_NUMBER
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.CssType.NUMBER
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.CssType.NUMBER_DIMENSION
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.CssType.NUMBER_PERCENTAGE
import me.fzzyhmstrs.fzzy_config.theme.parsing.parser.Parser
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.Token
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenProducer

object DigitProducer: TokenProducer() {

    override fun id(): String {
        return "digit"
    }

    override fun canProduce(reader: StringReader): Boolean {
        return reader.peek().isDigit()
    }

    override fun produce(context: ParseContext): Boolean {
        val reader = context.reader()
        val startColumn = reader.getColumn()
        val startLine = reader.getLine()
        val result = CssType.consumeNumber(reader)
        val token = if (CssType.isIdentSequenceStart(reader)) {
            val unit = CssType.consumeIdent(reader)
            Token(NUMBER_DIMENSION, CssType.NumberWithUnitValue(result.number, unit), startLine, startColumn)
        } else if (reader.canRead() && reader.peek() == '%') {
            reader.skip()
            Token(NUMBER_PERCENTAGE, CssType.NumberWithUnitValue(result.number, "%"), startLine, startColumn)
        } else {
            Token(NUMBER, Parser.NumberValue(result.number), startLine, startColumn)
        }
        context.token (if (result.isError()) {
            Token(BAD_NUMBER, token, startLine, startColumn, result.error)
        } else {
            token
        })
        return true
    }
}