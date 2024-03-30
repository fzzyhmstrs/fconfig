package me.fzzyhmstrs.fzzy_config.entry

import org.jetbrains.annotations.ApiStatus.Internal

@Internal
interface EntryKeyed {
    fun getEntryKey(): String //returned by the Updatable element to denote its place in the hierarchy.
    fun setEntryKey(key: String) //used by the config validator to set the elements key. Only done on CONFIGURATION sync on the client side. UpdateManager doesn't do ish on the Server
}