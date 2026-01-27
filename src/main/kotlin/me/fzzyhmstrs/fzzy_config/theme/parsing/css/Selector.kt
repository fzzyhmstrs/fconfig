/*
 * Copyright (c) 2026 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.parsing.css

interface Selector {
    fun matches(context: SelectorContext): Boolean
    fun selector(): String

    data object Any: Selector {
        override fun matches(context: SelectorContext): Boolean {
            return true
        }

        override fun selector(): String {
            return "*"
        }
    }

    class All(private val selectors: List<Selector>): Selector {
        override fun matches(context: SelectorContext): Boolean {
            return selectors.all { it.matches(context) }
        }

        override fun selector(): String {
            return selectors.joinToString("")
        }
    }

    class And(private val left: Selector, private val right: Selector): Selector {
        override fun matches(context: SelectorContext): Boolean {
            return left.matches(context) && right.matches(context)
        }

        override fun selector(): String {
            return "$left$right"
        }
    }

    class Or(private val left: Selector, private val right: Selector): Selector {
        override fun matches(context: SelectorContext): Boolean {
            return left.matches(context) || right.matches(context)
        }

        override fun selector(): String {
            return "$left$right"
        }
    }
}