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

import me.fzzyhmstrs.fzzy_config.util.ValidationResult

/**
 * interface for listening to entry changes
 *
 * SAM: [listenToEntry] attached a listener to the entry.
 * @param T the non-null type of the Entry stored value
 * @author fzzyhmstrs
 * @since 0.6.0
 */
@FunctionalInterface
fun interface EntryListener<T> {

    fun listenToEntry(listener: Consumer<EntryListener<T>>)
  
}
