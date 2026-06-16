/*
 * Copyright (c) 2026 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.parsing.css.rule

import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenQueue
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.map
import java.util.Optional

interface RuleKey<I: Any, O: Any> {
    fun createRule(queue: TokenQueue, previous: Rule<I, O>?): ValidationResult<Optional<Rule<I, O>>>
    fun createRuleSafe(queue: TokenQueue, previous: Rule<*, *>?): ValidationResult<Optional<Rule<I, O>>> {
        return createRule(queue, previous as? Rule<I, O>)
    }

    companion object {
        private val ruleKeys: MutableMap<String, RuleKey<*, *>> = mutableMapOf()

        fun register(declaration: String, key: RuleKey<*, *>) {
            val k = ruleKeys.put(declaration, key)
            if (k != null) {
                FC.LOGGER.error("RuleKey for $declaration already registered")
            }
        }

        fun parseRule(declaration: String, values: TokenQueue, soFar: Map<RuleKey<*, *>, Pair<Boolean, Rule<*, *>>>): ValidationResult<Optional<Pair<RuleKey<*, *>, Rule<*, *>>>> {
            val key = ruleKeys[declaration] ?: return ValidationResult.error(Optional.empty(), "Unknown rule declaration $declaration")
            val previous = soFar[key]?.second
            val ruleResult = key.createRuleSafe(values, previous)
            if (ruleResult.isError()) {
                return ValidationResult.error(Optional.empty(), "Invalid rule declaration: ${ruleResult.getError()}")
            }
            return ruleResult.map { o -> o.map { r -> key to r } }
        }
    }
}