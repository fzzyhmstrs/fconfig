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

import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.networking.api.ServerPlayNetworkContext
import me.fzzyhmstrs.fzzy_config.registry.SyncedConfigRegistry
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.networking.v1.*
import net.minecraft.network.packet.CustomPayload
import net.minecraft.server.network.ServerPlayerEntity


internal object NetworkEvents {

    fun canSend(playerEntity: ServerPlayerEntity, id: CustomPayload.Id<*>): Boolean {
        return ServerPlayNetworking.canSend(playerEntity, id)
    }

    fun send(playerEntity: ServerPlayerEntity, payload: CustomPayload) {
        ServerPlayNetworking.send(playerEntity, payload)
    }

    private fun receiveUpdate(payload: ConfigUpdateC2SCustomPayload, context: ServerPlayNetworkContext) {
        SyncedConfigRegistry.receiveConfigUpdate(
            payload.updates,
            context.player().server,
            context.player(),
            payload.playerPerm,
            payload.changeHistory,
            { _, id -> context.canReply(id.id) },
            { _, pl -> context.reply(pl) }
        )
    }

    private fun receiveForward(payload: SettingForwardCustomPayload, context: ServerPlayNetworkContext) {
        SyncedConfigRegistry.receiveSettingForward(
            payload.player,
            context.player(),
            payload.scope,
            payload.update,
            payload.summary,
            { player, id -> ServerPlayNetworking.canSend(player, id) },
            { player, pl -> ServerPlayNetworking.send(player, pl) }
        )
    }

    fun registerServer() {

        //PayloadTypeRegistry.configurationC2S().register(ConfigSyncS2CCustomPayload.type, ConfigSyncS2CCustomPayload.codec)
        PayloadTypeRegistry.configurationS2C().register(ConfigSyncS2CCustomPayload.type, ConfigSyncS2CCustomPayload.codec)
        //PayloadTypeRegistry.playS2C().register(ConfigPermissionsS2CCustomPayload.type, ConfigPermissionsS2CCustomPayload.codec)
        ConfigApi.network().registerLenientS2C(ConfigPermissionsS2CCustomPayload.type, ConfigPermissionsS2CCustomPayload.codec, NetworkEventsClient::receivePerms)
        //PayloadTypeRegistry.playC2S().register(ConfigSyncS2CCustomPayload.type, ConfigSyncS2CCustomPayload.codec)
        //PayloadTypeRegistry.playS2C().register(ConfigSyncS2CCustomPayload.type, ConfigSyncS2CCustomPayload.codec)
        ConfigApi.network().registerLenientS2C(ConfigSyncS2CCustomPayload.type, ConfigSyncS2CCustomPayload.codec, NetworkEventsClient::receiveSync)
        //PayloadTypeRegistry.playC2S().register(ConfigUpdateS2CCustomPayload.type, ConfigUpdateS2CCustomPayload.codec)
        //PayloadTypeRegistry.playS2C().register(ConfigUpdateS2CCustomPayload.type, ConfigUpdateS2CCustomPayload.codec)
        ConfigApi.network().registerLenientS2C(ConfigUpdateS2CCustomPayload.type, ConfigUpdateS2CCustomPayload.codec, NetworkEventsClient::receiveUpdate)
        //PayloadTypeRegistry.playC2S().register(ConfigUpdateC2SCustomPayload.type, ConfigUpdateC2SCustomPayload.codec)
        ConfigApi.network().registerLenientC2S(ConfigUpdateC2SCustomPayload.type, ConfigUpdateC2SCustomPayload.codec, this::receiveUpdate)
        //PayloadTypeRegistry.playS2C().register(ConfigUpdateC2SCustomPayload.type, ConfigUpdateC2SCustomPayload.codec)
        //PayloadTypeRegistry.playC2S().register(SettingForwardCustomPayload.type, SettingForwardCustomPayload.codec)
        ConfigApi.network().registerLenientC2S(SettingForwardCustomPayload.type, SettingForwardCustomPayload.codec, this::receiveForward)
        //PayloadTypeRegistry.playS2C().register(SettingForwardCustomPayload.type, SettingForwardCustomPayload.codec)
        ConfigApi.network().registerLenientS2C(SettingForwardCustomPayload.type, SettingForwardCustomPayload.codec, NetworkEventsClient::receiveForward)

        ConfigApi.network().registerLenientS2C(DynamicIdsS2CCustomPayload.type, DynamicIdsS2CCustomPayload.codec, NetworkEventsClient::receiveDynamicIds)


        ServerConfigurationConnectionEvents.CONFIGURE.register { handler, _ ->
            SyncedConfigRegistry.onConfigure(
                { id -> ServerConfigurationNetworking.canSend(handler, id) },
                { payload -> ServerConfigurationNetworking.send(handler, payload) }
            )
        }

        ServerPlayConnectionEvents.JOIN.register { handler, sender, server ->
            SyncedConfigRegistry.onJoin(
                handler.player,
                server,
                { player, id -> ConfigApi.network().canSend(id.id, player) },
                { player, payload -> ConfigApi.network().send(payload, player) }
            )
        }

        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register { server, _, _ ->
            SyncedConfigRegistry.onEndDataReload(
                server.playerManager.playerList,
                { player, id -> ConfigApi.network().canSend(id.id, player) },
                { player, payload -> ConfigApi.network().send(payload, player) }
            )
            ConfigApiImpl.invalidateLookup()
        }

        /*ServerPlayNetworking.registerGlobalReceiver(ConfigUpdateC2SCustomPayload.type){ payload, context ->
            SyncedConfigRegistry.receiveConfigUpdate(
                payload.updates,
                context.player().server,
                context.player(),
                payload.changeHistory,
                { player, id -> ServerPlayNetworking.canSend(player, id) },
                { player, pl -> ServerPlayNetworking.send(player, pl) }
            )
        }*/

        /*ServerPlayNetworking.registerGlobalReceiver(SettingForwardCustomPayload.type){ payload, context ->

        }*/
    }
}