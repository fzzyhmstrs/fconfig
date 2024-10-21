/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.networking

import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.cast
import me.fzzyhmstrs.fzzy_config.registry.SyncedConfigRegistry
import net.minecraft.network.packet.CustomPayload
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket
import net.minecraft.server.network.ServerPlayerConfigurationTask
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier
import net.neoforged.neoforge.event.OnDatapackSyncEvent
import net.neoforged.neoforge.network.PacketDistributor
import net.neoforged.neoforge.network.configuration.ICustomConfigurationTask
import net.neoforged.neoforge.network.event.OnGameConfigurationEvent
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent
import net.neoforged.neoforge.network.handling.IPayloadContext
import net.neoforged.neoforge.network.handling.IPlayPayloadHandler
import net.neoforged.neoforge.network.registration.IDirectionAwarePayloadHandlerBuilder
import net.neoforged.neoforge.network.registration.NetworkRegistry
import java.util.function.Consumer


internal object NetworkEvents {

    fun canSend(playerEntity: ServerPlayerEntity, id: Identifier): Boolean {
        return NetworkRegistry.getInstance().isConnected(playerEntity.networkHandler, id)
    }

    fun send(playerEntity: ServerPlayerEntity, payload: CustomPayload) {
        PacketDistributor.PLAYER.with(playerEntity).send(payload)
    }

    private fun handleUpdate(payload: ConfigUpdateC2SCustomPayload, context: IPayloadContext) {
        if (context.player().isEmpty) return
        SyncedConfigRegistry.receiveConfigUpdate(
            payload.updates,
            context.player().get().cast<ServerPlayerEntity>().server,
            context.player().get().cast(),
            payload.changeHistory,
            { player, id -> canSend(player, id) },
            { player, pl -> send(player, pl) }
        )

    }

    private fun handleSettingForwardBidirectional(builder: IDirectionAwarePayloadHandlerBuilder<SettingForwardCustomPayload, IPlayPayloadHandler<SettingForwardCustomPayload>>) {
        builder.server(this::handleSettingForward).client(NetworkEventsClient::handleSettingForward)
    }

    private fun handleSettingForward(payload: SettingForwardCustomPayload, context: IPayloadContext) {
        SyncedConfigRegistry.receiveSettingForward(
            payload.player,
            context.player().cast(),
            payload.scope,
            payload.update,
            payload.summary,
            { player, id -> canSend(player, id) },
            { player, pl -> send(player, pl) }
        )
    }

    fun registerDataSync(event: OnDatapackSyncEvent) {
        val serverPlayer = event.player
        if (serverPlayer == null) {
            SyncedConfigRegistry.onEndDataReload(
                event.relevantPlayers.toList(),
                { player, id -> canSend(player, id) },
                { player, payload -> send(player, payload) }
            )
            ConfigApiImpl.invalidateLookup()
        } else {
            SyncedConfigRegistry.onJoin(
                serverPlayer,
                serverPlayer.server,
                { player, id -> this.canSend(player, id) },
                { player, payload -> this.send(player, payload) }
            )
        }
    }

    fun registerConfigurations(event: OnGameConfigurationEvent) {
        event.register(object: ICustomConfigurationTask {
            private val key = ServerPlayerConfigurationTask.Key(ConfigSyncS2CCustomPayload.id)
            override fun run(consumer: Consumer<CustomPayload>) {
                SyncedConfigRegistry.onConfigure(
                    { _ -> true },
                    { payload -> consumer.accept(payload) }
                )
                event.listener.onTaskFinished(key)
            }

            override fun getKey(): ServerPlayerConfigurationTask.Key {
                return key
            }

        })
    }

    fun registerPayloads(event: RegisterPayloadHandlerEvent) {
        val registrar = event.registrar("fzzy_config").optional()

        registrar.configuration(ConfigSyncS2CCustomPayload.id, ::ConfigSyncS2CCustomPayload, NetworkEventsClient::handleConfigurationConfigSync)

        registrar.play(ConfigSyncS2CCustomPayload.id, ::ConfigSyncS2CCustomPayload, NetworkEventsClient::handleReloadConfigSync)

        registrar.play(ConfigPermissionsS2CCustomPayload.id, ::ConfigPermissionsS2CCustomPayload, NetworkEventsClient::handlePermsUpdate)

        registrar.play(ConfigUpdateS2CCustomPayload.id, ::ConfigUpdateS2CCustomPayload, NetworkEventsClient::handleUpdate)

        registrar.play(ConfigUpdateC2SCustomPayload.id, ::ConfigUpdateC2SCustomPayload, this::handleUpdate)

        registrar.play(SettingForwardCustomPayload.id, ::SettingForwardCustomPayload, this::handleSettingForwardBidirectional)

        registrar.play(DynamicIdsS2CCustomPayload.type, ::DynamicIdsS2CCustomPayload, NetworkEventsClient::receiveDynamicIds)
        //PayloadTypeRegistry.configurationC2S().register(ConfigSyncS2CCustomPayload.type, ConfigSyncS2CCustomPayload.codec)
        //PayloadTypeRegistry.configurationS2C().register(ConfigSyncS2CCustomPayload.type, ConfigSyncS2CCustomPayload.codec)
        //PayloadTypeRegistry.playS2C().register(ConfigPermissionsS2CCustomPayload.type, ConfigPermissionsS2CCustomPayload.codec)
        //PayloadTypeRegistry.playC2S().register(ConfigSyncS2CCustomPayload.type, ConfigSyncS2CCustomPayload.codec)
        //PayloadTypeRegistry.playS2C().register(ConfigSyncS2CCustomPayload.type, ConfigSyncS2CCustomPayload.codec)
        //PayloadTypeRegistry.playC2S().register(ConfigUpdateS2CCustomPayload.type, ConfigUpdateS2CCustomPayload.codec)
        //PayloadTypeRegistry.playS2C().register(ConfigUpdateS2CCustomPayload.type, ConfigUpdateS2CCustomPayload.codec)
        //PayloadTypeRegistry.playC2S().register(ConfigUpdateC2SCustomPayload.type, ConfigUpdateC2SCustomPayload.codec)
        //PayloadTypeRegistry.playS2C().register(ConfigUpdateC2SCustomPayload.type, ConfigUpdateC2SCustomPayload.codec)
        //PayloadTypeRegistry.playC2S().register(SettingForwardCustomPayload.type, SettingForwardCustomPayload.codec)
        //PayloadTypeRegistry.playS2C().register(SettingForwardCustomPayload.type, SettingForwardCustomPayload.codec)
    }
}