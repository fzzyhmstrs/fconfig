package me.fzzyhmstrs.fzzy_config.validation.entry

import me.fzzyhmstrs.fzzy_config.api.ValidationResult
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
    fun deserializeEntry(toml: TomlElement, errorBuilder: MutableList<String>, fieldName: String, ignoreNonSync: Boolean): ValidationResult<T>
}