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
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.rule.Ruleset
import java.util.*
import java.util.function.Consumer

class CssStyleSheet(private val rules: List<Ruleset>, private val children: List<CssStyleSheet>): ParsePrinter {

    /*
    * May want to base style queries around a "style stack"
    * a sort of context builder that would allow a rendering agent to build layered context as the rendered objects are traversed
    * - Start the stack by applying screen context information, screen width/height
    * - screen can provide opening salvo of context: base namespace and so on to define the style sheets used
    * - elements add their layers of context: their selector context etc.
    *   - This would actually open us to building a tree "immediate-mode-style" that can be used for e.g. first-sibling etc. rules
    *   - Build out the whole context tree and then */

    val size: Int
        get() {
            return rules.size + children.size
        }

    override fun print(printer: Consumer<String>) {
        printer.accept("Rulesets")
        for (rule in rules) {
            printer.accept(rule.toString())
        }
        printer.accept("")
        printer.accept("Children Stylesheets")
        for ((index, sheet) in children.withIndex()) {
            val consumer: Consumer<String> = Consumer { s -> printer.accept("  Child $index: $s") }
            sheet.print(consumer)
        }
    }

    class Builder {
        private val rules: MutableList<Ruleset> = mutableListOf()
        private val children: MutableList<CssStyleSheet> = mutableListOf()

        fun ruleset(ruleset: Ruleset): Builder {
            rules.add(ruleset)
            return this
        }

        fun child(child: CssStyleSheet): Builder {
            children.add(child)
            return this
        }

        fun build(): CssStyleSheet {
            return CssStyleSheet(Collections.unmodifiableList(rules), Collections.unmodifiableList(children))
        }
    }
}