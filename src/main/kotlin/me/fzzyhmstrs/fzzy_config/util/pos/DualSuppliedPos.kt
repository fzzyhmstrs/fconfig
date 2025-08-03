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

import java.util.function.Supplier

/**
 * A relative [Pos] wth an offset supplier. Offsets a parent Supplier. This position is immutable in the direct sense. Mutation of this pos via [set], [inc], and [dec] does *not* affect position.
 * @param parent Supplier<Int> - the supplied base position this is relative to
 * @param offset Supplier<Int> - the supplied offset compared to the parent
 * @author fzzyhmstrs
 * @since 0.7.2
 */
open class DualSuppliedPos(protected val parent: Supplier<Int>, protected val offset: Supplier<Int>): Pos {
    override fun get(): Int {
        return parent.get() + offset.get()
    }
    override fun set(new: Int) {
    }
    override fun inc(amount: Int) {
    }
    override fun dec(amount: Int) {
    }
    override fun toString(): String {
        return "DualSupplied(${get()})[${parent.get()} + ${offset.get()}]"
    }
}