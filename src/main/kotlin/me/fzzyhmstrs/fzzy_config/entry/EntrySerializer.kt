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

import net.peanuuutz.tomlkt.TomlElement

/**
 * Deserializes individual entries in a complex [Entry]
 *
 * SAM: [serializeEntry] takes a nullable input, error builder list, and ignoreNonSync boolean, returns a serialized TomlElement. Expectation is that if input is null, the Entry will serialize its own stored T, otherwise it will handle serialization of the externally provied T
 * @param T the type of the Entry stored value
 * @author fzzyhmstrs
 * @since 0.1.1
 */
@FunctionalInterface
fun interface EntrySerializer<T> {
    //TODO
    fun serializeEntry(input: T?, errorBuilder: MutableList<String>, flags: Byte): TomlElement
}