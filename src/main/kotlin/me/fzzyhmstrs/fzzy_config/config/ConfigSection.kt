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
import me.fzzyhmstrs.fzzy_config.cast
import me.fzzyhmstrs.fzzy_config.entry.*
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.screen.entry.EntryCreators
import me.fzzyhmstrs.fzzy_config.screen.widget.TextureDeco
import me.fzzyhmstrs.fzzy_config.util.TranslatableEntry
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.Walkable
import net.peanuuutz.tomlkt.TomlElement
import org.jetbrains.annotations.ApiStatus.Internal

/**
 * A section of a [Config]
 *
 * Use to organize related sub-topics of a larger config. Auto GUI creation will use sections to create separate "layers" of GUI Screens for the player to drill down into. Recommended practice is to group all sections at the beginning or end of a Config, so the section widgets in the GUI are grouped together, and the non-grouped entries are also all together.
 *
 * Follows the same rules as a Config regarding setting definitions. See the documentation for [Config] for details.
 *
 * [Laying Out Configs](https://moddedmc.wiki/en/project/fzzy-config/docs/config-design/Laying-out-Configs) has more information about how sections can be best utilized.
 * @author fzzyhmstrs
 * @since 0.2.0
 */
open class ConfigSection: Walkable, EntryDeserializer<ConfigSection>, EntrySerializer<ConfigSection>, TranslatableEntry, EntryParent, EntryAnchor, EntryCreator {

    @Transient
    @Internal
    final override var translatableEntryKey: String = "fc.config.generic.section"

    @Internal
    @Deprecated("Implement the override using ValidationResult.ErrorEntry.Mutable. Scheduled for removal in 0.8.0.",
        ReplaceWith("serializeEntry(input, flags).get()")
    )
    final override fun serializeEntry(input: ConfigSection?, errorBuilder: MutableList<String>, flags: Byte): TomlElement {
        return serializeEntry(input, flags).log { s, _ -> errorBuilder.add(s) }.get()
    }

    @Internal
    final override fun serializeEntry(input: ConfigSection?, flags: Byte): ValidationResult<TomlElement> {
        return ConfigApiImpl.serializeToToml(input ?: this, "Error(s) encountered serializing config section", flags).cast()
    }

    @Internal
    @Deprecated("Implement the override without an errorBuilder. Scheduled for removal in 0.8.0. In 0.7.0, the provided ValidationResult should encapsulate all encountered errors, and all passed errors will be incorporated into a parent result as applicable.",
        ReplaceWith("deserializeEntry(toml, fieldName, flags)")
    )
    final override fun deserializeEntry(toml: TomlElement, errorBuilder: MutableList<String>, fieldName: String, flags: Byte): ValidationResult<ConfigSection> {
        return deserializeEntry(toml, fieldName, flags).log { s, _ -> errorBuilder.add(s) }
    }

    @Internal
    final override fun deserializeEntry(toml: TomlElement, fieldName: String, flags: Byte): ValidationResult<ConfigSection> {
        val result = ConfigApiImpl.deserializeFromToml(this, toml, "Error(s) encountered deserializing config section", flags)
        return result
    }

    /**
     * Anchor modifier method for a section. By default, provides a "map" icon decoration to the base anchor. You can provide a custom icon if you want a special icon for the config in the goto menu. If your config has a long name, you may also want to create and provide a shortened "summary" name for a goto link.
     *
     * Super should be used to generate the anchor you modify, like: `return super.anchorEntry(anchor).name(myNewName).decoration(myNewDecoration)`
     * @param anchor [EntryAnchor.Anchor] automatically generated input Anchor for modification.
     * @return Anchor with any desired modifications.
     * @see [TextureDeco] for other built in icons
     * @see [me.fzzyhmstrs.fzzy_config.screen.decoration.SpriteDecoration] for a simple class to build your own icon
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    override fun anchorEntry(anchor: EntryAnchor.Anchor): EntryAnchor.Anchor {
        return anchor.decoration(TextureDeco.DECO_MAP).type(EntryAnchor.AnchorType.SECTION)
    }

    final override fun anchorId(scope: String): String {
        return translatableEntryKey
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