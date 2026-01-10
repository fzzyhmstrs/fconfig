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
import me.fzzyhmstrs.fzzy_config.theme.parsing.parser.StringReader
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenProducer

object MultiCommentProducer: TokenProducer() {

    override fun id(): String {
        return "multi-line comment"
    }

    override fun canProduce(reader: StringReader): Boolean {
        return reader.peekFor("/*")
    }

    override fun produce(context: ParseContext): Boolean {
        val reader = context.reader()
        var lastRead = ' '
        while (reader.canRead()) {
            val read = reader.read()
            if (lastRead == '*' && read == '/') return true
            lastRead = read
        }
        return false
    }
}