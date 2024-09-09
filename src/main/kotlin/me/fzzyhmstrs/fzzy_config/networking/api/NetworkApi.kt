/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.networking.api

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier
import java.util.function.Function

/**
 * API for multiloader abstraction of simple play-phase networking
 * @author fzzyhmstrs
 * @since 0.4.1
 */
interface NetworkApi {

    fun buf(): PacketByteBuf

    /**
     * Checks whether a certain channel can be used for sending a packet. Works in both networking directions.
     *
     * This should be paired with [send] to make sure the transmission will be possible before sending.
     * @param id [Identifier] the channel id. This would normally be `CustomPayload.Id#id`
     * @param playerEntity [PlayerEntity], nullable - the player associated with this network transmission. For the server that will be the server player you are sending to, for the client it can be null or the client player.
     * @return whether the packet can be sent or not.
     * @author fzzyhmstrs
     * @since 0.4.1
     */
    fun canSend(id: Identifier, playerEntity: PlayerEntity?): Boolean

    /**
     * Sends a payload to a receiver. If the server player is defined, will be an S2C transmission, otherwise C2S
     *
     * Check if you can send at all first with [canSend]
     * @param payload [CustomPayload] the payload to send
     * @param playerEntity [PlayerEntity], nullable - the server player if you are sending S2C, null or the client player for C2S
     * @author fzzyhmstrs
     * @since 0.4.1
     */
    fun send(payload: CustomPayload, playerEntity: PlayerEntity?)

    /**
     * registers a clientbound (S2C) payload type and receipt handler. This must be done on both logical sides (client and server). A common entrypoint is typically the best place for this.
     * @param T the payload type to register
     * @param id [CustomPayload.Id] the id of the custom payload
     * @param function [Function]&lt;[PacketByteBuf],[T]&gt; the function for building the payload from a given buf
     * @param handler [S2CPayloadHandler] a handler for dealing with receiving the payload. This handler will be on the client handling a payload received from the server. As such, take care with your client-only class references, pushing them to a method to reference, perhaps.
     * @author fzzyhmstrs
     * @since 0.4.1
     */
    fun <T: CustomPayload> registerS2C(id: Identifier, function: Function<PacketByteBuf, T>, handler: S2CPayloadHandler<T>)
    /**
     * registers a serverbound (C2S) payload type and receipt handler. This must be done on both logical sides (client and server). A common entrypoint is typically the best place for this.
     * @param T the payload type to register
     * @param id [CustomPayload.Id] the id of the custom payload
     * @param function [Function]&lt;[PacketByteBuf],[T]&gt; the function for building the payload from a given buf
     * @param handler [S2CPayloadHandler] a handler for dealing with receiving the payload. This handler will be on the server handling a payload sent from the client.
     * @author fzzyhmstrs
     * @since 0.4.1
     */
    fun <T: CustomPayload> registerC2S(id: Identifier, function: Function<PacketByteBuf, T>, handler: C2SPayloadHandler<T>)
}