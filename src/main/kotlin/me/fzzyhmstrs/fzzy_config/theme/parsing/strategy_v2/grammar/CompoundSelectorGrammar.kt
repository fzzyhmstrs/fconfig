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

//TODO make grammars only return an errored ValidationResult when something is actually "wrong". Empty optional isn't necessarily wrong.

object CompoundSelectorGrammar: TokenConsumer<Optional<Selector>> {

    override fun consume(queue: TokenQueue, args: Set<String>): ValidationResult<Optional<Selector>> {
        if (!queue.canPoll()) return ValidationResult.error(Optional.empty(), "Can't consume a compound selector from an empty queue")
        val selectors: MutableList<Selector> = mutableListOf()
        val type = TypeSelectorGrammar.consume(queue, args)
        if (type.isValid() && type.get().isPresent) {
            selectors.add(type.get().get())
        }
        while (queue.canPoll()) {
            val subclass = SubclassSelectorGrammar.consume(queue, args)
            if (subclass.isValid() && subclass.get().isPresent) {
                selectors.add(subclass.get().get())
                continue
            }
            break
        }
        return ValidationResult.success(Optional.of(Selector.All(selectors)))
    }
}