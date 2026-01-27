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


object ComponentValueListGrammar: TokenConsumer<List<TokenQueue>> {

    override fun consume(queue: TokenQueue, args: Set<String>): ValidationResult<List<TokenQueue>> {
        if (!queue.canPoll()) return ValidationResult.error(listOf(), "Can't consume a list of component values from an empty queue")
        val queues: MutableList<TokenQueue> = mutableListOf()
        var currentList: LinkedList<Token<*>> = LinkedList()
        while (queue.canPoll()) {
            val peek = queue.peek()
            when (peek.type) {
                CssType.COMMA -> {
                    queue.poll()
                    val q = TokenQueue.Impl(currentList)
                    queues.add(q)
                    currentList = LinkedList()
                }
                Parser.EOF -> {
                    queue.poll()
                    val q = TokenQueue.Impl(currentList)
                    queues.add(q)
                    return ValidationResult.success(queues)
                }
                else -> {
                    currentList.add(queue.poll())
                }
            }
        }
        return ValidationResult.success(queues)
    }

}