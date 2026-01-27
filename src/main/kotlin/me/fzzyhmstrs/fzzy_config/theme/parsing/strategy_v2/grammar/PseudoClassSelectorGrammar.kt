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

import me.fzzyhmstrs.fzzy_config.theme.parsing.css.*
import me.fzzyhmstrs.fzzy_config.theme.parsing.strategy_v2.TokenConsumer
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.Token
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenQueue
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import java.util.LinkedList
import java.util.Optional


object PseudoClassSelectorGrammar: TokenConsumer<Optional<Selector>> {

    override fun consume(queue: TokenQueue, args: Set<String>): ValidationResult<Optional<Selector>> {
        if (!queue.canPoll()) return ValidationResult.error(Optional.empty(), "Can't consume a pseudo-class selector from an empty queue")
        return queue.attempt { split ->
            val token = split.poll()
            if (token.value == ":" && split.canPoll()) {
                val token2 = split.poll()
                if (token2.type == CssType.IDENT) {
                    val key = token2.asString()
                    val pseudo = Pseudo.getPseudo(key) ?: return@attempt ValidationResult.error(Optional.empty(), "Unsupported pseudo-class selector")
                    ValidationResult.success(Optional.of(PseudoClass(pseudo, key)))
                } else if (token2.type == CssType.FUNCTION && split.canPoll()) {
                    val key = token2.value as String
                    val func = Func.getFunc(key) ?: return@attempt ValidationResult.error(Optional.empty(), "Invalid pseudo-class selector")
                    val funcVals: LinkedList<Token<*>> = LinkedList()
                    while (split.canPoll()) {
                        val t = split.poll()
                        if (t.type == CssType.CLOSE_PARENTHESIS) {
                            val raw = funcVals.fold("") { r, tkn -> r + tkn.asString() }
                            val selector = func.prepare(TokenQueue.Impl(funcVals), args) { f, a -> PseudoFunction(f as Func<Any>, key, a, raw) }
                            return@attempt if(selector != null)
                                ValidationResult.success(Optional.of(selector))
                            else
                                ValidationResult.error(Optional.empty(), "Invalid pseudo-class selector")
                        } else {
                            funcVals.add(t)
                        }
                    }
                    ValidationResult.error(Optional.empty(), "Not a pseudo-class selector")
                } else {
                    ValidationResult.error(Optional.empty(), "Not a pseudo-class selector")
                }
            } else {
                ValidationResult.error(Optional.empty(), "Not a pseudo-class selector")
            }
        }
    }

    class PseudoClass(private val pseudo: Pseudo, private val name: String): Selector {

        override fun matches(context: SelectorContext): Boolean {
            return pseudo.getterGetter(context.pseudoGetter)
        }

        override fun selector(): String {
            return ":$name"
        }
    }

    class PseudoFunction<in T: Any>(private val func: Func<T>, private val name: String, private val args: T, private val raw: String): Selector {

        override fun matches(context: SelectorContext): Boolean {
            return func.apply(args, context)
        }

        override fun selector(): String {
            return "$name($raw)"
        }
    }
}