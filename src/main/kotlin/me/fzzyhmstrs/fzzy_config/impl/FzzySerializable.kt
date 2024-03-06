package me.fzzyhmstrs.fzzy_config.impl

import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import net.peanuuutz.tomlkt.TomlElement

/**
 * Implementing Class will be automatically de/serialized by [ConfigHelper] as part of config de/serialization.
 *
 * Internal to FzzyConfig. Used by [ValidatedField] and [ConfigSection]
 *
 * @author fzzyhsmtrs
 * @since 0.2.0
 */
internal interface FzzySerializable {
    fun serialize(errorBuilder: MutableList<String>, ignoreNonSync: Boolean = true): TomlElement
    fun deserialize(toml: TomlElement, errorBuilder: MutableList<String>, fieldName: String, ignoreNonSync: Boolean = true): ValidationResult<Boolean>
}