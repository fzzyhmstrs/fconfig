/*
 * Copyright (c) 2025 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.css2.token.tokens2

import me.fzzyhmstrs.fzzy_config.theme.css2.ParseContext
import me.fzzyhmstrs.fzzy_config.theme.css2.parser.StringReader
import me.fzzyhmstrs.fzzy_config.theme.css2.test.CssType
import me.fzzyhmstrs.fzzy_config.theme.css2.test.CssType.DELIM
import me.fzzyhmstrs.fzzy_config.theme.css2.test.CssType.HASH
import me.fzzyhmstrs.fzzy_config.theme.css2.test.CssType.consumeIdent
import me.fzzyhmstrs.fzzy_config.theme.css2.test.Parser.STRING_VALUE
import me.fzzyhmstrs.fzzy_config.theme.css2.token.Token
import me.fzzyhmstrs.fzzy_config.theme.css2.token.TokenProducer

object HashProducer: TokenProducer() {

    override fun id(): String {
        return "hash"
    }

    override fun canProduce(reader: StringReader): Boolean {
        return reader.peek() == '#'
    }

    override fun produce(context: ParseContext) {
        val reader = context.reader()
        val hash = reader.read()
        if (!reader.canRead()) {
            context.token(DELIM, STRING_VALUE, hash.toString(), reader.getLine(), reader.getColumn(), "Delimiter # found")
        } else if (CssType.isIdentSequenceStart(reader)) {
            val id = consumeIdent(reader)
            context.token(HASH, STRING_VALUE, id)
        } else {
            context.token(DELIM, STRING_VALUE, hash.toString(), reader.getLine(), reader.getColumn(), "Delimiter # found")
        }
    }
}