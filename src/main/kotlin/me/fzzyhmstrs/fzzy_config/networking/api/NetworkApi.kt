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

import me.fzzyhmstrs.fzzy_config.config.Config
import net.minecraft.world.entity.player.Player
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.MinecraftServer
import net.minecraft.resources.Identifier

/**
 * API for multiloader abstraction of simple play-phase networking
 * @author fzzyhmstrs
 * @since 0.4.1
 */
interface NetworkApi {

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
    fun canSend(id: Identifier, playerEntity: Player?): Boolean

    /**
     * Sends a payload to a receiver. If the server player is defined, will be an S2C transmission, otherwise C2S
     *
     * Check if you can send at all first with [canSend]
     * @param payload [CustomPayload] the payload to send
     * @param playerEntity [PlayerEntity], nullable - the server player if you are sending S2C, null or the client player for C2S
     * @author fzzyhmstrs
     * @since 0.4.1
     */
    fun send(payload: CustomPacketPayload, playerEntity: Player?)

    /**
     * registers a client-bound (S2C) payload type and receipt handler. This must be done on both logical sides (client and server). A common entrypoint is typically the best place for this.
     * @param T the payload type to register
     * @param id [CustomPayload.Id] the id of the custom payload
     * @param codec [PacketCodec] the packet codec for serializing the custom payload
     * @param handler [S2CPayloadHandler] a handler for dealing with receiving the payload. This handler will be on the client handling a payload received from the server. As such, take care with your client-only class references, pushing them to a method to reference, perhaps.
     * @author fzzyhmstrs
     * @since 0.4.1
     */
    fun <T: CustomPacketPayload> registerS2C(id: CustomPacketPayload.Type<T>, codec: StreamCodec<in RegistryFriendlyByteBuf, T>, handler: S2CPayloadHandler<T>)
    /**
     * registers a server-bound (C2S) payload type and receipt handler. This must be done on both logical sides (client and server). A common entrypoint is typically the best place for this.
     * @param T the payload type to register
     * @param id [CustomPayload.Id] the id of the custom payload
     * @param codec [PacketCodec] the packet codec for serializing the custom payload
     * @param handler [S2CPayloadHandler] a handler for dealing with receiving the payload. This handler will be on the server handling a payload sent from the client.
     * @author fzzyhmstrs
     * @since 0.4.1
     */
    fun <T: CustomPacketPayload> registerC2S(id: CustomPacketPayload.Type<T>, codec: StreamCodec<in RegistryFriendlyByteBuf, T>, handler: C2SPayloadHandler<T>)
    /**
     * registers a client-bound (S2C) payload type and receipt handler. This does not have to be done on both server and client (hence lenient). This allows for clients without Fzzy Config to connect to a server with mods that register connections this way. A common entrypoint is typically the best place for registering this.     * @param T the payload type to register
     * @param id [CustomPayload.Id] the id of the custom payload
     * @param codec [PacketCodec] the packet codec for serializing the custom payload
     * @param handler [S2CPayloadHandler] a handler for dealing with receiving the payload. This handler will be on the client handling a payload received from the server. As such, take care with your client-only class references, pushing them to a method to reference, perhaps.
     * @author fzzyhmstrs
     * @since 0.6.6, 0.6.5-fix1 for Forge 1.20.1
     */
    fun <T: CustomPacketPayload> registerLenientS2C(id: CustomPacketPayload.Type<T>, codec: StreamCodec<in RegistryFriendlyByteBuf, T>, handler: S2CPayloadHandler<T>)
    /**
     * registers a server-bound (C2S) payload type and receipt handler. This does not have to be done on both server and client (hence lenient). This allows for clients without Fzzy Config to connect to a server with mods that register connections this way. A common entrypoint is typically the best place for this.     * @param T the payload type to register
     * @param id [CustomPayload.Id] the id of the custom payload
     * @param codec [PacketCodec] the packet codec for serializing the custom payload
     * @param handler [S2CPayloadHandler] a handler for dealing with receiving the payload. This handler will be on the server handling a payload sent from the client.
     * @author fzzyhmstrs
     * @since 0.6.6, 0.6.5-fix1 for Forge 1.20.1
     */
    fun <T: CustomPacketPayload> registerLenientC2S(id: CustomPacketPayload.Type<T>, codec: StreamCodec<in RegistryFriendlyByteBuf, T>, handler: C2SPayloadHandler<T>)

    /**
     * Manually sync a non-client config (which doesn't need syncing).
     *
     * Configs synced this way should probably also use [Config.save]
     * @param config [Config] the config that is going to be synced
     * @param server [MinecraftServer] the server instance for sending the update to all connected players.
     * @author fzzyhmstrs
     * @since 0.7.3
     */
    fun syncConfig(config: Config, server: MinecraftServer)
}