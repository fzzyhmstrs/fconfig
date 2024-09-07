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

import me.fzzyhmstrs.fzzy_config.cast
import me.fzzyhmstrs.fzzy_config.registry.SyncedConfigRegistry
import net.minecraft.network.packet.CustomPayload
import net.minecraft.network.packet.Packet
import net.minecraft.server.network.ServerPlayerConfigurationTask
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier
import net.neoforged.neoforge.event.OnDatapackSyncEvent
import net.neoforged.neoforge.network.PacketDistributor
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import net.neoforged.neoforge.network.handling.IPayloadContext
import net.neoforged.neoforge.network.registration.NetworkRegistry
import java.util.function.Consumer


internal object NetworkEvents {

    fun canSend(playerEntity: ServerPlayerEntity, id: CustomPayload.Id<*>): Boolean {
        return NetworkRegistry.hasChannel(playerEntity.networkHandler, id.id())
    }

    fun send(playerEntity: ServerPlayerEntity, payload: CustomPayload) {
        PacketDistributor.sendToPlayer(playerEntity, payload)
    }

    private fun handleUpdate(payload: ConfigUpdateC2SCustomPayload, context: IPayloadContext) {
        SyncedConfigRegistry.receiveConfigUpdate(
            payload.updates,
            context.player().cast<ServerPlayerEntity>().server,
            context.player().cast(),
            payload.changeHistory,
            { player, id -> canSend(player, id) },
            { player, pl -> send(player, pl) }
        )

    }

    private fun handleSettingForwardBidirectional(payload: SettingForwardCustomPayload, context: IPayloadContext) {
        if (context.flow().isServerbound) {
            this.handleSettingForward(payload, context)
        } else {
            NetworkEventsClient.handleSettingForward(payload, context)
        }
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
        } else {
            SyncedConfigRegistry.onJoin(
                serverPlayer,
                serverPlayer.server,
                { player, id -> this.canSend(player, id) },
                { player, payload -> this.send(player, payload) }
            )
        }
    }

    fun registerConfigurations(event: RegisterConfigurationTasksEvent) {
        event.register(object: ServerPlayerConfigurationTask {
            override fun sendPacket(sender: Consumer<Packet<*>>) {
                SyncedConfigRegistry.onConfigure(
                    { _ -> true },
                    { payload -> sender.accept(payload.toVanillaClientbound()) }
                )
            }

            override fun getKey(): ServerPlayerConfigurationTask.Key {
                return ServerPlayerConfigurationTask.Key(ConfigSyncS2CCustomPayload.type.id)
            }

        })
    }

    fun registerPayloads(event: RegisterPayloadHandlersEvent) {
        val registrar = event.registrar("fzzy_config").optional()

        registrar.configurationToClient(ConfigSyncS2CCustomPayload.type, ConfigSyncS2CCustomPayload.codec, NetworkEventsClient::handleConfigurationConfigSync)

        registrar.playToClient(ConfigSyncS2CCustomPayload.type, ConfigSyncS2CCustomPayload.codec, NetworkEventsClient::handleReloadConfigSync)

        registrar.playToClient(ConfigPermissionsS2CCustomPayload.type, ConfigPermissionsS2CCustomPayload.codec, NetworkEventsClient::handlePermsUpdate)

        registrar.playToClient(ConfigUpdateS2CCustomPayload.type, ConfigUpdateS2CCustomPayload.codec, NetworkEventsClient::handleUpdate)

        registrar.playToClient(ConfigUpdateC2SCustomPayload.type, ConfigUpdateC2SCustomPayload.codec, this::handleUpdate)

        registrar.playBidirectional(SettingForwardCustomPayload.type, SettingForwardCustomPayload.codec, this::handleSettingForwardBidirectional)

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