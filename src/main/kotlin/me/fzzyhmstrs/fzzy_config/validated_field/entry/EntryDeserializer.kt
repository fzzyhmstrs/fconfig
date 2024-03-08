package me.fzzyhmstrs.fzzy_config.validated_field.entry

import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import net.peanuuutz.tomlkt.TomlElement

/**
 * Deserializes individual entries in a complex [ValidatedField]
 *
 * SAM: [deserialize] takes a TomlElement, returns a deserialized instance of T
 * @author fzzyhmstrs
 * @since 0.1.1
 */
@FunctionalInterface
fun interface EntryDeserializer<T> {
    fun deserializeEntry(toml: TomlElement, errorBuilder: MutableList<String>, fieldName: String, ignoreNonSync: Boolean): ValidationResult<T>
}