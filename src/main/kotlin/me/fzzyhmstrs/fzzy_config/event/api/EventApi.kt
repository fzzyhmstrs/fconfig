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

import net.minecraft.util.Identifier

/**
 * API for registration of config events
 *
 * [See the Wiki](https://moddedmc.wiki/en/project/fzzy-config/docs/config-concepts/Events) for more details and examples.
 * @author fzzyhmstrs
 * @since 0.5.0
 */
interface EventApi {

    /**
     * Registers a listener to the global `onSyncClient` event. This will be fired on the logical client when a config is synced.
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
     * Registers a listener to the global `onSyncServer` event. This will be fired on the logical server when a config is about to be synced to clients.
     *
     * This occurs when the player logs in or datapacks are reloaded.
     * @param listener [OnSyncServerListener] callback that is fired when any config is synced on the server side. This can be used to inspect other configs, not just your own.
     * @see [me.fzzyhmstrs.fzzy_config.config.Config.onSyncServer] A direct-implementation option for inspecting your own config on sync.
     * @author fzzyhmstrs
     * @since 0.5.0
     */
    fun onSyncServer(listener: OnSyncServerListener)

    /**
     * Registers a listener to the global `onChangedClient` event. This will be fired on the logical client when a client side config is updated in-game.
     *
     * Typically, this is when the user closes the config screen, but also occurs after a connected client recieves a S2C update.
     *
     * This should only perform client logic, and anything referencing client-only classes needs to go here.
     * @param listener [OnUpdateClientListener] callback that is fired when any config is updated on the client side. This can be used to inspect other configs, not just your own.
     * @see [me.fzzyhmstrs.fzzy_config.config.Config.onUpdateClient] A direct-implementation option for inspecting your own config on change.
     * @author fzzyhmstrs
     * @since 0.5.0
     */
    fun onUpdateClient(listener: OnUpdateClientListener)

    /**
     * Registers a listener to the global `onChangedServer` event. This will be fired on the logical server after an updated config is prepared for saving.
     *
     * Typically, this will be after a config update is received from a connected client, and that update passes permission checks.
     * @param listener [OnUpdateServerListener] callback that is fired when any config is updated on the server side. This can be used to inspect other configs, not just your own.
     * @see [me.fzzyhmstrs.fzzy_config.config.Config.onUpdateServer] A direct-implementation option for inspecting your own config on change.
     * @author fzzyhmstrs
     * @since 0.5.0
     */
    fun onUpdateServer(listener: OnUpdateServerListener)

    /**
     * Registers a listener to the `onRegisteredClient` event.
     * - If the config has already been registered when this is called the listener will be fired right away
     * - Otherwise, the listener will be queued and fired after the config is registered
     *
     * Typically, this will happen sometime during mod initialization, but that isn't guaranteed. Fzzy Configs can be loaded lazily. If the config is never loaded, or not loaded on the client, this listener will never be fired.
     *
     * This should only perform client logic, and anything referencing client-only classes needs to go here.
     * @param configId [Identifier] the registry id of the config to listen for
     * @param listener [OnRegisteredClientListener] callback that is fired when any config is registered on the client side. This can be used to act on registration of other configs, not just your own.
     * @author fzzyhmstrs
     * @since 0.5.0
     */
    fun onRegisteredClient(configId: Identifier, listener: OnRegisteredClientListener)

    /**
     * Registers a listener to the global `onChangedServer` event. This will be fired on the logical server after an updated config is prepared for saving.
     * - If the config has already been registered when this is called the listener will be fired right away
     * - Otherwise, the listener will be queued and fired after the config is registered
     *
     * Typically, this will happen sometime during mod initialization, but that isn't guaranteed. Fzzy Configs can be loaded lazily. This is a "common" event despite the name (fired on client and server). If the config is never loaded, or not loaded as a synced config, this listener will never be fired.
     * @param configId [Identifier] the registry id of the config to listen for
     * @param listener [OnRegisteredServerListener] callback that is fired when any config is updated on the server side. This can be used to inspect other configs, not just your own.
     * @see [me.fzzyhmstrs.fzzy_config.config.Config.onUpdateServer] A direct-implementation option for inspecting your own config on change.
     * @author fzzyhmstrs
     * @since 0.5.0
     */
    fun onRegisteredServer(configId: Identifier, listener: OnRegisteredServerListener)
}