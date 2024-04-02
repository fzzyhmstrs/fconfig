package me.fzzyhmstrs.fzzy_config.util.pos

/**
 * An absolute [Pos]. Mutation of this pos will directly change it's position with no other offsets or other side effects.
 * @param p Int - this Pos's position
 * @author fzzyhmstrs
 * @since 0.2.0
 */
class AbsPos(private var p: Int = 0): Pos {
    override fun get(): Int {
        return p
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
        return "[$p]"
    }
}
