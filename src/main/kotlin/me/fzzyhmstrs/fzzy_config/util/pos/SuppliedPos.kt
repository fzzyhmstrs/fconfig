package me.fzzyhmstrs.fzzy_config.util.pos

import java.util.function.Supplier

/**
 * A relative [Pos] wth a secondary offset supplier. Offsets a parent Pos. Mutation of this pos will alter the offset.
 * @param parent Pos - the Pos this is relative to
 * @param p Int - the offset compared to the parent
 * @param offset Supplier<Int> - the secondary supplied offset compared to the parent
 * @author fzzyhmstrs
 * @since 0.2.0
 */
class SuppliedPos(private val parent: Pos, private var p: Int, private val offset: Supplier<Int>): Pos {
    override fun get(): Int {
        return parent.get() + p + offset.get()
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
        return "[$parent + $p + ${offset.get()}]"
    }
}
