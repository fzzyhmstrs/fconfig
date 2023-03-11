package me.fzzyhmstrs.fzzy_config.interfaces

import me.fzzyhmstrs.fzzy_config.interfaces.ServerClientSynced

/**
 * Base interface for classes that want to be server-client synced.
 *
 * SAM: [initConfig] use as a convenience method for putting registration and other initialization tasks and call in a ModInitializer
 */
interface SyncedConfig: ServerClientSynced {
    fun initConfig()
}
