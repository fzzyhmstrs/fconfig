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

object StringProducer: TokenProducer() {

    override fun id(): String {
        return "String"
    }

    override fun canProduce(reader: StringReader): Boolean {
        return (reader.peek() == '"' || reader.peek() == '\'')
    }

    override fun produce(context: ParseContext): Boolean {
        val reader = context.reader()
        val startColumn = reader.getColumn()
        val startLine = reader.getLine()
        val open = reader.read() //consume the opening " or '
        val stringBuilder = StringBuilder()
        var escaped = false
        while (reader.canRead()) {
            val c = reader.peek()
            if (escaped) {
                escaped = false
                if (!Json5Type.consumeEscape(c, reader, stringBuilder)) {
                    context.token(Token(Json5Type.BAD_STRING, stringBuilder.toString(), startLine, startColumn, "Illegal escape code at ln${reader.getLine()}/col${reader.getColumn()}"))
                    return true
                }
                continue
            }
            when (c) {
                '\\' -> {
                    reader.skip()
                    escaped = true
                    continue
                }
                '"', '\'' -> {
                    reader.skip()
                    if (c == open) {
                        context.token(Token(Json5Type.STRING, stringBuilder.toString(), startLine, startColumn))
                        return true
                    } else {
                        stringBuilder.append(c)
                    }
                }
                else -> {
                    reader.skip()
                    stringBuilder.append(c)
                }
            }
        }
        if (escaped) {
            return false //not finished a valid multi-line string
        }
        context.token(Token(Json5Type.BAD_STRING, stringBuilder.toString(), startLine, startColumn, "Unclosed string found at ln${reader.getLine()}/col${reader.getColumn()}"))
        return true
    }
}