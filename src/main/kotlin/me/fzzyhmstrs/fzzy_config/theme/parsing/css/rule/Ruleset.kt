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

import me.fzzyhmstrs.fzzy_config.theme.parsing.css.CssType
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.Selector
import me.fzzyhmstrs.fzzy_config.theme.parsing.strategy_v2.consume.DeclarationConsumer
import me.fzzyhmstrs.fzzy_config.theme.parsing.strategy_v2.consume.QualifiedRuleConsumer
import me.fzzyhmstrs.fzzy_config.theme.parsing.strategy_v2.grammar.SelectorListGrammar
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import java.util.Optional

class Ruleset(val selectors: List<Selector>, val rules: Map<RuleKey<*, *>, Pair<Boolean, Rule<*, *>>>, val children: List<Ruleset>) {

    companion object {
        fun parse(rule: QualifiedRuleConsumer.QualifiedRule, args: Set<String>): ValidationResult<Optional<Ruleset>> {
            val prelude = rule.prelude
            val selectorResult = SelectorListGrammar.consume(prelude, args)
            if (selectorResult.isError()) {
                return ValidationResult.error(Optional.empty(), "Error creating Ruleset: ${selectorResult.getError()}")
            }
            val selectors = selectorResult.get()
            val rules: MutableMap<RuleKey<*, *>, Pair<Boolean, Rule<*, *>>> = mutableMapOf()
            val children: MutableList<Ruleset> = mutableListOf()
            val errors: MutableList<String> = mutableListOf()
            val styleBlock = rule.value
            val styleRules = styleBlock.rules
            while (styleRules.canPoll()) {
                val styleRule = styleRules.poll()
                if (styleRule.type == CssType.AT_RULE) {
                    errors.add("Nested at-rules aren't supported: ${styleRule.asString()}")
                } else if (styleRule.type == CssType.QUALIFIED_RULE) {
                    val qr = styleRule.valueStrict(CssType.QUALIFIED_RULE) as QualifiedRuleConsumer.QualifiedRule
                    val qrResult = parse(qr, args).also { it.writeError(errors) }
                    if (qrResult.get().isPresent) {
                        children.add(qrResult.get().get())
                    }
                } else {
                    errors.add("Unknown or errored input found: ${styleRule.asString()}")
                }
            }
            val styleDeclarations = styleBlock.declarations
            while (styleDeclarations.canPoll()) {
                val styleDeclaration = styleDeclarations.poll()
                if (styleDeclaration.type == CssType.DECLARATION) {
                    val declaration = styleDeclaration.valueStrict(CssType.DECLARATION) as DeclarationConsumer.Declaration
                    val declId = declaration.identifier
                    val declValues = declaration.values
                    val declImportant = declaration.important
                    val declResult = RuleKey.parseRule(declId, declValues, rules)
                    if (declResult.isError()) {
                        errors.add("Unknown or errored rule declaration found: ${declResult.getError()}")
                        continue
                    }
                    if (declResult.get().isEmpty) {
                        continue
                    }
                    rules[declResult.get().get().first] = declImportant to declResult.get().get().second
                }
            }
            return ValidationResult.predicated(Optional.of(Ruleset(selectors, rules, children)), errors.isEmpty(), "Errors while creating RuleSet: $errors")
        }
    }
}