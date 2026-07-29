/*
 * Copyright (c) 2026 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.parsing.css.consumer

import me.fzzyhmstrs.fzzy_config.theme.parsing.css.CssStyleSheet
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.CssType
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.Errors
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.Errors.cssCritical
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.rule.AtRule
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.rule.AtRuleKey
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.rule.Ruleset
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenConsumer
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenQueue
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.attachTo
import java.util.*

object StyleSheetConsumer: TokenConsumer<Optional<CssStyleSheet>> {

    override fun consume(queue: TokenQueue, args: Set<String>): ValidationResult<Optional<CssStyleSheet>> {
        val errors = ValidationResult.createMutable("Errors encountered parsing CSS stylesheet")
        val rules = ListOfRulesConsumer.consume(queue, args).attachTo(errors)
        if (rules.cssCritical()) return ValidationResult.error(Optional.empty(), Errors.CRITICAL, "Creating style sheet: ${rules.getError()}")

        val atRules: MutableList<AtRuleConsumer.AtRule> = mutableListOf()
        val qualifiedRules: MutableList<QualifiedRuleConsumer.QualifiedRule> = mutableListOf()

        for (rule in rules.get()) {
            if (rule.type == CssType.AT_RULE) {
                atRules.add(rule.valueStrict(CssType.AT_RULE))
            } else if (rule.type == CssType.QUALIFIED_RULE) {
                qualifiedRules.add(rule.valueStrict(CssType.QUALIFIED_RULE))
            }
        }

        val builder = CssStyleSheet.Builder()
        AtRuleKey.process(AtRule.TOP_LEVEL, builder, atRules)
        for (r in qualifiedRules) {
            Ruleset.parse(r, args).attachTo(errors).get().ifPresent { builder.ruleset(it) }
        }

        return ValidationResult.success(Optional.of(builder.build()))
    }
}