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
 * Defines the mutable position of something
 * @author fzzyhmstrs
 * @since 0.2.0
 */
interface Pos: Supplier<Int> {
    /**
     * Returns the position of this pos
     * @author fzzyhmstrs
     * @since 0.2.0, originates from the Supplier interface as of 0.5.9
     */
    override fun get(): Int
    /**
     * Sets the position of this pos
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun set(new: Int)
    /**
     * Increments this position by the given amount
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun inc(amount: Int)
    /**
     * Decrements this position by the given amount
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun dec(amount: Int)

    operator fun plus(other: Pos): Int {
        return this.get() + other.get()
    }

    operator fun minus(other: Pos): Int {
        return this.get() - other.get()
    }

    operator fun times(other: Pos): Int {
        return this.get() * other.get()
    }

    operator fun div(other: Pos): Int {
        return this.get() / other.get()
    }

    operator fun rem(other: Pos): Int {
        return this.get() % other.get()
    }

    interface ParentPos: Pos {
        fun parent(): Pos
        fun offset(): Int
    }

    interface RootPos: Pos

    interface SuppliedPos: ParentPos {
        fun supplier(): Supplier<Int>
    }

    companion object {
        /**
         * Reference zero position, good for a default value
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        val ZERO = ImmutablePos(0)
    }
}