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
import me.fzzyhmstrs.fzzy_config.theme.css2.test.CssType.BAD_URL
import me.fzzyhmstrs.fzzy_config.theme.css2.test.CssType.FUNCTION
import me.fzzyhmstrs.fzzy_config.theme.css2.test.CssType.IDENT
import me.fzzyhmstrs.fzzy_config.theme.css2.test.CssType.URL
import me.fzzyhmstrs.fzzy_config.theme.css2.test.CssType.consumeEscape
import me.fzzyhmstrs.fzzy_config.theme.css2.test.CssType.consumeIdent
import me.fzzyhmstrs.fzzy_config.theme.css2.test.CssType.isIdentStartCodePoint
import me.fzzyhmstrs.fzzy_config.theme.css2.test.CssType.isValidEscape
import me.fzzyhmstrs.fzzy_config.theme.css2.token.TokenProducer
import java.lang.StringBuilder

object IdentProducer: TokenProducer() {

    override fun id(): String {
        return "ident"
    }

    override fun canProduce(reader: StringReader): Boolean {
        return isIdentStartCodePoint(reader.peek())
    }

    override fun produce(context: ParseContext): Boolean {
        val reader = context.reader()
        val startLine = reader.getLine()
        val startColumn = reader.getColumn()
        val ident = consumeIdent(context.reader())
        if (ident.lowercase() == "url" && reader.canRead() && reader.peek() == '(') {
            reader.skip()
            reader.skipWhitespace()
            if (!reader.canRead()) {
                context.token(IDENT, ident, startLine, startColumn)
            } else if (reader.canRead() && (reader.peek() == '"' || reader.peek() == '\'')) {
                context.token(FUNCTION, ident, startLine, startColumn)
            } else {
                reader.skipWhitespace()
                val builder = StringBuilder()
                val line = reader.getLine()
                val column = reader.getColumn()
                var bad = -1
                while (reader.canRead()) {
                    when (val c = reader.peek()) {
                        ')' -> {
                            reader.skip()
                            if (bad != -1) {
                                context.token(BAD_URL, line, column, "URL parsing error at ln$line/col$bad")
                            } else {
                                context.token(URL, builder.toString(), startLine, startColumn)
                            }
                            return true
                        }
                        '"', '\'', '(' -> {
                            reader.skip()
                            if (bad == -1)
                                bad = reader.getColumn()
                            continue
                        }
                        '\\' -> {
                            if (isValidEscape(reader)) {
                                reader.skip()
                                builder.append(consumeEscape(reader.read(), reader))
                            } else {
                                reader.skip()
                                if (bad == -1)
                                    bad = reader.getColumn()
                            }
                        }
                        else -> {
                            if (c.isWhitespace()) {
                                reader.skipWhitespace()
                            } else {
                                builder.append(reader.read())
                            }
                        }
                    }
                }
                context.token(BAD_URL, line, column, "Uncaptured URL parsing error at ln$line/col$column")
            }
        } else if (reader.canRead() && reader.peek() == '(') {
            reader.skip()
            context.token(FUNCTION, ident, startLine, startColumn)
        } else {
            context.token(IDENT, ident, startLine, startColumn)
        }
        return true
    }
}