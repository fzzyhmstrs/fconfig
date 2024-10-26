/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.entry

import java.util.function.Consumer
import java.util.function.Supplier

/**
 * A base Entry for configs.
 *
 * Performs 8 basic functions
 * - serialize contents
 * - deserialize input
 * - validate updates
 * - correct errors
 * - provide widgets
 * - apply inputs
 * - supply outputs
 * - create instances
 * - manages flags
 *
 * @param T the non-null type of the Entry stored value
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@JvmDefaultWithCompatibility
interface Entry<T, E: Entry<T, E>>: EntryHandler<T>, EntryWidget<T>, EntryFlag, Consumer<T>, Supplier<T> {
    fun instanceEntry(): E
    fun isValidEntry(input: Any?): Boolean
    fun trySet(input: Any?)

}