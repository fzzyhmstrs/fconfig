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
 * Listener for on-changed events on the client side. Will be fired for any and all updated configs, allowing inspection of other configs. For use in your own configs, consider directly implementing [Config.onUpdateClient][me.fzzyhmstrs.fzzy_config.config.Config.onUpdateClient]
 *
 * Register with [EventApi.onUpdateClient][me.fzzyhmstrs.fzzy_config.event.api.EventApi.onUpdateClient]
 * @author fzzyhmstrs
 * @since 0.5.0
 */
@FunctionalInterface
fun interface OnUpdateClientListener {

    /**
     * Called by the `onChangedClient` event when the client side of a config is changed.
     * @param id [Identifier] the registered id attached to the config instance.
     * @param config [Config] the config instance. This should only be read, or changes only made to transient fields/methods. Making updates to settings here will NOT be captured by the synchronization system.
     * @author fzzyhmstrs
     * @since 0.5.0
     */
    fun onChanged(id: Identifier, config: Config)
}