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

import me.fzzyhmstrs.fzzy_config.theme.parsing.ParsePrinter
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.CssType
import me.fzzyhmstrs.fzzy_config.theme.parsing.parser.Parser
import me.fzzyhmstrs.fzzy_config.theme.parsing.strategy_v2.TokenConsumer
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.Token
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenQueue
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import java.util.LinkedList
import java.util.function.Consumer

object ListOfRulesConsumer: TokenConsumer<List<Token<*>>> {

    override fun consume(queue: TokenQueue, args: Set<String>): ValidationResult<List<Token<*>>> {
        val rules: LinkedList<Token<*>> = LinkedList()
        val errors: MutableList<String> = mutableListOf()
        while(queue.canPoll()) {
            val peek = queue.peek()
            when (peek.type) {
                CssType.CDC, CssType.CDO, Parser.EOL, CssType.WHITESPACE -> {
                    queue.poll()
                    continue
                }
                Parser.EOF -> {
                    return ValidationResult.predicated(rules, errors.isEmpty(), errors.toString())
                }
                CssType.AT -> {
                    rules.add(AtRuleConsumer.consume(queue, args).also { it.writeError(errors) }.get())
                }
                else -> {
                    rules.add(QualifiedRuleConsumer.consume(queue, args).also { it.writeError(errors) }.get())
                }
            }
        }
        return ValidationResult.predicated(rules, errors.isEmpty(), errors.toString())
    }

    interface Rule: ParsePrinter {
        val prelude: TokenQueue
        val values: TokenQueue

        override fun print(printer: Consumer<String>) {
            printer.accept("Prelude")
            prelude.print(printer)
            printer.accept("Values")
            values.print(printer)
        }
    }

    data object UnknownRule: Rule {
        override val prelude: TokenQueue = TokenQueue.empty()
        override val values: TokenQueue = TokenQueue.Impl(LinkedList())
    }

    fun unknownRule(line: Int, col: Int): Token<*> {
        return Token(CssType.UNKNOWN_RULE, UnknownRule, line, col, "Unknown Rule")
    }
}