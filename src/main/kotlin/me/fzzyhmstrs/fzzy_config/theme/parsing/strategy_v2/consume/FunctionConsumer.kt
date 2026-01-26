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
import me.fzzyhmstrs.fzzy_config.theme.parsing.strategy_v2.TokenConsumer.Companion.ERROR
import me.fzzyhmstrs.fzzy_config.theme.parsing.strategy_v2.consume.SimpleBlockConsumer.Block
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.Token
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenQueue
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import java.util.*
import java.util.function.Consumer

object FunctionConsumer: TokenConsumer<Token<*>> {

    override fun consume(queue: TokenQueue, args: Set<String>): ValidationResult<Token<*>> {
        if (!queue.canPoll()) return ValidationResult.error(ERROR, "Couldn't read an at-rule from an exhausted queue")
        val identifier = queue.poll()
        if (identifier.type != CssType.FUNCTION) return ValidationResult.error(ERROR, "Function doesn't start with a function token")
        val componentValues: LinkedList<Token<*>> = LinkedList()
        val errors: MutableList<String> = mutableListOf()
        while (queue.canPoll()) {
            val peek = queue.peek()
            when (peek.type) {
                Parser.EOF -> {
                    queue.poll()
                    return ValidationResult.error(ERROR, "Unexpected EOF before function [$identifier] completed")
                }
                CssType.CLOSE_PARENTHESIS -> {
                    queue.poll()
                    val token = Token(CssType.FUNCTION_CONSUMED, Function(identifier.value as String, TokenQueue.Impl(componentValues)), identifier.line(), identifier.column())
                    return ValidationResult.predicated(token, errors.isEmpty(), errors.toString())
                }
                else -> {
                    componentValues.add(ComponentValueConsumer.consume(queue, args).also { it.writeError(errors) }.get())
                }
            }
        }
        return ValidationResult.error(ERROR, "Unclosed function [$identifier] encountered")
    }

    data class Function(val identifier: String, val values: TokenQueue): ParsePrinter {

        override fun print(printer: Consumer<String>) {
            printer.accept("F: $identifier")
            printer.accept("Values")
            values.print(printer)
        }
    }
}