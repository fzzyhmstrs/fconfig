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
import me.fzzyhmstrs.fzzy_config.networking.api.*
import me.fzzyhmstrs.fzzy_config.util.PlatformUtils
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.packet.CustomPayload
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier
import net.neoforged.neoforge.network.PacketDistributor
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent
import net.neoforged.neoforge.network.registration.IPayloadRegistrar
import net.neoforged.neoforge.network.registration.NetworkRegistry
import java.util.function.Function

internal object NetworkApiImpl: NetworkApi {

    override fun canSend(id: Identifier, playerEntity: PlayerEntity?): Boolean {
        return if (playerEntity is ServerPlayerEntity) {
            NetworkRegistry.getInstance().isConnected(playerEntity.networkHandler, id)
        } else {
            NetworkEventsClient.canSend(id)
        }
    }

    override fun send(payload: CustomPayload, playerEntity: PlayerEntity?) {
        if (playerEntity is ServerPlayerEntity) {
            PacketDistributor.PLAYER.with(playerEntity).send(payload)
        } else {
            PacketDistributor.SERVER.noArg().send(payload)
        }
    }

    private val registeredS2CPayloads: MutableList<S2CRegistration<*>> = mutableListOf()

    override fun <T : CustomPayload> registerS2C(id: Identifier, function: Function<PacketByteBuf, T>, handler: S2CPayloadHandler<T>) {
        registeredS2CPayloads.add(S2CRegistration(id, function, handler))
    }

    private val registeredC2SPayloads: MutableList<C2SRegistration<*>> = mutableListOf()

    override fun <T : CustomPayload> registerC2S(id: Identifier, function: Function<PacketByteBuf, T>, handler: C2SPayloadHandler<T>) {
        registeredC2SPayloads.add(C2SRegistration(id, function, handler))
    }

    internal fun onRegister(event: RegisterPayloadHandlerEvent) {
        val registrarMap: MutableMap<String, IPayloadRegistrar> = mutableMapOf()
        for (registration in registeredS2CPayloads) {
            val registrar = registrarMap.computeIfAbsent(registration.id.namespace) { str -> event.registrar(str).optional() }
            registration.apply(registrar)
        }
        for (registration in registeredC2SPayloads) {
            val registrar = registrarMap.computeIfAbsent(registration.id.namespace) { str -> event.registrar(str).optional() }
            registration.apply(registrar)
        }
    }

    private data class S2CRegistration<T : CustomPayload>(val id: Identifier, val function: Function<PacketByteBuf, T>, val handler: S2CPayloadHandler<T>) {
        fun apply(registrar: IPayloadRegistrar) {
            registrar.play(id, { b -> function.apply(b) }) { payload, context ->
                if (PlatformUtils.isClient()) {
                    val newContext = ClientPlayNetworkContext(context)
                    handler.handle(payload, newContext)
                }
            }
        }
    }

    private data class C2SRegistration<T : CustomPayload>(val id: Identifier, val function: Function<PacketByteBuf, T>, val handler: C2SPayloadHandler<T>) {
        fun apply(registrar: IPayloadRegistrar) {
            registrar.play(id, { b -> function.apply(b) }) { payload, context ->
                val newContext = ServerPlayNetworkContext(context)
                handler.handle(payload, newContext)
            }
        }
    }


}