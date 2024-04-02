package me.fzzyhmstrs.fzzy_config.util.pos

/**
 * Defines the mutable position of something
 * @author fzzyhmstrs
 * @since 0.2.0
 */
interface Pos {
    /**
     * Returns the position of this pos
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun get(): Int
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
}
