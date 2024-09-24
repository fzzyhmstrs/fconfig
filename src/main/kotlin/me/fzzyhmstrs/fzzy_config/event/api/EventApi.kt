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

/**
 * API for registration of config events
 * @author fzzyhmstrs
 * @since 0.5.0
 */
interface EventApi {

    /**
     * Registers a listener to the global `onSyncClient` event. This will be fired on the logical client when a client side config is synced to the client.
     *
     * This occurs when the player logs in or datapacks are reloaded.
     *
     * This should only perform client logic.
     * @param listener [OnSyncClientListener] callback that is fired when any config is synced on the client side. This can be used to inspect other configs, not just your own.
     * @see [me.fzzyhmstrs.fzzy_config.config.Config.onSyncClient] A direct-implementation option for inspecting your own config on sync.
     * @author fzzyhmstrs
     * @since 0.5.0
     */
    fun onSyncClient(listener: OnSyncClientListener)
    
    /**
     * Registers a listener to the global `onChangedClient` event. This will be fired on the logical client when a client side config is updated in-game.
     *
     * Typically this is when the user closes the config screen, but also occurs after a connected client recieves a S2C update.
     *
     * This should only perform client logic, and anything referencing client-only classes needs to go here.
     * @param listener [OnChangedClientListener] callback that is fired when any config is updated on the client side. This can be used to inspect other configs, not just your own.
     * @see [me.fzzyhmstrs.fzzy_config.config.Config.onChangedClient] A direct-implementation option for inspecting your own config on change.
     * @author fzzyhmstrs
     * @since 0.5.0
     */
    fun onChangedClient(listener: OnChangedClientListener)

    /**
     * Registers a listener to the global `onChangedServer` event. This will be fired on the logical server after an updated config is prepared for saving.
     *
     * Typically this will be after a config update is received from a connected client, and that update passes permission checks.
     * @param listener [OnChangedServerListener] callback that is fired when any config is updated on the server side. This can be used to inspect other configs, not just your own.
     * @see [me.fzzyhmstrs.fzzy_config.config.Config.onChangedServer] A direct-implementation option for inspecting your own config on change.
     * @author fzzyhmstrs
     * @since 0.5.0
     */
    fun onChangedServer(listener: OnChangedServerListener)
}
