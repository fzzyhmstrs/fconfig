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
import me.fzzyhmstrs.fzzy_config.networking.NetworkEventsClient
import me.fzzyhmstrs.fzzy_config.networking.api.*
import me.fzzyhmstrs.fzzy_config.util.platform.impl.PlatformUtils
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.PacketByteBuf
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier
import net.minecraftforge.network.NetworkRegistry
import net.minecraftforge.network.PacketDistributor
import net.minecraftforge.network.simple.SimpleChannel
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Function

internal object NetworkApiImpl: NetworkApi {

    override fun canSend(id: Identifier, playerEntity: PlayerEntity?): Boolean {
        return if (playerEntity is ServerPlayerEntity) {
            playerEntity.networkHandler.isConnectionOpen
        } else {
            NetworkEventsClient.canSend(id)
        }
    }

    override fun send(payload: FzzyPayload, playerEntity: PlayerEntity?) {
        if (playerEntity is ServerPlayerEntity) {
            channelMap[payload.getId()]?.send(PacketDistributor.PLAYER.with { playerEntity }, payload)
        } else {
            channelMap[payload.getId()]?.send(PacketDistributor.SERVER.noArg(), payload)
        }
    }

    private val channelMap: MutableMap<Identifier, SimpleChannel> = mutableMapOf()

    private val indexMap: MutableMap<Identifier, AtomicInteger> = mutableMapOf()

    override fun <T : FzzyPayload> registerS2C(id: Identifier, clazz: Class<T>, function: Function<PacketByteBuf, T>, handler: S2CPayloadHandler<T>) {
        val version = "1.0"
        val index = indexMap.computeIfAbsent(id) { _ -> AtomicInteger(0) }
        val channel = channelMap.computeIfAbsent(id) { i -> NetworkRegistry.newSimpleChannel(i, { version }, { serverVersion -> serverVersion == version}, { clientVersion -> clientVersion == version }) }
        @Suppress("INACCESSIBLE_TYPE")
        channel.registerMessage(
            index.incrementAndGet(),
            clazz,
            { payload: T, buf: PacketByteBuf -> payload.write(buf) },
            function,
            { payload, contextSuppler ->
                if (PlatformUtils.isClient()) {
                    val newContext = ClientPlayNetworkContext(contextSuppler.get())
                    handler.handle(payload, newContext)
                    contextSuppler.get().packetHandled = true
                }
            })
    }

    override fun <T : FzzyPayload> registerC2S(id: Identifier, clazz: Class<T>, function: Function<PacketByteBuf, T>, handler: C2SPayloadHandler<T>) {
        val version = "1.0"
        val index = indexMap.computeIfAbsent(id) { _ -> AtomicInteger(0) }
        val channel = channelMap.computeIfAbsent(id) { i -> NetworkRegistry.newSimpleChannel(i, { version }, { serverVersion -> serverVersion == version}, { clientVersion -> clientVersion == version }) }
        @Suppress("INACCESSIBLE_TYPE")
        channel.registerMessage(
            index.incrementAndGet(),
            clazz,
            { payload: T, buf: PacketByteBuf -> payload.write(buf) },
            function,
            { payload, contextSuppler ->
                val newContext = ServerPlayNetworkContext(contextSuppler.get())
                handler.handle(payload, newContext)
                contextSuppler.get().packetHandled = true
            })
    }

}