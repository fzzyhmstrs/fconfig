/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.css2.parser

import me.fzzyhmstrs.fzzy_config.theme.css2.CascadingStyleSheet
import me.fzzyhmstrs.fzzy_config.theme.css2.token.CssToken

object CssParser {

    fun parse(css: String): CascadingStyleSheet {
        val filteredCss = filterCodePoints(css)
        TODO()
    }

    private fun filterCodePoints(css: String): String {
        val builder = StringBuilder()
        var wasCr = false
        for (c in css) {
            when (c) {
                '\u000D' -> {
                    wasCr = true
                    builder.append('\u000A')
                }
                '\u000C' -> {
                    wasCr = false
                    builder.append('\u000A')
                }
                '\u000A' -> {
                    if (!wasCr) {
                        builder.append('\u000A')
                    }
                    wasCr = false
                }
                '\u0000' -> {
                    wasCr = false
                    builder.append('\uFFFD')
                }
                else -> {
                    wasCr = false
                    if (c.isSurrogate()) {
                        builder.append('\uFFFD')
                    } else {
                        builder.append(c)
                    }
                }
            }
        }
        return builder.toString()
    }

    private fun tokenizeCss(css: String): List<CssToken> {
        TODO()
    }


}