package me.fzzyhmstrs.fzzy_config.util.pos

import java.util.function.Supplier

class SuppliedPos(private val parent: Pos, private var p: Int = 0, private val offset: Supplier<Int>): Pos {
    override fun get(): Int {
        return parent.get() + p + offset.get()
    }
    override fun set(new: Int) {
        p = new
    }
    override fun toString(): String {
        return "[$parent + $p + ${offset.get()}]"
    }
}