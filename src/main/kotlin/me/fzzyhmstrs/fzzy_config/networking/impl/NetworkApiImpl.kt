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

import me.fzzyhmstrs.fzzy_config.networking.NetworkEventsClient
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.networking.api.*
import me.fzzyhmstrs.fzzy_config.registry.SyncedConfigRegistry
import me.fzzyhmstrs.fzzy_config.util.platform.impl.PlatformUtils
import net.minecraft.world.entity.player.Player
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.resources.Identifier
import net.neoforged.neoforge.client.network.ClientPacketDistributor
import net.neoforged.neoforge.network.PacketDistributor
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import net.neoforged.neoforge.network.registration.NetworkRegistry
import net.neoforged.neoforge.network.registration.PayloadRegistrar

internal object NetworkApiImpl: NetworkApi {

    override fun canSend(id: Identifier, playerEntity: Player?): Boolean {
        return if (playerEntity is ServerPlayer) {
            NetworkRegistry.hasChannel(playerEntity.connection, id)
        } else {
            NetworkEventsClient.canSend(id)
        }
    }

    override fun send(payload: CustomPacketPayload, playerEntity: Player?) {
        if (playerEntity is ServerPlayer) {
            PacketDistributor.sendToPlayer(playerEntity, payload)
        } else {
            ClientPacketDistributor.sendToServer(payload)
        }
    }

    private val registeredS2CPayloads: MutableList<S2CRegistration<*>> = mutableListOf()

    override fun <T : CustomPacketPayload> registerS2C(id: CustomPacketPayload.Type<T>, codec: StreamCodec<in RegistryFriendlyByteBuf, T>, handler: S2CPayloadHandler<T>) {
        registeredS2CPayloads.add(S2CRegistration(id, codec, handler))
    }

    private val registeredC2SPayloads: MutableList<C2SRegistration<*>> = mutableListOf()

    override fun <T : CustomPacketPayload> registerC2S(id: CustomPacketPayload.Type<T>, codec: StreamCodec<in RegistryFriendlyByteBuf, T>, handler: C2SPayloadHandler<T>) {
        registeredC2SPayloads.add(C2SRegistration(id, codec, handler))
    }

    internal fun onRegister(event: RegisterPayloadHandlersEvent) {
        val registrarMap: MutableMap<String, PayloadRegistrar> = mutableMapOf()
        for (registration in registeredS2CPayloads) {
            val registrar = registrarMap.computeIfAbsent(registration.id.id.namespace) { str -> event.registrar(str).optional() }
            registration.apply(registrar)
        }
        for (registration in registeredC2SPayloads) {
            val registrar = registrarMap.computeIfAbsent(registration.id.id.namespace) { str -> event.registrar(str).optional() }
            registration.apply(registrar)
        }
    }

    private data class S2CRegistration<T : CustomPacketPayload>(val id: CustomPacketPayload.Type<T>, val codec: StreamCodec<in RegistryFriendlyByteBuf, T>, val handler: S2CPayloadHandler<T>) {
        fun apply(registrar: PayloadRegistrar) {
            registrar.playToClient(id, codec) { payload, context ->
                if (PlatformUtils.isClient()) {
                    val newContext = ClientPlayNetworkContext(context)
                    handler.handle(payload, newContext)
                }
            }
        }
    }

    private data class C2SRegistration<T : CustomPacketPayload>(val id: CustomPacketPayload.Type<T>, val codec: StreamCodec<in RegistryFriendlyByteBuf, T>, val handler: C2SPayloadHandler<T>) {
        fun apply(registrar: PayloadRegistrar) {
            registrar.playToServer(id, codec) { payload, context ->
                val newContext = ServerPlayNetworkContext(context)
                handler.handle(payload, newContext)
            }
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