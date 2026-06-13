/*
 * Copyright (c) 2025 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.parsing.css

import me.fzzyhmstrs.fzzy_config.theme.parsing.ParsePrinter
import me.fzzyhmstrs.fzzy_config.theme.parsing.strategy_v2.consume.AtRuleConsumer
import me.fzzyhmstrs.fzzy_config.theme.parsing.strategy_v2.consume.QualifiedRuleConsumer
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.Token
import java.util.function.Consumer

class CssStyleSheet(private val atRules: List<AtRuleConsumer.AtRule>, private val qualifiedRules: List<QualifiedRuleConsumer.QualifiedRule>): ParsePrinter {

    val size: Int
        get() {
            return atRules.size + qualifiedRules.size
        }

    override fun print(printer: Consumer<String>) {
        printer.accept("At-Rules")
        for (rule in atRules) {
            printer.accept(rule.toString())
        }
        printer.accept("")
        printer.accept("Qualified Rules")
        for (rule in qualifiedRules) {
            printer.accept(rule.toString())
        }
    }
}