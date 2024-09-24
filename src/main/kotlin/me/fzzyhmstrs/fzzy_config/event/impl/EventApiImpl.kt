/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.event.api

import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.event.api.*
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier

internal object EventApiImpl: EventApi {

    private val onChangedClientListeners: MutableList<OnChangedClientListener> = mutableListOf()

    private val onChangedServerListeners: MutableList<OnChangedServerListener> = mutableListOf()
    
    override fun onChangedClient(listener: OnChangedClientListener) {
        onChangedClientListeners.add(listener)
    }

    override fun onChangedServer(listener: OnChangedServerListener) {
        onChangedServerListeners.add(listener)
    }

    internal fun fireOnChangedClient(id: Identifier, config: Config) {
        for (listener in onChangedClientListeners) {
            listener.onChanged(id, config)
        }
    }

    internal fun fireOnChangedServer(id: Identifier, config: Config, player: ServerPlayerEntity) {
        for (listener in onChangedServerListeners) {
            listener.onChanged(id, config, player)
        }
    }
}
