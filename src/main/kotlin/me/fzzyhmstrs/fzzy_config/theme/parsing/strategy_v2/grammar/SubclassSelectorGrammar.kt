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
import me.fzzyhmstrs.fzzy_config.theme.parsing.parser.Parser
import me.fzzyhmstrs.fzzy_config.theme.parsing.strategy_v2.TokenConsumer
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.Token
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenQueue
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import java.util.LinkedList
import java.util.Optional


object SubclassSelectorGrammar: TokenConsumer<Optional<Selector>> {

    override fun consume(queue: TokenQueue, args: Set<String>): ValidationResult<Optional<Selector>> {
        val id = IdSelectorGrammar.consume(queue, args)
        if (id.isValid() && id.get().isPresent) {
            return id
        }
        val clazz = ClassSelectorGrammar.consume(queue, args)
        if (clazz.isValid() && clazz.get().isPresent) {
            return clazz
        }
        val attr = AttrSelectorGrammar.consume(queue, args)
        if (attr.isValid() && attr.get().isPresent) {
            return attr
        }
        val pseudo = PseudoClassSelectorGrammar.consume(queue, args)
        if (pseudo.isValid() && pseudo.get().isPresent) {
            return pseudo
        }
        return ValidationResult.error(Optional.empty(), "Not a subclass selector")
    }
}