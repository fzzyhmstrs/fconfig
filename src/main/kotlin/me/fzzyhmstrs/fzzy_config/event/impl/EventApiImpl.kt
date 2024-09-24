/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.event.impl

import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.event.api.*
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier

internal object EventApiImpl: EventApi {

    private val onSyncClientListeners: MutableList<OnSyncClientListener> = mutableListOf()
    private val onSyncServerListeners: MutableList<OnSyncServerListener> = mutableListOf()
    private val onUpdateClientListeners: MutableList<OnUpdateClientListener> = mutableListOf()
    private val onUpdateServerListeners: MutableList<OnUpdateServerListener> = mutableListOf()

    /////////////////////

    override fun onSyncClient(listener: OnSyncClientListener) {
        onSyncClientListeners.add(listener)
    }

    override fun onSyncServer(listener: OnSyncServerListener) {
        onSyncServerListeners.add(listener)
    }

    override fun onUpdateClient(listener: OnUpdateClientListener) {
        onUpdateClientListeners.add(listener)
    }

    override fun onUpdateServer(listener: OnUpdateServerListener) {
        onUpdateServerListeners.add(listener)
    }

    /////////////////////

    internal fun fireOnSyncClient(id: Identifier, config: Config) {
        for (listener in onSyncClientListeners) {
            listener.onSync(id, config)
        }
    }

    internal fun fireOnSyncServer(id: Identifier, config: Config) {
        for (listener in onSyncServerListeners) {
            listener.onSync(id, config)
        }
    }

    internal fun fireOnUpdateClient(id: Identifier, config: Config) {
        for (listener in onUpdateClientListeners) {
            listener.onChanged(id, config)
        }
    }

    internal fun fireOnUpdateServer(id: Identifier, config: Config, player: ServerPlayerEntity) {
        for (listener in onUpdateServerListeners) {
            listener.onChanged(id, config, player)
        }
    }
}