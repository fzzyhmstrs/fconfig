package me.fzzyhmstrs.fzzy_config.config

import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import me.fzzyhmstrs.fzzy_config.impl.Walkable
import me.fzzyhmstrs.fzzy_config.validated_field.entry.EntryDeserializer
import me.fzzyhmstrs.fzzy_config.validated_field.entry.EntrySerializer
import net.peanuuutz.tomlkt.TomlElement

/**
 *
 */
open class ConfigSection: Walkable, EntryDeserializer<ConfigSection>, EntrySerializer<ConfigSection> {

    override fun serializeEntry(
        input: ConfigSection?,
        errorBuilder: MutableList<String>,
        ignoreNonSync: Boolean
    ): TomlElement {
        return ConfigApi.serializeToToml(this,errorBuilder,ignoreNonSync)
    }

    override fun deserializeEntry(
        toml: TomlElement,
        errorBuilder: MutableList<String>,
        fieldName: String,
        ignoreNonSync: Boolean
    ): ValidationResult<ConfigSection> {
        return ConfigApi.deserializeFromToml(this, toml, errorBuilder, ignoreNonSync)
    }

    override fun toString(): String {
        return ConfigApi.serializeConfig(this, mutableListOf())
    }
}