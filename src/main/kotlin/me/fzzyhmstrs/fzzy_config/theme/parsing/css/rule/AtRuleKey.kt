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

import com.google.common.collect.Multimap
import com.google.common.collect.MultimapBuilder
import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.consumer.AtRuleConsumer
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.consumer.StyleBlockConsumer
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.Errors
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenQueue
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.attachTo
import java.util.*

interface AtRuleKey<I: Any> {
    fun createRule(prelude: TokenQueue, value: Optional<StyleBlockConsumer.StyleBlock>): ValidationResult<Optional<AtRule<I>>>

    class Type<I: Any>(val type: String)

    companion object {
        private val atRuleKeys: MutableMap<String, AtRuleKey<*>> = mutableMapOf()
        private val types2Keys: Multimap<Type<*>, String> = MultimapBuilder.hashKeys().hashSetValues().build()

        fun <I: Any> register(identifier: String, key: AtRuleKey<I>, vararg type: Type<I>) {
            val k = atRuleKeys.put(identifier, key)
            if (k != null) {
                FC.LOGGER.error("RuleKey for $identifier already registered")
            }
            for (t in type) {
                types2Keys.put(t, identifier)
            }
        }

        fun <I: Any> process(type: Type<I>, input: I, atRules: List<AtRuleConsumer.AtRule>): ValidationResult<Unit> {
            val errors = ValidationResult.createMutable()
            val applicableRules = types2Keys[type]
            for (atRule in atRules) {
                if (!applicableRules.contains(atRule.identifier)) {
                    errors.addError(Errors.INVALID_AT) { b -> b.message(type.type).content(atRule.identifier) }
                } else {
                    val key = try {
                        atRuleKeys[atRule.identifier] as AtRuleKey<I>
                    } catch (e: Exception) {
                        errors.addError(Errors.CRITICAL, "Unexpected At-rule Key mismatch", e)
                        return ValidationResult.ofMutable(Unit, errors)
                    }
                    val result = key.createRule(atRule.prelude, atRule.value).attachTo(errors)
                    result.get().ifPresent { it.process(input) }
                }
            }
            return ValidationResult.success(Unit)
        }

    }
}