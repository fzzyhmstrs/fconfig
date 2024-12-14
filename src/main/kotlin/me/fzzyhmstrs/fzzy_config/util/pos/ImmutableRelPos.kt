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
 * A relative [Pos]. Offsets a parent Pos. Mutation of this pos will alter the offset.
 * @param parent Pos - the Pos this is relative to
 * @param p Int - the offset compared to the parent
 * @author fzzyhmstrs
 * @since 0.2.0
 */
open class ImmutableRelPos @JvmOverloads constructor(private val parent: Pos, private val p: Int = 0): Pos {
    override fun get(): Int {
        return parent.get() + p
    }
    override fun set(new: Int) {
    }
    override fun inc(amount: Int) {
    }
    override fun dec(amount: Int) {
    }
    override fun toString(): String {
        return "[$parent + $p]"
    }
}