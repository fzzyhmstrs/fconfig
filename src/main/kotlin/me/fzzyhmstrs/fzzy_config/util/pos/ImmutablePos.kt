package me.fzzyhmstrs.fzzy_config.util.pos

/**
 * An immutable [Pos]. Typically used as a position "anchor" in other positions
 * @author fzzyhmstrs
 * @since 0.2.0
 */
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
