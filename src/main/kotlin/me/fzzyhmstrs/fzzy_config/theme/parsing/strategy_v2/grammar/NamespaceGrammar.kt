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
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import java.util.LinkedList
import java.util.Optional


object NamespaceGrammar: TokenConsumer<Optional<Selector>> {

    override fun consume(queue: TokenQueue, args: Set<String>): ValidationResult<Optional<Selector>> {
        if (!queue.canPoll()) return ValidationResult.error(Optional.empty(), "Can't consume a namespace from an empty queue")
        return queue.attempt { split ->
            val token = split.poll()
            if (token.type == CssType.IDENT && split.canPoll()) {
                val token2 = split.poll()
                if (token2.value == "|") {
                    ValidationResult.success(Optional.of(InNamespace(token.asString())))
                } else {
                    ValidationResult.error(Optional.empty(), "Not a namespace")
                }
            } else if (token.value == "*" && split.canPoll()) {
                val token2 = split.poll()
                if (token2.value == "|") {
                    ValidationResult.success(Optional.of(AnyNamespace))
                } else {
                    ValidationResult.error(Optional.empty(), "Not a namespace")
                }
            } else if (token.value == "|") {
                ValidationResult.success(Optional.of(NoNamespace))
            } else {
                ValidationResult.error(Optional.empty(), "Not a namespace")
            }
        }
    }

    class InNamespace(private val namespace: String): Selector {

        override fun matches(context: SelectorContext): Boolean {
            return context.namespace == namespace
        }

        override fun selector(): String {
            return "$namespace|"
        }

        override fun specificity(): Specificity {
            return Specificity.ZERO
        }
    }

    data object AnyNamespace: Selector {

        override fun matches(context: SelectorContext): Boolean {
            return true
        }

        override fun selector(): String {
            return "*|"
        }

        override fun specificity(): Specificity {
            return Specificity.ZERO
        }
    }

    data object NoNamespace: Selector {

        override fun matches(context: SelectorContext): Boolean {
            return context.namespace == null
        }

        override fun selector(): String {
            return "|"
        }

        override fun specificity(): Specificity {
            return Specificity.ZERO
        }
    }
}