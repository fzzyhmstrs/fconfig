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
 * A [Pos] wth an offset supplier. Mutation of this pos will alter the offset.
 * @param p Int - the offset compared to the parent
 * @param offset Supplier<Int> - the supplied offset compared to the absolute position
 * @author fzzyhmstrs
 * @since 0.7.2
 */
open class OffsetSuppliedPos(private var p: Int, private val offset: Supplier<Int>): Pos {
    override fun get(): Int {
        return p + offset.get()
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
        return "Offset(${get()})[$p + ${offset.get()}]"
    }
}