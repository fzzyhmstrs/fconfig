/*
 * Copyright (c) 2026 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.parsing.strategy_v2

import me.fzzyhmstrs.fzzy_config.theme.parsing.css.CssStyleSheet
import me.fzzyhmstrs.fzzy_config.theme.parsing.strategy_v2.consume.ListOfRulesConsumer
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenQueue
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.map

object CssConsumer: TokenConsumer<CssStyleSheet> {

    override fun consume(queue: TokenQueue, args: Set<String>): ValidationResult<CssStyleSheet> {
        return ListOfRulesConsumer.consume(queue, args).map { CssStyleSheet(it) }
    }
}