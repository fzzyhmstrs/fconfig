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

import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.event.api.*
import me.fzzyhmstrs.fzzy_config.event.api.v2.OnUpdateServerListener as OnUpdateServerListenerV2
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier

internal object EventApiImpl: EventApi {

    private val onSyncClientListeners: MutableList<OnSyncClientListener> = mutableListOf()
    private val onSyncServerListeners: MutableList<OnSyncServerListener> = mutableListOf()
    private val onUpdateClientListeners: MutableList<OnUpdateClientListener> = mutableListOf()
    @Deprecated("Soft-remove by 0.8.0, remove by 0.9.0")
    private val onUpdateServerListeners: MutableList<OnUpdateServerListener> = mutableListOf()
    private val onUpdateServerListenersNew: MutableList<OnUpdateServerListenerV2> = mutableListOf()
    private val onRegisteredClientListeners: MutableMap<Identifier, MutableList<OnRegisteredClientListener>> = hashMapOf()
    private val onRegisteredServerListeners: MutableMap<Identifier, MutableList<OnRegisteredServerListener>> = hashMapOf()

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

    @Deprecated("Soft-remove by 0.8.0, remove by 0.9.0")
    override fun onUpdateServer(listener: OnUpdateServerListener) {
        onUpdateServerListeners.add(listener)
    }

    override fun onUpdateServer(listener: OnUpdateServerListenerV2) {
        onUpdateServerListenersNew.add(listener)
    }

    override fun onRegisteredClient(configId: Identifier, listener: OnRegisteredClientListener) {
        if (ConfigApiImpl.isClientConfigLoaded(configId)) {
            val config = ConfigApiImpl.getClientConfig(configId)
            if (config == null) {
                FC.LOGGER.error("Unexpected error encountered while listening to client config registration: Config $configId was null")
                return
            }
            listener.onRegistered(config)
        } else {
            onRegisteredClientListeners.compute(configId) { _, list ->
                list?.apply { add(listener) } ?: mutableListOf(listener)
            }
        }
    }

    override fun onRegisteredServer(configId: Identifier, listener: OnRegisteredServerListener) {
        if (ConfigApiImpl.isSyncedConfigLoaded(configId)) {
            val config = ConfigApiImpl.getSyncedConfig(configId)
            if (config == null) {
                FC.LOGGER.error("Unexpected error encountered while listening to server config registration: Config $configId was null")
                return
            }
            listener.onRegistered(config)
        } else {
            onRegisteredServerListeners.compute(configId) { _, list ->
                list?.apply { add(listener) } ?: mutableListOf(listener)
            }
        }
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

    @Deprecated("Soft-remove by 0.8.0, remove by 0.9.0")
    internal fun fireOnUpdateServer(id: Identifier, config: Config, player: ServerPlayerEntity) {
        for (listener in onUpdateServerListeners) {
            listener.onChanged(id, config, player)
        }
    }

    internal fun fireOnUpdateServer(id: Identifier, config: Config, context: ServerUpdateContext) {
        val player = context.getPlayer()
        if (player != null) {
            fireOnUpdateServer(id, config, player)
        }
        for (listener in onUpdateServerListenersNew) {
            listener.onChanged(id, config, context)
        }
    }

    internal fun fireOnRegisteredClient(id: Identifier, config: Config) {
        onRegisteredClientListeners[id]?.forEach {
            it.onRegistered(config)
        }
    }

    internal fun fireOnRegisteredServer(id: Identifier, config: Config) {
        onRegisteredServerListeners[id]?.forEach {
            it.onRegistered(config)
        }
    }
}