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

import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.NetworkPhase
import net.minecraft.network.NetworkSide
import net.minecraft.network.packet.CustomPayload
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Identifier

/**
 * A server-side network context, used to handle C2S payloads
 * @author fzzyhmstrs
 * @since 0.4.1
 */
class ServerPlayNetworkContext(private val context: ServerPlayNetworking.Context) : NetworkContext<ServerPlayerEntity> {

    /**
     * Executes a task on the main thread. This should be used for anything interacting with game state outside the network loop
     * @param runnable the Runnable to execute on the main thread
     * @author fzzyhmstrs
     * @since 0.4.1
     */
    override fun execute(runnable: Runnable) {
        server().execute(runnable)
    }

    /**
     * Disconnects the current session (single or multiplayer)
     * @param reason Why you are firing the disconnect
     * @author fzzyhmstrs
     * @since 0.4.1
     */
    override fun disconnect(reason: Text) {
        context.responseSender().disconnect(reason)
    }

    /**
     * Check for whether you can reply with a certain payload type (Typically this is `CustomPayload.Id#id`)
     * @param id [Identifier] the payload identifier to check
     * @return whether your response can be sent on the specified channel
     * @author fzzyhmstrs
     * @since 0.4.1
     */
    override fun canReply(id: Identifier): Boolean {
        return ServerPlayNetworking.canSend(player(), id)
    }

    /**
     * Replies to a payload with an opposite-direction response (S2C in this case)
     * @param payload the payload to respond with
     * @author fzzyhmstrs
     * @since 0.4.1
     */
    override fun reply(payload: CustomPayload) {
        context.responseSender().sendPacket(payload)
    }

    /**
     * Sends a payload to all players on the server. By default, skips the player that sent the inbound packet
     * @param payload [CustomPayload] the payload to send to the player list
     * @param skipCurrentPlayer Boolean, default true - whether to skip the player that sent the inbound payload.
     * @author fzzyhmstrs
     * @since 0.4.1
     */
    @JvmOverloads
    fun sendToAllPlayers(payload: CustomPayload, skipCurrentPlayer: Boolean = true) {
        for (player in server().playerManager.playerList) {
            if (skipCurrentPlayer && player == player()) continue
            ConfigApi.network().send(payload, player)
        }
    }

    /**
     * The player entity associated with this context. A server player in this case.
     * @return [ServerPlayerEntity]
     * @author fzzyhmstrs
     * @since 0.4.1
     */
    override fun player(): ServerPlayerEntity {
        return context.player()
    }

    /**
     * The server associated with this context.
     * @return [MinecraftServer]
     * @author fzzyhmstrs
     * @since 0.7.0
     */
    fun server(): MinecraftServer {
        return context.server()
    }

    /**
     * The current network phase. Always PLAY at the moment.
     * @author fzzyhmstrs
     * @since 0.4.1
     */
    override fun networkPhase(): NetworkPhase {
        return NetworkPhase.PLAY
    }

    /**
     * The network side of this context. Responses will be CLIENTBOUND
     * @author fzzyhmstrs
     * @since 0.4.1
     */
    override fun networkSide(): NetworkSide {
        return NetworkSide.CLIENTBOUND
    }
}