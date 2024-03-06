package me.fzzyhmstrs.fzzy_config.config

import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import me.fzzyhmstrs.fzzy_config.impl.FzzySerializable
import me.fzzyhmstrs.fzzy_config.impl.Walkable
import net.peanuuutz.tomlkt.TomlElement

/**
 *
 */
open class ConfigSection: FzzySerializable, Walkable {

    override fun serialize(errorBuilder: MutableList<String>, ignoreNonSync: Boolean): TomlElement {
        return ConfigApi.serializeToToml(this,errorBuilder,ignoreNonSync)
    }

    override fun deserialize(
        toml: TomlElement,
        errorBuilder: MutableList<String>,
        fieldName: String,
        ignoreNonSync: Boolean
    ): ValidationResult<Boolean> {
        val result = ConfigApi.deserializeFromToml(this, toml, errorBuilder, ignoreNonSync)
        return if (result.isError()) return ValidationResult.error(true, result.getError()) else ValidationResult.success(false)
    }

    override fun toString(): String {
        return ConfigApi.serializeConfig(this, mutableListOf())
    }
}