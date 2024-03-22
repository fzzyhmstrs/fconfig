package me.fzzyhmstrs.fzzy_config.updates

import org.jetbrains.annotations.ApiStatus.Internal

/**
 * Provided with updates that can
 *
 * Internal to FzzyConfig. Used by [Updatable] and [ConfigSection].
 *
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@Internal
internal interface UpdateKeyed {
    fun getUpdateKey(): String //returned by the Updatable element to denote its place in the heirarchy.
    fun setUpdateKey(key: String) //used by the config validator to set the elements key. Only done on CONFIGURATION sync on the client side. UpdateManager doesn't do ish on the Server
}