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

/**
 * wrapper interface for 4 basic handling interfaces
 * - serialize value
 * - deserialize inputs
 * - validate updates
 * - correct errors
 *
 * Interface method: [copyValue], handles providing deep copies of passed inputs (as deep as possible)
 * @param T the non-null type of the Entry stored value
 * @author fzzyhmstrs
 * @since 0.2.0, copyValue added 0.6.0
 */
interface EntryHandler<T>: EntrySerializer<T>, EntryDeserializer<T>, EntryValidator<T>, EntryCorrector<T> {
  
    fun copyValue(input: T): T {
        return T
    }
}
