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

object StyleBlockConsumer: TokenConsumer<Token<*>> {

    override fun consume(queue: TokenQueue, args: Set<String>): ValidationResult<Token<*>> {
        val (line, col) = if (!queue.canPoll()) 0 to 0 else queue.peek().line() to queue.peek().column()
        val declarations: LinkedList<Token<*>> = LinkedList()
        val rules: LinkedList<Token<*>> = LinkedList()
        val errors: MutableList<String> = mutableListOf()
        while (queue.canPoll()) {
            val peek = queue.peek()
            when (peek.type) {
                CssType.WHITESPACE, Parser.EOL -> {
                    queue.poll()
                }
                Parser.EOF -> {
                    return ValidationResult.predicated(Token(CssType.STYLE_BLOCK, StyleBlock(declarations, rules), line, col), errors.isEmpty(), errors.toString())
                }
                CssType.AT -> {
                    rules.add(AtRuleConsumer.consume(queue, args).also { it.writeError(errors) }.get())
                }
                CssType.IDENT -> {
                    val list: LinkedList<Token<*>> = LinkedList()
                    list.add(queue.poll()) //apply the identifier into the declaration
                    while (queue.canPoll()) {
                        val peek2 = queue.peek()
                        when (peek2.type) {
                            Parser.EOF, CssType.SEMI_COLON -> {
                                break
                            }
                            else -> {
                                list.add(queue.poll())
                            }
                        }
                        val queue2 = TokenQueue.Impl(list)
                        val decl = DeclarationConsumer.consume(queue2, args)
                        if (decl.isError()) {
                            errors.add(decl.getError())
                        } else {
                            declarations.add(decl.get())
                        }
                    }
                }
                CssType.DELIM -> {
                    if (peek.asString() == "&") {
                        val qual = QualifiedRuleConsumer.consume(queue, args)
                        if (qual.isError()) {
                            errors.add(qual.getError())
                        } else {
                            rules.add(qual.get())
                        }
                    }
                }
                else -> {
                    errors.add("Illegal token $peek found in style block, skipping")
                    val f = queue.poll()
                    if (f.message().isNotEmpty()) {
                        errors.add(f.message())
                    }
                    while (queue.canPoll()) {
                        val peek2 = queue.peek()
                        if (peek2.type == CssType.SEMI_COLON) {
                            queue.poll()
                            break
                        } else if (peek2.type == Parser.EOF) {
                            return ValidationResult.error(Token(CssType.STYLE_BLOCK, StyleBlock(declarations, rules), line, col), errors.toString())
                        } else {
                            val f2 = queue.poll()
                            if (f2.message().isNotEmpty()) {
                                errors.add(f2.message())
                            }
                        }
                    }
                }
            }
        }
        return ValidationResult.predicated(Token(CssType.STYLE_BLOCK, StyleBlock(TokenQueue.Impl(declarations), TokenQueue.Impl(rules)), line, col), errors.isEmpty(), errors.toString())
    }

    data class StyleBlock(val declarations: TokenQueue, val rules: TokenQueue) {
        constructor(declarations: LinkedList<Token<*>>, rules: LinkedList<Token<*>>): this(TokenQueue.Impl(declarations), TokenQueue.Impl(rules))
    }
}