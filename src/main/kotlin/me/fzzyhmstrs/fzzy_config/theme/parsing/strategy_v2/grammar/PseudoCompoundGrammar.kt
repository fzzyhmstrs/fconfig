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
import me.fzzyhmstrs.fzzy_config.theme.parsing.strategy_v2.TokenConsumer
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenQueue
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import java.util.Optional


object PseudoCompoundGrammar: TokenConsumer<Optional<Selector>> {

    override fun consume(queue: TokenQueue, args: Set<String>): ValidationResult<Optional<Selector>> {
        if (!queue.canPoll()) return ValidationResult.error(Optional.empty(), "Can't consume a pseudo-compound from an empty queue")
        val pseudoElement = PseudoElementSelectorGrammar.consume(queue, args)
        if (pseudoElement.get().isEmpty || pseudoElement.isError()) return ValidationResult.error(Optional.empty(), "Not a pseudo-compound")
        val scArgs = args + setOf("--user-actions-only")
        val scs: MutableList<Selector> = mutableListOf()
        while (queue.canPoll()) {
            val sc = queue.attempt { split ->
                val scInner = PseudoClassSelectorGrammar.consume(split, scArgs)
                if (scInner.isError() || scInner.get().isEmpty) return@attempt ValidationResult.error(Optional.empty(), "Not a valid following pseudo-class")
                scInner
            }
            if (sc.isValid() && sc.get().isPresent) {
                scs.add(sc.get().get())
                continue
            }
            break
        }
        return if (scs.isNotEmpty()) {
            ValidationResult.success(Optional.of(Selector.All(listOf(pseudoElement.get().get()) + scs)))
        } else {
            pseudoElement
        }
    }
}