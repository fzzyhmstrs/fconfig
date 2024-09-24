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
import net.minecraft.util.Identifier

/**
 * Listener for on-sync events on the client side. WIll be fired for any and all updated configs, allowing inspection of other configs. For use in your own configs, consider directly implementing [Config.onSyncClient][me.fzzyhmstrs.fzzy_config.config.Config.onSyncClient]
 *
 * Register with [EventApi.onSyncClient][me.fzzyhmstrs.fzzy_config.event.api.EventApi.onSyncClient]
 * @author fzzyhmstrs
 * @since 0.5.0
 */
@FunctionalInterface
fun interface OnSyncClientListener {

    /**
     * Called by the `onSyncClient` event when the client side of a config is synced. NOTE: Only called if a restart prompt is not created (so if the game state will continue into game).
     * @param id [Identifier] the registered id attached to the config instance.
     * @param config [Config] the config instance. This should only be read, or changes only made to transient fields/methods. Making updates to settings here will NOT be captured by the syncronization system.
     * @author fzzyhmstrs
     * @since 0.5.0
     */
    fun onSync(id: Identifier, config: Config)
}
