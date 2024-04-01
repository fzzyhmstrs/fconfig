package me.fzzyhmstrs.fzzy_config.util.pos

class ImmutablePos(private val p: Int = 0): Pos {
    override fun get(): Int {
        return p
    }
    override fun set(new: Int) {
    }
    override fun inc(amount: Int) {
    }
    override fun dec(amount: Int) {
    }
    override fun toString(): String {
        return "[$p]"
    }
}