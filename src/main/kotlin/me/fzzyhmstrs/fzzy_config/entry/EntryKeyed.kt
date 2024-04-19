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

import org.jetbrains.annotations.ApiStatus.Internal

@Internal
interface EntryKeyed {
    fun getEntryKey(): String //returned by the Updatable element to denote its place in the hierarchy.
    fun setEntryKey(key: String) //used by the config validator to set the elements key. Only done on CONFIGURATION sync on the client side. UpdateManager doesn't do ish on the Server
}