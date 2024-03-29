package me.fzzyhmstrs.fzzy_config.entry

import net.peanuuutz.tomlkt.TomlElement

/**
 * Deserializes individual entries in a complex [Entry]
 *
 * SAM: [serializeEntry] takes a nullable input, errorbuilder list, and ignoreNonSync boolean, returns a serialized TomlElement. Expectation is that if input is null, the Entry will serialize its own stored T, otherwise it will handle serialization of the externally provied T
 * @param T the type of the Entry stored value
 * @author fzzyhmstrs
 * @since 0.1.1
 */
@FunctionalInterface
fun interface EntrySerializer<T> {
    fun serializeEntry(input: T?, errorBuilder: MutableList<String>, ignoreNonSync: Boolean): TomlElement
}