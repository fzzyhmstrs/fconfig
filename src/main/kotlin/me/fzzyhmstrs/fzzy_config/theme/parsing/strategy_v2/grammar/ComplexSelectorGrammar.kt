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
import me.fzzyhmstrs.fzzy_config.theme.parsing.parser.Parser
import me.fzzyhmstrs.fzzy_config.theme.parsing.strategy_v2.TokenConsumer
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.Token
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenQueue
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenType
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import java.util.Deque
import java.util.LinkedList
import java.util.Optional
import java.util.Queue
import java.util.Stack


object ComplexSelectorGrammar: TokenConsumer<Optional<Selector>> {

    override fun consume(queue: TokenQueue, args: Set<String>): ValidationResult<Optional<Selector>> {
        if (!queue.canPoll()) return ValidationResult.error(Optional.empty(), "Can't consume a complex selector from an empty queue")
        queue.consumeWhitespace() // leading whitespace in a selector don't matter
        var selector: Selector? = null
        var combinator: Combinator? = null
        while (queue.canPoll()) {
            val s = parseSelector(queue, args)
            if (s.isError()) {
                return ValidationResult.error(Optional.empty(), "Invalid complex selector: ${s.getError()}")
            }
            if (s.get().isEmpty) {
                return ValidationResult.error(Optional.empty(), "Invalid complex selector: Selector empty")
            }
            if (combinator != null) {
                if (selector == null) {
                    return ValidationResult.error(Optional.empty(), "Invalid complex selector: Combinator before selector")
                }
                val cs = combinator.combine(selector, s.get().get())
                selector = cs
                combinator = null
            } else {
                selector = s.get().get()
            }
            val c = parseCombinator(queue, args)
            if (c.isError()) {
                return ValidationResult.error(Optional.empty(), "Invalid complex selector: ${s.getError()}")
            }
            if (!c.get().isEmpty) {
                combinator = c.get().get()
            }
        }
        if (combinator != null && combinator != DescendantCombinator) {
            return ValidationResult.error(Optional.empty(), "Invalid complex selector: Combinator without right-hand argument")
        }
        if (selector == null) {
            return ValidationResult.error(Optional.empty(), "Invalid complex selector: Selector empty")
        }
        return ValidationResult.success(Optional.of(selector))
    }

    private fun parseSelector(queue: TokenQueue, args: Set<String>): ValidationResult<Optional<Selector>> {
        return ComplexSelectorUnitGrammar.consume(queue, args)
    }

    private fun parseCombinator(queue: TokenQueue, args: Set<String>): ValidationResult<Optional<Combinator>> {
        val tokens: LinkedList<Token<*>> = LinkedList()
        var hasCombinator = false
        while (queue.canPoll()) {
            val t = queue.poll()
            when (t.type) {
                CssType.WHITESPACE -> {
                    tokens.add(t)
                }
                CssType.DELIM -> {
                    when (t.valueStrict(CssType.DELIM)) {
                        ">", "~", "+" -> {
                            if (hasCombinator) {
                                return ValidationResult.error(Optional.empty(), "More than one combinator found between selectors: ${(tokens + listOf(t)).map(Token<*>::asString).joinToString("") }}")
                            }
                            hasCombinator = true
                            tokens.add(t)
                        }
                        else -> {
                            break
                        }
                    }
                }
                else -> {
                    break
                }
            }
        }
        if (tokens.isEmpty()) {
            return ValidationResult.success(Optional.empty())
        }
        if (tokens.size == 1) {
            val t = tokens[0]
            if (t.type == CssType.WHITESPACE) {
                if (t.valueStrict(CssType.WHITESPACE) == " ") {
                    return ValidationResult.success(Optional.of(DescendantCombinator))
                }
                return ValidationResult.success(Optional.empty())
            }
        }
         do {
            val t = tokens.poll()
            if (t.type == CssType.DELIM) {
                when (t.valueStrict(CssType.DELIM)) {
                    ">" -> {
                        return ValidationResult.success(Optional.of(ChildCombinator))
                    }
                    "~", "+" -> {
                        return ValidationResult.error(Optional.empty(), "Sibling combinators not implemented yet")
                    }
                }
            }
        } while (tokens.isNotEmpty())
        return ValidationResult.error(Optional.empty(), "Invalid combinator")
    }

    private interface Combinator {
        fun combine(left: Selector, right: Selector): Selector
    }

    private object DescendantCombinator: Combinator {
        override fun combine(
            left: Selector,
            right: Selector
        ): Selector {
            return DescendantSelector(left, right)
        }
    }

    private class DescendantSelector(private val left: Selector, private val right: Selector): Selector {

        override fun matches(context: SelectorContext): Boolean {
            return right.matches(context) && parentMatches(context)
        }

        private fun parentMatches(context: SelectorContext): Boolean {
            var parent: SelectorContext? = context.selectorParent()
            while (parent != null) {
                if (left.matches(parent)) return true
                parent = parent.selectorParent()
            }
            return false
        }

        override fun selector(): String {
            return " "
        }

        override fun specificity(): Specificity {
            return left.specificity() + right.specificity()
        }
    }

    private object ChildCombinator: Combinator {
        override fun combine(
            left: Selector,
            right: Selector
        ): Selector {
            return ChildSelector(left, right)
        }
    }

    private class ChildSelector(private val left: Selector, private val right: Selector): Selector {

        override fun matches(context: SelectorContext): Boolean {
            return right.matches(context) && parentMatches(context)
        }

        private fun parentMatches(context: SelectorContext): Boolean {
            return context.selectorParent()?.let { left.matches(it) } ?: false
        }

        override fun selector(): String {
            return " "
        }

        override fun specificity(): Specificity {
            return left.specificity() + right.specificity()
        }
    }
}