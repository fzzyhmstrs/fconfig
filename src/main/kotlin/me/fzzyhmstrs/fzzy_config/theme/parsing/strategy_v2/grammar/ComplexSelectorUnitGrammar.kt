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
import java.util.*


object ComplexSelectorUnitGrammar: TokenConsumer<Optional<Selector>> {

    override fun consume(queue: TokenQueue, args: Set<String>): ValidationResult<Optional<Selector>> {
        if (!queue.canPoll()) return ValidationResult.error(Optional.empty(), "Can't consume a complex selector unit from an empty queue")
        val compound = CompoundSelectorGrammar.consume(queue, args)
        if (compound.isError()) return ValidationResult.error(Optional.empty(), "Invalid complex selector")
        val scs: MutableList<Selector> = mutableListOf()
        while (queue.canPoll()) {
            val sc = queue.attempt { split ->
                val scInner = PseudoCompoundGrammar.consume(split, args)
                if (scInner.isError() || scInner.get().isEmpty) return@attempt ValidationResult.error(Optional.empty(), "Not a valid following pseudo-compound")
                scInner
            }
            if (sc.isValid() && sc.get().isPresent) {
                scs.add(sc.get().get())
                continue
            }
            break
        }
        return if (scs.isNotEmpty()) {
            if (compound.get().isPresent)
                ValidationResult.success(Optional.of(Selector.All(listOf(compound.get().get()) + scs)))
            else
                ValidationResult.success(Optional.of(Selector.All(scs)))
        } else if (compound.get().isPresent) {
            compound
        } else {
            ValidationResult.error(Optional.empty(), "Invalid complex selector")
        }
    }
}