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

import me.fzzyhmstrs.fzzy_config.theme.parsing.css.Selector
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.SelectorContext
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.Specificity
import me.fzzyhmstrs.fzzy_config.theme.parsing.strategy_v2.TokenConsumer
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenQueue
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import java.util.Optional


object TypeSelectorGrammar: TokenConsumer<Optional<Selector>> {

    override fun consume(queue: TokenQueue, args: Set<String>): ValidationResult<Optional<Selector>> {
        if (!queue.canPoll()) return ValidationResult.error(Optional.empty(), "Can't consume a namespace from an empty queue")
        return queue.attempt { split ->
            val name = WqNameGrammar(::Name).consume(split, args)
            if (name.isError()) {
                val ns = NamespaceGrammar.consume(split, args).get()
                if (ns.isPresent && split.canPoll()) {
                    val token = split.poll()
                    if (token.value == "*") {
                        if (ns.isPresent) {
                            ValidationResult.success(Optional.of(ns.get()))
                        } else {
                            ValidationResult.success(Optional.of(Selector.Universal))
                        }
                    } else {
                        ValidationResult.error(Optional.empty(), "Not a type selector")
                    }
                } else {
                    ValidationResult.error(Optional.empty(), "Not a type selector")
                }
            } else {
                name
            }
        }
    }

    class Name(private val name: String, queue: TokenQueue /*unused on purpose*/): Selector {

        override fun matches(context: SelectorContext): Boolean {
            return context.type == name
        }

        override fun selector(): String {
            return name
        }

        override fun specificity(): Specificity {
            return Specificity.TYPE
        }
    }
}