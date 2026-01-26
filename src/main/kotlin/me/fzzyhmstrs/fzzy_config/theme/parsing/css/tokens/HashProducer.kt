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
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.CssType.DELIM
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.CssType.HASH
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.CssType.consumeIdent
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenProducer

object HashProducer: TokenProducer() {

    override fun id(): String {
        return "hash"
    }

    override fun canProduce(reader: StringReader): Boolean {
        return reader.peek() == '#'
    }

    override fun produce(context: ParseContext): Boolean {
        val reader = context.reader()
        val startLine = reader.getLine()
        val startColumn = reader.getColumn()
        val hash = reader.read()
        if (!reader.canRead()) {
            context.token(DELIM, hash.toString(), reader.getLine(), reader.getColumn())
        } else if (CssType.isIdentSequenceStart(reader)) {
            val id = consumeIdent(reader)
            context.token(HASH, id, startLine, startColumn)
        } else {
            context.token(DELIM, hash.toString(), reader.getLine(), reader.getColumn())
        }
        return true
    }
}