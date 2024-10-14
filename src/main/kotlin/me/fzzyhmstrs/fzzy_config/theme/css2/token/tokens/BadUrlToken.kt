/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.css2.token.tokens

import me.fzzyhmstrs.fzzy_config.theme.css2.token.CssToken
import me.fzzyhmstrs.fzzy_config.theme.css2.token.TokenError
import me.fzzyhmstrs.fzzy_config.theme.css2.token.TokenType

class BadUrlToken(val value: String, val column: Int, val line: Int, val error: String): CssToken, TokenError {

    override fun type(): TokenType {
        return TokenType.BAD_URL
    }
}