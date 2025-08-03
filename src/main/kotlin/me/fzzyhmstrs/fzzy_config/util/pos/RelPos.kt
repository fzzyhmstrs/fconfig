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
open class RelPos @JvmOverloads constructor(private val parent: Pos, private var p: Int = 0): Pos.ParentPos {
    override fun get(): Int {
        return parent.get() + p
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
        return "Rel(${get()})[$parent + $p]"
    }


    override fun parent(): Pos {
        return parent
    }
    override fun offset(): Int {
        return p
    }


    companion object {
        fun optimized(parent: Pos, p: Int = 0): Pos {
            return when (parent) {
                is Pos.SuppliedPos -> RelPos(parent, p)
                is Pos.ParentPos -> RelPos(parent.parent(), p + parent.offset())
                is Pos.RootPos -> AbsPos(parent.get() + p)
                else -> RelPos(parent, p)
            }
        }
    }
}