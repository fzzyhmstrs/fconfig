package me.fzzyhmstrs.fzzy_config.config

import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.entry.EntryDeserializer
import me.fzzyhmstrs.fzzy_config.entry.EntryKeyed
import me.fzzyhmstrs.fzzy_config.entry.EntrySerializer
import me.fzzyhmstrs.fzzy_config.impl.Walkable
import me.fzzyhmstrs.fzzy_config.util.Translatable
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import net.peanuuutz.tomlkt.TomlElement
import org.jetbrains.annotations.ApiStatus.Internal

/**
 * A section of a [Config]
 *
 * Use to organize related sub-topics of a larger config. Auto GUI creation will use sections to create separate "layers" of GUI Screens for the player to drill down into. Recommended practice is to group all sections at the beginning or end of a Config, so the section widgets in the GUI are grouped together, and the non-grouped entries are also all together.
 *
 * Follows the same rules as a Config regarding setting definitions. See the documentation for [Config] for details.
 * @sample me.fzzyhmstrs.fzzy_config.examples.ExampleTranslations.lang
 * @author fzzyhmstrs
 * @since 0.2.0
 */
open class ConfigSection: Walkable, EntryDeserializer<ConfigSection>, EntrySerializer<ConfigSection>, EntryKeyed, Translatable {

    private var sectionKey = "fc.config.generic.section"

    @Internal
    override fun serializeEntry(
        input: ConfigSection?,
        errorBuilder: MutableList<String>,
        flags: Byte
    ): TomlElement {
        return ConfigApi.serializeToToml(input ?: this, errorBuilder, flags)
    }

    @Internal
    override fun deserializeEntry(
        toml: TomlElement,
        errorBuilder: MutableList<String>,
        fieldName: String,
        flags: Byte
    ): ValidationResult<ConfigSection> {
        return ConfigApi.deserializeFromToml(this, toml, errorBuilder, flags).let { it.wrap(it.get().config) }
    }

    override fun translationKey(): String {
        return getEntryKey()
    }

    override fun descriptionKey(): String {
        return getEntryKey() + ".desc"
    }

    @Internal
    override fun getEntryKey(): String {
        return sectionKey
    }

    @Internal
    override fun setEntryKey(key: String) {
        sectionKey = key
    }

    override fun toString(): String {
        return ConfigApi.serializeConfig(this, mutableListOf())
    }
}
