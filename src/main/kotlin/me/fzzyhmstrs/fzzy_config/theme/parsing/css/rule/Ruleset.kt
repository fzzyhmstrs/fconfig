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
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.Errors
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.Errors.cssCritical
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.Selector
import me.fzzyhmstrs.fzzy_config.theme.parsing.strategy.builder.Creator
import me.fzzyhmstrs.fzzy_config.theme.parsing.strategy.consumers.AtRuleConsumer
import me.fzzyhmstrs.fzzy_config.theme.parsing.strategy.consumers.DeclarationConsumer
import me.fzzyhmstrs.fzzy_config.theme.parsing.strategy.consumers.QualifiedRuleConsumer
import me.fzzyhmstrs.fzzy_config.theme.parsing.strategy.grammar.SelectorListGrammar
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.attachTo
import java.util.Collections
import java.util.Optional

//Pair is IMPORTANT to RULE
class Ruleset(val selectors: List<Selector>, val rules: Map<DeclarationKey<*, *>, Pair<Boolean, Declaration<*>>>, val children: List<Ruleset>) {

    class Builder {
        private val selectors: MutableList<Selector> = mutableListOf()
        private val rules: MutableMap<DeclarationKey<*, *>, Pair<Boolean, Creator<Declaration<*>>>> = mutableMapOf()
        private val children: MutableList<Ruleset> = mutableListOf()

        fun selector(selector: Selector): Builder {
            selectors.add(selector)
            return this
        }

        fun selectors(selector: List<Selector>): Builder {
            selectors.addAll(selector)
            return this
        }

        fun rule(key: DeclarationKey<*, *>, important: Boolean, declaration: Creator<Declaration<*>>): Builder {
            rules[key] = important to declaration
            return this
        }

        fun rules(): Map<DeclarationKey<*, *>, Pair<Boolean, Creator<Declaration<*>>>> {
            return rules
        }

        fun child(child: Ruleset): Builder {
            children.add(child)
            return this
        }

        fun build(): Ruleset {
            return Ruleset(Collections.unmodifiableList(selectors),
                Collections.unmodifiableMap(rules.mapValues { (key, value) -> Pair(value.first, value.second.create()) }),
                Collections.unmodifiableList(children))
        }
    }

    companion object {
        fun parse(rule: QualifiedRuleConsumer.QualifiedRule, args: Set<String>): ValidationResult<Optional<Ruleset>> {
            val errors = ValidationResult.createMutable()
            val prelude = rule.prelude
            val selectorResult = SelectorListGrammar.consume(prelude, args).attachTo(errors)
            if (selectorResult.cssCritical()) {
                return ValidationResult.error(Optional.empty(), Errors.CRITICAL, "Creating Ruleset")
            }

            val builder = Builder()

            val styleBlock = rule.value
            val styleRules = styleBlock.rules
            val atRules: MutableList<AtRuleConsumer.AtRule> = mutableListOf()
            while (styleRules.canPoll()) {
                val styleRule = styleRules.poll()
                if (styleRule.type == CssType.AT_RULE) {
                    atRules.add(styleRule.valueStrict(CssType.AT_RULE))
                } else if (styleRule.type == CssType.QUALIFIED_RULE) {
                    val qr = styleRule.valueStrict(CssType.QUALIFIED_RULE) as QualifiedRuleConsumer.QualifiedRule
                    val qrResult = parse(qr, args).attachTo(errors)
                    if (qrResult.get().isPresent) {
                        builder.child(qrResult.get().get())
                    }
                } else {
                    errors.addError(Errors.UNKNOWN_RULE, styleRule.asString())
                }
            }
            AtRuleKey.process(AtRule.RULESET, builder, atRules)

            val styleDeclarations = styleBlock.declarations
            while (styleDeclarations.canPoll()) {
                val styleDeclaration = styleDeclarations.poll()
                if (styleDeclaration.type == CssType.DECLARATION) {
                    val declaration = styleDeclaration.valueStrict(CssType.DECLARATION) as DeclarationConsumer.Declaration
                    val declId = declaration.identifier
                    val declValues = declaration.values
                    val declImportant = declaration.important
                    val declResult = DeclarationKey.parseDecl(declId, declValues, builder.rules()).attachTo(errors)
                    if (declResult.cssCritical() || declResult.get().isEmpty) {
                        continue
                    }
                    builder.rule(declResult.get().get().first, declImportant, declResult.get().get().second)
                }
            }
            return ValidationResult.ofMutable(Optional.of(builder.build()), errors)
        }
    }
}