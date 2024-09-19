/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.entry

import me.fzzyhmstrs.fzzy_config.annotations.Action
import net.peanuuutz.tomlkt.TomlElement

/**
 * An entry that is a parent of other entries
 * @author fzzyhmstrs
 * @since 0.1.1
 */
fun interface EntryParent {
    fun actions(): Set<Action>
}