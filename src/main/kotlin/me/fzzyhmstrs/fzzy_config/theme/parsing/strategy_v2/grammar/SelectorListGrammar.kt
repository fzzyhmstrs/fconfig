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
import me.fzzyhmstrs.fzzy_config.theme.parsing.parser.Parser
import me.fzzyhmstrs.fzzy_config.theme.parsing.strategy_v2.TokenConsumer
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.Token
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenQueue
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import java.util.LinkedList


object SelectorListGrammar: TokenConsumer<List<Token<*>>> {

    override fun consume(queue: TokenQueue, args: Set<String>): ValidationResult<List<Token<*>>> {
        if (!queue.canPoll()) return ValidationResult.error(listOf(), "Can't consume a selector list from an empty queue")
        val queuesResult = ComponentValueListGrammar.consume(queue, args)
        val strict = args.contains("--strict-selector-list")
        if (queuesResult.isError() && strict) return ValidationResult.error(listOf(), "Selector list can't be built from invalid component values list: ${queuesResult.getError()}")
        val queues = queuesResult.get()
        val selectors: MutableList<Token<*>> = mutableListOf()
        for (queue2 in queues) {
            val result = ComplexSelectorGrammar.consume(queue2, args)
            if (result.isError() && strict) return ValidationResult.error(listOf(), "Selector list can't be built with invalid selector: ${result.getError()}")
            selectors.addAll(result.get())
        }
        return ValidationResult.success(selectors)
    }

}