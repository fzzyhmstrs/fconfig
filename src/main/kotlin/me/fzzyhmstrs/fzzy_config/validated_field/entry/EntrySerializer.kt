package me.fzzyhmstrs.fzzy_config.validated_field.entry

import net.peanuuutz.tomlkt.TomlElement

/**
 * Deserializes individual entries in a complex [ValidatedField]
 *
 * SAM: [deserialize] takes a TomlElement, returns a deserialized instance of T
 * @author fzzyhmstrs
 * @since 0.1.1
 */
@FunctionalInterface
fun interface EntrySerializer<T> {
    fun serializeEntry(input: T?, errorBuilder: MutableList<String>, ignoreNonSync: Boolean): TomlElement
}