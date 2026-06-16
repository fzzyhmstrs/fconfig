/*
 * Copyright (c) 2026 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.parsing.strategy_v2.grammar

import me.fzzyhmstrs.fzzy_config.theme.parsing.css.CssType
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.Selector
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.SelectorContext
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.Specificity
import me.fzzyhmstrs.fzzy_config.theme.parsing.strategy_v2.TokenConsumer
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenQueue
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import java.util.Optional


object IdSelectorGrammar: TokenConsumer<Optional<Selector>> {

    override fun consume(queue: TokenQueue, args: Set<String>): ValidationResult<Optional<Selector>> {
        if (!queue.canPoll()) return ValidationResult.error(Optional.empty(), "Can't consume an id selector from an empty queue")
        return queue.attempt { split ->
            val token = split.poll()
            if (token.type == CssType.HASH) {
                ValidationResult.success(Optional.of(Id(token.value as String)))
            } else {
                ValidationResult.error(Optional.empty(), "Not an id selector")
            }
        }
    }

    private class Id(private val id: String): Selector {

        override fun matches(screenContext: Selector.Position, context: SelectorContext): Boolean {
            return context.selectorId() == id
        }

        override fun selector(): String {
            return "#$id"
        }

        override fun specificity(): Specificity {
            return Specificity.ID
        }
    }
}