/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.config

import me.fzzyhmstrs.fzzy_config.annotations.Action
import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.entry.*
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.screen.entry.EntryCreators
import me.fzzyhmstrs.fzzy_config.screen.widget.TextureDeco
import me.fzzyhmstrs.fzzy_config.util.Translatable
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.contextualize
import me.fzzyhmstrs.fzzy_config.util.Walkable
import net.peanuuutz.tomlkt.TomlElement
import org.jetbrains.annotations.ApiStatus.Internal

/**
 * A section of a [Config]
 *
 * Use to organize related sub-topics of a larger config. Auto GUI creation will use sections to create separate "layers" of GUI Screens for the player to drill down into. Recommended practice is to group all sections at the beginning or end of a Config, so the section widgets in the GUI are grouped together, and the non-grouped entries are also all together.
 *
 * Follows the same rules as a Config regarding setting definitions. See the documentation for [Config] for details.
 * @author fzzyhmstrs
 * @since 0.2.0
 */
open class ConfigSection: Walkable, EntryDeserializer<ConfigSection>, EntrySerializer<ConfigSection>, Translatable, EntryKeyed, EntryParent, EntryAnchor, EntryCreator {

    @Transient
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
        return ConfigApi.deserializeFromToml(this, toml, errorBuilder, flags).contextualize()
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

    override fun anchorEntry(anchor: EntryAnchor.Anchor): EntryAnchor.Anchor {
        return anchor.decoration(TextureDeco.DECO_MAP)
    }

    override fun createEntry(context: EntryCreator.CreatorContext): List<EntryCreator.Creator> {
        return EntryCreators.createSectionEntry(context)
    }

    override fun actions(): Set<Action> {
        return ConfigApiImpl.getActions(this, ConfigApiImpl.IGNORE_NON_SYNC)
    }

    override fun toString(): String {
        return ConfigApi.serializeConfig(this, mutableListOf())
    }
}