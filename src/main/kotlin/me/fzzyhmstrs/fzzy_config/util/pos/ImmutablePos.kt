/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.util.pos

/**
 * An immutable [Pos]. Typically used as a position "anchor" in other positions
 * @param p Int - this Pos's position
 * @author fzzyhmstrs
 * @since 0.2.0
 */
class ImmutablePos(private val p: Int = 0): Pos {
    override fun get(): Int {
        return p
    }
    override fun set(new: Int) {
    }
    override fun inc(amount: Int) {
    }
    override fun dec(amount: Int) {
    }
    override fun toString(): String {
        return "Immutable[$p]"
    }
}