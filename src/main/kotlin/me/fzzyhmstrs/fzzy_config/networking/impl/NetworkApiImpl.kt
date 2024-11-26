/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.networking.impl

import me.fzzyhmstrs.fzzy_config.networking.api.*
import me.fzzyhmstrs.fzzy_config.util.platform.impl.PlatformUtils
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier

internal object NetworkApiImpl: NetworkApi {

    override fun canSend(id: Identifier, playerEntity: PlayerEntity?): Boolean {
        return if (playerEntity is ServerPlayerEntity) {
            ServerPlayNetworking.canSend(playerEntity, id)
        } else {
            ClientPlayNetworking.canSend(id)
        }
    }

    override fun send(payload: CustomPayload, playerEntity: PlayerEntity?) {
        if (playerEntity is ServerPlayerEntity) {
            ServerPlayNetworking.send(playerEntity, payload)
        } else {
            ClientPlayNetworking.send(payload)
        }
    }

    override fun <T : CustomPayload> registerS2C(id: CustomPayload.Id<T>, codec: PacketCodec<in RegistryByteBuf, T>, handler: S2CPayloadHandler<T>) {
        PayloadTypeRegistry.playS2C().register(id, codec)
        if (PlatformUtils.isClient()) {
            ClientPlayNetworking.registerGlobalReceiver(id) { payload, context ->
                val newContext = ClientPlayNetworkContext(context)
                handler.handle(payload, newContext)
            }
        }
    }

    override fun <T : CustomPayload> registerC2S(id: CustomPayload.Id<T>, codec: PacketCodec<in RegistryByteBuf, T>, handler: C2SPayloadHandler<T>) {
        PayloadTypeRegistry.playC2S().register(id, codec)
        ServerPlayNetworking.registerGlobalReceiver(id)  { payload, context ->
            val newContext = ServerPlayNetworkContext(context)
            handler.handle(payload, newContext)
        }
    }

}
