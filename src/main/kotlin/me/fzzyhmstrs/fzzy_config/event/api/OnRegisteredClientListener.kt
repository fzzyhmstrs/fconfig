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

import me.fzzyhmstrs.fzzy_config.api.RegisterType
import me.fzzyhmstrs.fzzy_config.config.Config

/**
 * Listener for on-registered events on the client side. Will be fired for all configs registered on the client, including your own.
 *
 * Register with [EventApi.onRegisteredClient][me.fzzyhmstrs.fzzy_config.event.api.EventApi.onRegisteredClient]
 * @author fzzyhmstrs
 * @since 0.5.9
 */
@FunctionalInterface
fun interface OnRegisteredClientListener {

    /**
     * Called by the `onRegisteredClient` event when the client side of a config is synced.
     *
     * If the config in question is registered with [RegisterType.SERVER] only, this will *not* be called.
     * @param config [Config] the config instance. This should only be read, or changes only made to transient fields/methods. Making updates to settings here will NOT be captured by the synchronization system.
     * @author fzzyhmstrs
     * @since 0.5.9
     */
    fun onRegistered(config: Config)
}