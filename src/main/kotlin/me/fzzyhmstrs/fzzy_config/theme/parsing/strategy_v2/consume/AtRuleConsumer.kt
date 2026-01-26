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
import me.fzzyhmstrs.fzzy_config.theme.parsing.strategy_v2.consume.ListOfRulesConsumer.Rule
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.Token
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenQueue
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import java.util.LinkedList

object AtRuleConsumer: TokenConsumer<Token<*>> {

    override fun consume(queue: TokenQueue, args: Set<String>): ValidationResult<Token<*>> {
        if (!queue.canPoll()) return ValidationResult.error(ListOfRulesConsumer.unknownRule(0, 0), "Couldn't read an at-rule from an exhausted queue")
        val at = queue.poll()
        val identifier = at.value(CssType.AT) ?: return ValidationResult.error(ListOfRulesConsumer.unknownRule(at.line(), at.column()), "Couldn't read an at-rule, opening token wasn't an AT token")
        val prelude: LinkedList<Token<*>> = LinkedList()
        var value: Token<*> = Token(CssType.EMPTY_RULE, EmptyRule, at.line(), at.column())
        val errors: MutableList<String> = mutableListOf()
        while (queue.canPoll()) {
            val peek = queue.peek()
            when (peek.type) {
                Parser.EOF -> {
                    queue.poll()
                    return ValidationResult.error(ListOfRulesConsumer.unknownRule(at.line(), at.column()), "Unexpected EOF before at-rule [$identifier] completed")
                }
                CssType.SEMI_COLON -> {
                    queue.poll()
                    return ValidationResult.predicated(Token(CssType.AT_RULE, AtRule(identifier, prelude, value), at.line(), at.column()), errors.isEmpty(), errors.toString())
                }
                CssType.OPEN_BRACE -> {
                    value = SimpleBlockConsumer(CssType.CLOSE_BRACE).consume(queue, args).also { it.writeError(errors) }.get()
                    return ValidationResult.predicated(Token(CssType.AT_RULE, AtRule(identifier, prelude, value), at.line(), at.column()), errors.isEmpty(), errors.toString())
                }
                else -> {
                    if (peek.type == CssType.SIMPLE_BLOCK && (peek.value as SimpleBlockConsumer.Block).type == CssType.OPEN_BRACE) {
                        value = queue.poll()
                        return ValidationResult.predicated(Token(CssType.AT_RULE, AtRule(identifier, prelude, value), at.line(), at.column()), errors.isEmpty(), errors.toString())
                    }
                    prelude.add(ComponentValueConsumer.consume(queue, args).also { it.writeError(errors) }.get())
                }
            }
        }
        return ValidationResult.error(ListOfRulesConsumer.unknownRule(at.line(), at.column()), "Unclosed at-rule [$identifier] encountered")
    }

    data class AtRule(val identifier: String, override val prelude: TokenQueue, override val values: TokenQueue): Rule {
        constructor(identifier: String, prelude: LinkedList<Token<*>>, value: Token<*>): this(identifier, TokenQueue.Impl(prelude), TokenQueue.single(value))
    }

    data object EmptyRule: Rule {
        override val prelude: TokenQueue = TokenQueue.empty()
        override val values: TokenQueue = TokenQueue.Impl(LinkedList())
    }
}