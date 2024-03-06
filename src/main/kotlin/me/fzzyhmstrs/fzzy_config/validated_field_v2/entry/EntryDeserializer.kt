package me.fzzyhmstrs.fzzy_config.validated_field_v2.entry

import net.peanuuutz.tomlkt.TomlElement

/**
 * Deserializes individual entries in a complex [ValidatedField]
 *
 * SAM: [deserialize] takes a TomlElement, returns a deserialized instance of T
 * @author fzzyhmstrs
 * @since 0.1.1
 */
fun interface EntryDeserializer<T> {
    fun deserializeEntry(toml: TomlElement, fieldName: String): ValidationResult<T>
}
