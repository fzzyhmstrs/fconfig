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
import me.fzzyhmstrs.fzzy_config.theme.css2.test.CssType
import me.fzzyhmstrs.fzzy_config.theme.css2.test.CssType.BAD_STRING
import me.fzzyhmstrs.fzzy_config.theme.css2.test.CssType.STRING
import me.fzzyhmstrs.fzzy_config.theme.css2.token.TokenProducer

object StringProducer: TokenProducer() {

    override fun id(): String {
        return "string"
    }

    override fun canProduce(reader: StringReader): Boolean {
        return (reader.peek() == '"' || reader.peek() == '\'')
    }

    override fun produce(context: ParseContext): Boolean {
        val reader = context.reader()
        val startColumn = reader.getColumn()
        val startLine = reader.getLine()
        val builder = StringBuilder()
        val open = reader.read() //consume the opening " or '
        var solidusFound = false
        while(reader.canRead()) {
            val c = reader.read()
            if (solidusFound) {
                solidusFound = false
                if (c == '\n') {
                    continue
                }
                builder.append(CssType.consumeEscape(c, reader))
                continue
            }
            when (c) {
                '"', '\'' -> {
                    if (c == open) {
                        context.token(STRING, builder.toString(), startLine, startColumn)
                        return true
                    } else {
                        builder.append(c)
                    }
                }
                '\n' -> {
                    context.token(BAD_STRING, builder.toString(), startColumn, startLine, "Illegal newline")
                    return true
                }
                '\u005C' -> {
                    solidusFound = true
                }
                else -> {
                    builder.append(c)
                }
            }
        }
        context.token(BAD_STRING, builder.toString(), startColumn, startLine, "Unterminated string value; EOF reached")
        return true
    }

}