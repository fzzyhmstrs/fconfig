/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.util.pos

import java.util.function.Supplier

/**
 * An immutable [Pos] based on a reference value supplier. Mutation of this pos will have no effect.
 * @param reference Supplier - the reference supplied value
 * @author fzzyhmstrs
 * @since 0.6.0
 */
class ReferencePos constructor(private val reference: Supplier<Int>): Pos {
    override fun get(): Int {
        return reference.get()
    }
    override fun set(new: Int) {
    }
    override fun inc(amount: Int) {
    }
    override fun dec(amount: Int) {
    }
    override fun toString(): String {
        return "[${reference.get()}]"
    }
}