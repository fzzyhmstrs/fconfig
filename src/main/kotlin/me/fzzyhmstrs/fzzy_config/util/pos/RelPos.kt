package me.fzzyhmstrs.fzzy_config.util.pos

class RelPos(private val parent: Pos, private var p: Int = 0): Pos {
    override fun get(): Int {
        return parent.get() + p
    }
    override fun set(new: Int) {
        p = new
    }
    override fun toString(): String {
        return "[$parent + $p]"
    }
}