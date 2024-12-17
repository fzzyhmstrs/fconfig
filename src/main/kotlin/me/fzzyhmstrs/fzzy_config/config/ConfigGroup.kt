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

import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.entry.*
import me.fzzyhmstrs.fzzy_config.screen.entry.EntryCreators
import me.fzzyhmstrs.fzzy_config.screen.widget.TextureDeco
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.Translatable
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.function.Supplier

/**
 * Defines the start of a config group
 *
 * This could be used to link to a wiki, open another non-fzzy-config config, open a non-config screen, open a patchouli guide, run a command, and so on.
 * @author fzzyhmstrs
 * @since 0.6.0
 */
open class ConfigGroup @JvmOverloads constructor(private val groupName: String = ""): Translatable, EntryKeyed, EntryAnchor, EntryCreator, EntryPermissible {

    @Transient
    private var groupKey = "fc.config.generic.group"

    @Internal
    override fun getEntryKey(): String {
        return groupKey
    }

    @Internal
    override fun setEntryKey(key: String) {
        sectionKey = key
    }

    override fun translationKey(): String {
        return getEntryKey()
    }

    override fun descriptionKey(): String {
        return getEntryKey() + ".desc"
    }

    override fun prefixKey(): String {
        return getEntryKey() + ".prefix"
    }
  
    override fun anchorEntry(anchor: EntryAnchor.Anchor): EntryAnchor.Anchor {
        return anchor.decoration(TextureDeco.DECO_LIST)
    }

    override fun prepare(scope: String, groups: LinkedList<String>, annotations: List<Annotation>, globalAnnotations: List<Annotation>) {
        val fieldName = if (groupName != "") groupName else context.scope.substringAfterLast('.')
        context.groups.push(fieldName)
    }
    
    override fun createEntry(context: EntryCreator.CreatorContext): List<EntryCreator.Creator> {
        val fieldName = if (groupName != "") groupName else context.scope.substringAfterLast('.')
        return EntryCreators.createGroupEntry(context, fieldName)
    }

    @Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
    annotation class Pop

    companion object {
        fun pop(annotations: List<Annotation>, groups: LinkedList<String>) {
            if (annotations.firstOrNull { it is Pop } != null) {
                groups.poll()
            }
        }
    }
}
