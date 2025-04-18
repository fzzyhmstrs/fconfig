/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.util

import me.fzzyhmstrs.fzzy_config.entry.EntryKeyed
import me.fzzyhmstrs.fzzy_config.nullCast
import net.minecraft.text.MutableText

/**
 * A [Translatable] object that uses it's [EntryKeyed] key to build the translation/description/prefix keys.
 * @author fzzyhmstrs
 * @since 0.6.0
 */
@JvmDefaultWithoutCompatibility
interface TranslatableEntry: Translatable, EntryKeyed {

    var translatableEntryKey: String

    override fun setEntryKey(key: String) {
        this.translatableEntryKey = key
    }

    override fun getEntryKey(): String {
        return translatableEntryKey
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

    override fun translation(fallback: String?): MutableText {
        return Translatable.getScopedResult(this.getEntryKey())?.name?.nullCast() ?: super.translation(fallback)
    }

    override fun description(fallback: String?): MutableText {
        return Translatable.getScopedResult(this.getEntryKey())?.desc?.nullCast() ?: super.description(fallback)
    }

    override fun prefix(fallback: String?): MutableText {
        return Translatable.getScopedResult(this.getEntryKey())?.prefix?.nullCast() ?: super.prefix(fallback)
    }

}