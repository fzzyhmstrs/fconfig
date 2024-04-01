package me.fzzyhmstrs.fzzy_config.util.pos

interface Pos {
    fun get(): Int
    fun set(new: Int)
    fun inc(amount: Int)
    fun dec(amount: Int)
}