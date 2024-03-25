package me.fzzyhmstrs.fzzy_config.util.pos

class AbsPos(private var p: Int = 0): Pos {
    override fun get(): Int {
        return p
    }
    override fun set(new: Int) {
        p = new
    }

    override fun toString(): String {
        return "[$p]"
    }
}