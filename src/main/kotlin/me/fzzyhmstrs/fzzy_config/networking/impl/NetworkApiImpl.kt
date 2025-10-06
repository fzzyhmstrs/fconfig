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

import me.fzzyhmstrs.fzzy_config.networking.FzzyPayload
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.networking.api.*
import me.fzzyhmstrs.fzzy_config.registry.SyncedConfigRegistry
import me.fzzyhmstrs.fzzy_config.util.platform.impl.PlatformUtils
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.PacketByteBuf
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier
import java.util.function.Function

internal object NetworkApiImpl: NetworkApi {

    override fun buf(): PacketByteBuf {
        return PacketByteBufs.create()
    }

    override fun canSend(id: Identifier, playerEntity: PlayerEntity?): Boolean {
        return if (playerEntity is ServerPlayerEntity) {
            ServerPlayNetworking.canSend(playerEntity, id)
        } else {
            ClientPlayNetworking.canSend(id)
        }
    }

    override fun send(payload: FzzyPayload, playerEntity: PlayerEntity?) {
        if (playerEntity is ServerPlayerEntity) {
            val buf = buf()
            payload.write(buf)
            ServerPlayNetworking.send(playerEntity, payload.getId(), buf)
        } else {
            val buf = buf()
            payload.write(buf)
            ClientPlayNetworking.send(payload.getId(), buf)
        }
    }

    override fun <T : FzzyPayload> registerS2C(id: Identifier, clazz: Class<T>, function: Function<PacketByteBuf, T>, handler: S2CPayloadHandler<T>) {
        if (PlatformUtils.isClient()) {
            ClientPlayNetworking.registerGlobalReceiver(id) { c, h, b, ps ->
                val newContext = ClientPlayNetworkContext(c, h, ps)
                handler.handle(function.apply(b), newContext)
            }
        }
    }

    override fun <T : FzzyPayload> registerC2S(id: Identifier, clazz: Class<T>, function: Function<PacketByteBuf, T>, handler: C2SPayloadHandler<T>) {
        ServerPlayNetworking.registerGlobalReceiver(id)  { _, p, h, b, ps ->
            val newContext = ServerPlayNetworkContext(p, h, ps)
            handler.handle(function.apply(b), newContext)
        }
    }

    override fun <T: FzzyPayload> registerLenientS2C(id: Identifier, clazz: Class<T>, function: Function<PacketByteBuf, T>, handler: S2CPayloadHandler<T>) {
        registerS2C(id, clazz, function, handler)
    }

    override fun <T : FzzyPayload> registerLenientC2S(id: Identifier, clazz: Class<T>, function: Function<PacketByteBuf, T>, handler: C2SPayloadHandler<T>) {
        registerC2S(id, clazz, function, handler)
    }

    override fun syncConfig(config: Config, server: MinecraftServer) {
        SyncedConfigRegistry.manualSync(config, server)
    }

}