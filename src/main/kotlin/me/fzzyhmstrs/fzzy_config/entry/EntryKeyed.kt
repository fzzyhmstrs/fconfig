package me.fzzyhmstrs.fzzy_config.entry

import org.jetbrains.annotations.ApiStatus.Internal

/**
 * Provided with updates that can
 *
 * Internal to FzzyConfig. Used by [me.fzzyhmstrs.fzzy_config.updates.Updatable] and [me.fzzyhmstrs.fzzy_config.config.ConfigSection].
 *
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@Internal
internal interface EntryKeyed {
    fun getEntryKey(): String //returned by the Updatable element to denote its place in the heirarchy.
    fun setEntryKey(key: String) //used by the config validator to set the elements key. Only done on CONFIGURATION sync on the client side. UpdateManager doesn't do ish on the Server
}