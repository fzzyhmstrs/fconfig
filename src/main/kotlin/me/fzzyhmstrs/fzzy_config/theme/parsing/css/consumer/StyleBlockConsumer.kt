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

import me.fzzyhmstrs.fzzy_config.theme.parsing.css.CssType
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.Errors
import me.fzzyhmstrs.fzzy_config.theme.parsing.parser.Parser
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.Token
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenConsumer
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenQueue
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.attachTo
import java.util.*

object StyleBlockConsumer: TokenConsumer<StyleBlockConsumer.StyleBlock> {

    override fun consume(queue: TokenQueue, args: Set<String>): ValidationResult<StyleBlock> {
        val (line, col) = if (!queue.canPoll()) 0 to 0 else queue.peek().line() to queue.peek().column()
        val declarations: LinkedList<Token<*>> = LinkedList()
        val rules: LinkedList<Token<*>> = LinkedList()
        val errors = ValidationResult.createMutable()
        while (queue.canPoll()) {
            val peek = queue.peek()

            when (peek.type) {
                CssType.WHITESPACE, Parser.EOL -> {
                    queue.poll()
                }
                Parser.EOF -> {
                    return ValidationResult.ofMutable(StyleBlock(declarations, rules), errors)
                }
                CssType.AT -> {
                    rules.add(AtRuleConsumer.consume(queue, args).attachTo(errors).get())
                }
                CssType.IDENT -> {
                    queue.sliceTo({ peek2 ->
                        when (peek2.type) {
                            Parser.EOF, CssType.SEMI_COLON -> true
                            else -> false
                        }
                    }, { queue2 ->
                        //println("Slice is: $queue2")
                        val decl = DeclarationConsumer.consume(queue2, args).attachTo(errors)
                        if (decl.isValid()) {
                            declarations.add(decl.get())
                        }
                    })
                }
                CssType.DELIM -> {
                    if (peek.asString() == "&") {
                        val qual = QualifiedRuleConsumer.consume(queue, args).attachTo(errors)
                        if (qual.isValid()) {
                            rules.add(qual.get())
                        }
                    }
                }
                else -> {
                    errors.addError(Errors.SYNTAX, "Illegal token $peek found in style block, skipping")
                    val f = queue.poll()
                    if (f.message().isNotEmpty()) {
                        errors.addError(Errors.ERROR_TOKEN, f.message())
                    }
                    while (queue.canPoll()) {
                        val peek2 = queue.peek()
                        if (peek2.type == CssType.SEMI_COLON) {
                            queue.poll()
                            break
                        } else if (peek2.type == Parser.EOF) {
                            return ValidationResult.error(StyleBlock(declarations, rules), Errors.END_OF_FILE) { b -> b.content("Style Block").addError(errors.entry) }
                        } else {
                            val f2 = queue.poll()
                            if (f2.message().isNotEmpty()) {
                                errors.addError(Errors.ERROR_TOKEN, f2.message())
                            }
                        }
                    }
                }
            }
        }
        return ValidationResult.ofMutable(StyleBlock(TokenQueue.of(declarations), TokenQueue.of(rules)), errors)
    }

    data class StyleBlock(val declarations: TokenQueue, val rules: TokenQueue) {
        constructor(declarations: LinkedList<Token<*>>, rules: LinkedList<Token<*>>): this(TokenQueue.of(declarations), TokenQueue.of(rules))
    }
}