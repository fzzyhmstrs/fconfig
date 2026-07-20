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

class Specificity(val id: Int, val clazz: Int, val type: Int): Comparable<Specificity> {

    companion object {
        private val comparator = Comparator.comparingInt<Specificity> { s ->
            s.id }.thenComparing(Comparator.comparingInt<Specificity> { s ->
                s.clazz }.thenComparing(Comparator.comparingInt { s ->
                    s.type }
                )
            )

        val ZERO = Specificity(0, 0, 0)
        val ID = id()
        val CLASS = clazz()
        val TYPE = type()

        fun id(id: Int = 1): Specificity {
            return Specificity(id, 0, 0)
        }

        fun clazz(clazz: Int = 1): Specificity {
            return Specificity(0, clazz, 0)
        }

        fun type(type: Int = 1): Specificity {
            return Specificity(0, 0, type)
        }
    }

    override fun compareTo(other: Specificity): Int {
        return comparator.compare(this, other)
    }

    operator fun plus(other: Specificity): Specificity {
        return Specificity(this.id + other.id, this.clazz + other.clazz, this.type + other.type)
    }
}