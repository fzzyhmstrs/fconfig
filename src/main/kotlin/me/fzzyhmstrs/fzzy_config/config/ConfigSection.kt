package me.fzzyhmstrs.fzzy_config.config

import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.api.Translatable
import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import me.fzzyhmstrs.fzzy_config.impl.Walkable
import me.fzzyhmstrs.fzzy_config.updates.UpdateKeyed
import me.fzzyhmstrs.fzzy_config.validated_field.entry.EntryDeserializer
import me.fzzyhmstrs.fzzy_config.validated_field.entry.EntrySerializer
import net.peanuuutz.tomlkt.TomlElement

/**
 * A section of a [Config]
 *
 * Use to organize related sub-topics of a larger config. Auto GUI creation will use sections to create separate "layers" of GUI for the player to drill down into. Recommended practice is to group all sections at the beginning or end of a Config, so the section widgets in the GUI are grouped together, and the non-grouped entries are also all together.
 *
 * Follows the same rules as a Config regarding setting definitions. See the documentation for []Config] for details.
 * @author fzzyhmstrs
 * @since 0.2.0
 */
open class ConfigSection: Walkable, EntryDeserializer<ConfigSection>, EntrySerializer<ConfigSection>, UpdateKeyed, Translatable {

    private var sectionKey = "fc.config.generic.section"

    override fun serializeEntry(
        input: ConfigSection?,
        errorBuilder: MutableList<String>,
        ignoreNonSync: Boolean
    ): TomlElement {
        return ConfigApi.serializeToToml(this, errorBuilder, ignoreNonSync)
    }

    override fun deserializeEntry(
        toml: TomlElement,
        errorBuilder: MutableList<String>,
        fieldName: String,
        ignoreNonSync: Boolean
    ): ValidationResult<ConfigSection> {
        return ConfigApi.deserializeFromToml(this, toml, errorBuilder, ignoreNonSync)
    }

    override fun translationKey(): String {
        return getUpdateKey()
    }

    override fun descriptionKey(): String {
        return getUpdateKey() + ".desc"
    }

    override fun getUpdateKey(): String {
        return sectionKey
    }

    override fun setUpdateKey(key: String) {
        sectionKey = key
    }

    override fun toString(): String {
        return ConfigApi.serializeConfig(this, mutableListOf())
    }
}