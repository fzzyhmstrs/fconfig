/*
 * Copyright (c) 2026 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.parsing.css.consumer

import me.fzzyhmstrs.fzzy_config.theme.parsing.ParsePrinter
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.CssType
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.Errors
import me.fzzyhmstrs.fzzy_config.theme.parsing.parser.Parser
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.Token
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenConsumer
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenConsumer.Companion.ERROR
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenQueue
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenType
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.attachTo
import java.util.*
import java.util.function.Consumer

class SimpleBlockConsumer(private val closing: TokenType<*>): TokenConsumer<Token<*>> {

    override fun consume(queue: TokenQueue, args: Set<String>): ValidationResult<Token<*>> {
        if (!queue.canPoll()) return ValidationResult.error(ERROR, Errors.END_OF_FILE, "Simple Block")
        val opening = queue.poll() //strip off the start token
        val componentValues: LinkedList<Token<*>> = LinkedList()
        val errors = ValidationResult.createMutable()
        while (queue.canPoll()) {
            val peek = queue.peek()
            when (peek.type) {
                Parser.EOF -> {
                    queue.poll()
                    return ValidationResult.error(ERROR, Errors.END_OF_FILE, "${opening.asString()}-block not properly completed")
                }
                closing -> {
                    queue.poll()
                    val token = Token(CssType.SIMPLE_BLOCK, Block(opening.type, TokenQueue.of(componentValues)), opening.line(), opening.column())
                    return ValidationResult.ofMutable(token, errors)
                }
                else -> {
                    componentValues.add(ComponentValueConsumer.consume(queue, args).attachTo(errors).get())
                }
            }
        }
        return ValidationResult.error(ERROR, Errors.SYNTAX, "Unclosed ${opening.asString()}-block encountered")
    }

    data class Block(val type: TokenType<*>, val values: TokenQueue): ParsePrinter {

        override fun print(printer: Consumer<String>) {
            printer.accept("Type: $type")
            printer.accept("Values")
            values.print(printer)
        }
    }
}