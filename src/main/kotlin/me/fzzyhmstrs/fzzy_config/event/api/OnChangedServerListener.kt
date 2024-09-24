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
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier

/**
 * Listener for on-changed events on the client side. WIll be fired for any and all updated configs, allowing inspection of other configs. For use in your own configs, consider directly implmeenting [Config.onChangedClient][me.fzzyhmstrs.fzzy_config.config.Config.onChangedClient]
 *
 * Register with [EventApi.onChangedClient][me.fzzyhmstrs.fzzy_config.event.api.EventApi.onChangedClient]
 * @author fzzyhmstrs
 * @since 0.5.0
 */
@FunctionalInterface
fun interface OnChangedServerListener {

    /**
     * Called by the `onChangedClient` event when the client side of a config is changed.
     * @param id [Identifier] the registered id attached to the config instance.
     * @author fzzyhmstrs
     * @since 0.5.0
     */
    fun onChanged(id: Identifier, config: Config, player: ServerPlayerEntity)
}
