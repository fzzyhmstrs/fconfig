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

import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.networking.api.*
import me.fzzyhmstrs.fzzy_config.registry.SyncedConfigRegistry
import me.fzzyhmstrs.fzzy_config.util.platform.impl.PlatformUtils
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.world.entity.player.Player
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.resources.Identifier

internal object NetworkApiImpl: NetworkApi {

    override fun canSend(id: Identifier, playerEntity: Player?): Boolean {
        return if (playerEntity is ServerPlayer) {
            ServerPlayNetworking.canSend(playerEntity, id)
        } else {
            ClientPlayNetworking.canSend(id)
        }
    }

    override fun send(payload: CustomPacketPayload, playerEntity: Player?) {
        if (playerEntity is ServerPlayer) {
            ServerPlayNetworking.send(playerEntity, payload)
        } else {
            ClientPlayNetworking.send(payload)
        }
    }

    override fun <T : CustomPacketPayload> registerS2C(id: CustomPacketPayload.Type<T>, codec: StreamCodec<in RegistryFriendlyByteBuf, T>, handler: S2CPayloadHandler<T>) {
        PayloadTypeRegistry.clientboundPlay().register(id, codec)
        if (PlatformUtils.isClient()) {
            ClientPlayNetworking.registerGlobalReceiver(id) { payload, context ->
                val newContext = ClientPlayNetworkContext(context)
                handler.handle(payload, newContext)
            }
        }
    }

    override fun <T : CustomPacketPayload> registerC2S(id: CustomPacketPayload.Type<T>, codec: StreamCodec<in RegistryFriendlyByteBuf, T>, handler: C2SPayloadHandler<T>) {
        PayloadTypeRegistry.serverboundPlay().register(id, codec)
        ServerPlayNetworking.registerGlobalReceiver(id)  { payload, context ->
            val newContext = ServerPlayNetworkContext(context)
            handler.handle(payload, newContext)
        }
    }

    override fun <T: CustomPacketPayload> registerLenientS2C(id: CustomPacketPayload.Type<T>, codec: StreamCodec<in RegistryFriendlyByteBuf, T>, handler: S2CPayloadHandler<T>) {
        registerS2C(id, codec, handler)
    }

    override fun <T : CustomPacketPayload> registerLenientC2S(id: CustomPacketPayload.Type<T>, codec: StreamCodec<in RegistryFriendlyByteBuf, T>, handler: C2SPayloadHandler<T>) {
        registerC2S(id, codec, handler)
    }

    override fun syncConfig(config: Config, server: MinecraftServer) {
        SyncedConfigRegistry.manualSync(config, server)
    }

}