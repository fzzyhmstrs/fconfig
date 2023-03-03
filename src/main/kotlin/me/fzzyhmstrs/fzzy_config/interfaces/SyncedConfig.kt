package me.fzzyhmstrs.fzzy_config.interfaces

import me.fzzyhmstrs.fzzy_config.interfaces.ServerClientSynced

interface SyncedConfig: ServerClientSynced {
    fun initConfig()
}
