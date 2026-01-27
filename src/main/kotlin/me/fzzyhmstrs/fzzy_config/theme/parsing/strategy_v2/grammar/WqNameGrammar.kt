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
import me.fzzyhmstrs.fzzy_config.theme.parsing.strategy_v2.TokenConsumer
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenQueue
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import java.util.*

class WqNameGrammar(private val selectorMaker: (String, TokenQueue) -> Selector?): TokenConsumer<Optional<Selector>> {

    override fun consume(queue: TokenQueue, args: Set<String>): ValidationResult<Optional<Selector>> {
        if (!queue.canPoll()) return ValidationResult.error(Optional.empty(), "Can't consume a wq-name from an empty queue")
        return queue.attempt { split ->
            val ns = NamespaceGrammar.consume(split, args).get()
            if (split.canPoll()) {
                val token = split.poll()
                if (token.type == CssType.IDENT) {
                    if (ns.isPresent) {
                        val s = selectorMaker(token.asString(), split)
                        if (s == null) {
                            ValidationResult.error(Optional.empty(), "Invalid wq-name")
                        } else {
                            ValidationResult.success(Optional.of(Selector.And(ns.get(), s)))
                        }
                    } else {
                        val s = selectorMaker(token.asString(), split)
                        if (s == null) {
                            ValidationResult.error(Optional.empty(), "Invalid wq-name")
                        } else {
                            ValidationResult.success(Optional.of(s))
                        }

                    }
                } else {
                    ValidationResult.error(Optional.empty(), "Not a wq-name")
                }
            } else {
                ValidationResult.error(Optional.empty(), "Not a wq-name")
            }
        }
    }
}