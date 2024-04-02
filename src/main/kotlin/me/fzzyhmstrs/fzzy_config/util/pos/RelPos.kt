package me.fzzyhmstrs.fzzy_config.util.pos
/**
 * A relative [Pos]. Offsets a parent Pos. Mutation of this pos will alter the offset.
 * @param parent Pos - the Pos this is relative to
 * @param p Int - the offset compared to the parent
 * @author fzzyhmstrs
 * @since 0.2.0
 */
class RelPos @JvmOverloads constructor(private val parent: Pos, private var p: Int = 0): Pos {
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
        return "[$parent + $p]"
    }
}
