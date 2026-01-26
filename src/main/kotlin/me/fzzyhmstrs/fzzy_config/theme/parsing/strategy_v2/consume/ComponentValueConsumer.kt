/*
 * Copyright (c) 2026 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.parsing.strategy_v2.consume

import me.fzzyhmstrs.fzzy_config.theme.parsing.css.CssType
import me.fzzyhmstrs.fzzy_config.theme.parsing.strategy_v2.TokenConsumer
import me.fzzyhmstrs.fzzy_config.theme.parsing.strategy_v2.TokenConsumer.Companion.ERROR
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.Token
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenQueue
import me.fzzyhmstrs.fzzy_config.util.ValidationResult


object ComponentValueConsumer: TokenConsumer<Token<*>> {

    override fun consume(queue: TokenQueue, args: Set<String>): ValidationResult<Token<*>> {
        if (!queue.canPoll()) return ValidationResult.error(ERROR, "Can't consume a component value from an empty queue")
        val peek = queue.peek()
        when (peek.type) {
            CssType.OPEN_BRACE -> {
                return SimpleBlockConsumer(CssType.CLOSE_BRACE).consume(queue, args)
            }
            CssType.OPEN_BRACKET -> {
                return SimpleBlockConsumer(CssType.CLOSE_BRACKET).consume(queue, args)
            }
            CssType.OPEN_PARENTHESIS -> {
                return SimpleBlockConsumer(CssType.CLOSE_PARENTHESIS).consume(queue, args)
            }
            CssType.FUNCTION -> {
                return FunctionConsumer.consume(queue, args)
            }
            else -> {
                return ValidationResult.success(queue.poll())
            }
        }

    }
}