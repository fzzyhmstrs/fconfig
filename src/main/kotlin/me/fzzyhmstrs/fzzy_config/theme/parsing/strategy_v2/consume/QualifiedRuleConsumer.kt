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
import me.fzzyhmstrs.fzzy_config.theme.parsing.parser.Parser
import me.fzzyhmstrs.fzzy_config.theme.parsing.strategy_v2.TokenConsumer
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.Token
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenQueue
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import java.util.*

object QualifiedRuleConsumer: TokenConsumer<Token<*>> {

    override fun consume(queue: TokenQueue, args: Set<String>): ValidationResult<Token<*>> {
        if (!queue.canPoll()) return ValidationResult.error(ListOfRulesConsumer.unknownRule(0, 0), "Couldn't read a qualified-rule from an exhausted queue")
        val prelude: LinkedList<Token<*>> = LinkedList()
        val value: Token<*>
        val errors: MutableList<String> = mutableListOf()
        val initial = queue.peek()
        while (queue.canPoll()) {
            val peek = queue.peek()
            when (peek.type) {
                Parser.EOF -> {
                    queue.poll()
                    return ValidationResult.error(ListOfRulesConsumer.unknownRule(initial.line(), initial.column()), "Unexpected EOF before qualified-rule completed")
                }
                CssType.OPEN_BRACE -> {
                    value = SimpleBlockConsumer(CssType.CLOSE_BRACE).consume(queue, args).also { it.writeError(errors) }.get()
                    return ValidationResult.predicated(Token(CssType.QUALIFIED_RULE, QualifiedRule(prelude, value), initial.line(), initial.column()), errors.isEmpty(), errors.toString())
                }
                else -> {
                    if (peek.type == CssType.SIMPLE_BLOCK && (peek.value as SimpleBlockConsumer.Block).type == CssType.OPEN_BRACE) {
                        value = queue.poll()
                        return ValidationResult.predicated(Token(CssType.QUALIFIED_RULE, QualifiedRule(prelude, value), initial.line(), initial.column()), errors.isEmpty(), errors.toString())
                    }
                    prelude.add(ComponentValueConsumer.consume(queue, args).also { it.writeError(errors) }.get())
                }
            }
        }
        return ValidationResult.error(ListOfRulesConsumer.unknownRule(initial.line(), initial.column()), "Unclosed qualified-rule encountered")
    }

    data class QualifiedRule(override val prelude: TokenQueue, override val values: TokenQueue): ListOfRulesConsumer.Rule {
        constructor(prelude: LinkedList<Token<*>>, value: Token<*>): this(TokenQueue.Impl(prelude), TokenQueue.single(value))
    }
}