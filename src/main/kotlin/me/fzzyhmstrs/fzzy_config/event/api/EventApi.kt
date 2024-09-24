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

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier

/**
 * API for multiloader abstraction of simple play-phase networking
 * @author fzzyhmstrs
 * @since 0.4.1
 */
interface EventApi {

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
