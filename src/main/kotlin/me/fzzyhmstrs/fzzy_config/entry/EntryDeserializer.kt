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
import net.peanuuutz.tomlkt.TomlElement

/**
 * Deserializes individual entries in a complex [Entry]
 *
 * SAM: [deserializeEntry] takes a TomlElement, errorBuilder list, fieldName, and ignoreNonSync boolean, returns [ValidationResult]<T> with the deserialized or fallback value
 * @param T the type of the Entry stored value
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@FunctionalInterface
fun interface EntryDeserializer<T> {
    fun deserializeEntry(toml: TomlElement, errorBuilder: MutableList<String>, fieldName: String, flags: Byte): ValidationResult<T>
    fun deserializedChanged(old: Any?, new: Any?): Boolean {
        return old != new
    }
}