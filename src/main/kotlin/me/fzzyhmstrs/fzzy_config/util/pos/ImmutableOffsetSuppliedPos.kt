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
 * A relative [Pos] wth an offset supplier and a constant offset. Offsets a parent Pos. This position is immutable in the direct sense. Mutation of this pos via [set], [inc], and [dec] does *not* affect position.
 * @param parent Pos - the Pos this is relative to
 * @param p Int - constant numeric offset
 * @param offset Supplier<Int> - the supplied offset compared to the parent
 * @author fzzyhmstrs
 * @since 0.7.2
 */
open class ImmutableOffsetSuppliedPos(protected val parent: Pos, protected val p: Int, protected val offset: Supplier<Int>): Pos.SuppliedPos {
    override fun get(): Int {
        return parent.get() + p + offset.get()
    }
    override fun set(new: Int) {
    }
    override fun inc(amount: Int) {
    }
    override fun dec(amount: Int) {
    }
    override fun toString(): String {
        return "ImmutableOffset(${get()})[$parent + $p + ${offset.get()}]"
    }

    override fun supplier(): Supplier<Int> {
        return offset
    }

    override fun parent(): Pos {
        return parent
    }

    override fun offset(): Int {
        return p
    }

    companion object {
        fun optimized(parent: Pos, p: Int, offset: Supplier<Int>): Pos {
            return when (parent) {
                is Pos.SuppliedPos -> ImmutableOffsetSuppliedPos(parent.parent(), parent.offset() + p) { parent.supplier().get() + offset.get() }
                is Pos.ParentPos -> ImmutableOffsetSuppliedPos(parent.parent(), parent.offset() + p, offset)
                is Pos.RootPos -> OffsetSuppliedPos(parent.get() + p, offset)
                else -> ImmutableOffsetSuppliedPos(parent, p, offset)
            }
        }
    }
}