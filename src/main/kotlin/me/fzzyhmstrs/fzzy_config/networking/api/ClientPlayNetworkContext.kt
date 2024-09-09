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
import me.fzzyhmstrs.fzzy_config.networking.FzzyPayload
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayNetworkHandler
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.network.NetworkSide
import net.minecraft.text.Text
import net.minecraft.util.Identifier

/**
 * A client-side network context, used to handle S2C payloads
 * @author fzzyhmstrs
 * @since 0.4.1
 */
class ClientPlayNetworkContext(private val client: MinecraftClient, private val handler: ClientPlayNetworkHandler, private val sender: PacketSender): NetworkContext<ClientPlayerEntity> {

    /**
     * Executes a task on the main thread. This should be used for anything interacting with game state outside the network loop
     * @param runnable the Runnable to execute on the main thread
     * @author fzzyhmstrs
     * @since 0.4.1
     */
    override fun execute(runnable: Runnable) {
        MinecraftClient.getInstance().execute(runnable)
    }

    /**
     * Disconnects the current session (single or multiplayer)
     * @param reason Why you are firing the disconnect
     * @author fzzyhmstrs
     * @since 0.4.1
     */
    override fun disconnect(reason: Text) {
        client.world?.disconnect(); client.disconnect()
    }

    /**
     * Check for whether you can reply with a certain payload type (Typically this is `CustomPayload.Id#id`)
     * @param id [Identifier] the payload identifier to check
     * @return whether your response can be sent on the specified channel
     * @author fzzyhmstrs
     * @since 0.4.1
     */
    override fun canReply(id: Identifier): Boolean {
        return ClientPlayNetworking.canSend(id)
    }

    /**
     * Replies to a payload with an opposite-direction response (C2S in this case)
     * @param payload the payload to respond with
     * @author fzzyhmstrs
     * @since 0.4.1
     */
    override fun reply(payload: FzzyPayload) {
        val buf = ConfigApi.network().buf()
        payload.write(buf)
        sender.sendPacket(payload.getId(), buf)
    }

    /**
     * The player entity associated with this context. A client player in this case.
     * @return [ClientPlayerEntity]
     * @author fzzyhmstrs
     * @since 0.4.1
     */
    override fun player(): ClientPlayerEntity {
        return client.player!!
    }

    /**
     * The network side of this context. Responses will be SERVERBOUND
     * @author fzzyhmstrs
     * @since 0.4.1
     */
    override fun networkSide(): NetworkSide {
        return NetworkSide.SERVERBOUND
    }
}