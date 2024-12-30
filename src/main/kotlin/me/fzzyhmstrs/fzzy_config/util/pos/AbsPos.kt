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
 * An absolute [Pos]. Mutation of this pos will directly change it's position with no other offsets or other side effects.
 * @param p Int - this Pos's position
 * @author fzzyhmstrs
 * @since 0.2.0
 */
class AbsPos(private var p: Int = 0): Pos {
    override fun get(): Int {
        return p
    }
    override fun set(new: Int) {
        p = new
    }
    override fun inc(amount: Int) {
        p += amount
    }
    override fun dec(amount: Int) {
        p -= amount
    }
    override fun toString(): String {
        return "Abs[$p]"
    }
}