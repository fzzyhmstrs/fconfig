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
    fun specificity(): Specificity
    fun flatten(): List<Selector> {
        return listOf(this)
    }
    fun collect(): Selector {
        return this
    }

    data object Universal: Selector {
        override fun matches(context: SelectorContext): Boolean {
            return true
        }

        override fun selector(): String {
            return "*"
        }

        override fun specificity(): Specificity {
            return Specificity.ZERO
        }
    }

    class All(private val selectors: List<Selector>): Selector {

        private val specificity = selectors.fold(Specificity.ZERO) { sp, s -> sp + s.specificity() }

        override fun matches(context: SelectorContext): Boolean {
            return selectors.all { it.matches(context) }
        }

        override fun selector(): String {
            return selectors.joinToString("")
        }

        override fun specificity(): Specificity {
            return specificity
        }

        override fun flatten(): List<Selector> {
            return selectors.flatMap { it.flatten() }
        }

        override fun collect(): Selector {
            return All(flatten())
        }
    }

    class Any(private val selectors: List<Selector>): Selector {

        private val specificity = selectors.fold(Specificity.ZERO) { sp, s -> sp + s.specificity() }

        override fun matches(context: SelectorContext): Boolean {
            return selectors.any { it.matches(context) }
        }

        override fun selector(): String {
            return selectors.joinToString("")
        }

        override fun specificity(): Specificity {
            return specificity
        }

        override fun flatten(): List<Selector> {
            return selectors.flatMap { it.flatten() }
        }

        override fun collect(): Selector {
            return Any(flatten())
        }
    }

    class And(private val left: Selector, private val right: Selector): Selector {

        private val specificity = left.specificity() + right.specificity()

        override fun matches(context: SelectorContext): Boolean {
            return left.matches(context) && right.matches(context)
        }

        override fun selector(): String {
            return "$left$right"
        }

        override fun specificity(): Specificity {
            return specificity
        }

        override fun flatten(): List<Selector> {
            return left.flatten() + right.flatten()
        }

        override fun collect(): Selector {
            return All(flatten())
        }
    }

    class Or(private val left: Selector, private val right: Selector): Selector {

        private val specificity = left.specificity() + right.specificity()

        override fun matches(context: SelectorContext): Boolean {
            return left.matches(context) || right.matches(context)
        }

        override fun selector(): String {
            return "$left$right"
        }

        override fun specificity(): Specificity {
            return specificity
        }

        override fun flatten(): List<Selector> {
            return left.flatten() + right.flatten()
        }

        override fun collect(): Selector {
            return Any(flatten())
        }
    }
}