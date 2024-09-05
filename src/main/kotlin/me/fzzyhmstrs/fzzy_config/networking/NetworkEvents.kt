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

    fun registerServer() {

        //PayloadTypeRegistry.configurationC2S().register(ConfigSyncS2CCustomPayload.type, ConfigSyncS2CCustomPayload.codec)
        PayloadTypeRegistry.configurationS2C().register(ConfigSyncS2CCustomPayload.type, ConfigSyncS2CCustomPayload.codec)
        PayloadTypeRegistry.playS2C().register(ConfigPermissionsS2CCustomPayload.type, ConfigPermissionsS2CCustomPayload.codec)
        //PayloadTypeRegistry.playC2S().register(ConfigSyncS2CCustomPayload.type, ConfigSyncS2CCustomPayload.codec)
        PayloadTypeRegistry.playS2C().register(ConfigSyncS2CCustomPayload.type, ConfigSyncS2CCustomPayload.codec)
        //PayloadTypeRegistry.playC2S().register(ConfigUpdateS2CCustomPayload.type, ConfigUpdateS2CCustomPayload.codec)
        PayloadTypeRegistry.playS2C().register(ConfigUpdateS2CCustomPayload.type, ConfigUpdateS2CCustomPayload.codec)
        PayloadTypeRegistry.playC2S().register(ConfigUpdateC2SCustomPayload.type, ConfigUpdateC2SCustomPayload.codec)
        //PayloadTypeRegistry.playS2C().register(ConfigUpdateC2SCustomPayload.type, ConfigUpdateC2SCustomPayload.codec)
        PayloadTypeRegistry.playC2S().register(SettingForwardCustomPayload.type, SettingForwardCustomPayload.codec)
        PayloadTypeRegistry.playS2C().register(SettingForwardCustomPayload.type, SettingForwardCustomPayload.codec)

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
                { player, id -> ServerPlayNetworking.canSend(player, id) },
                { _, payload -> sender.sendPacket(payload) }
            )
        }

        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register { server, _, _ ->
            SyncedConfigRegistry.onEndDataReload(
                server.playerManager.playerList,
                { player, id -> ServerPlayNetworking.canSend(player, id) },
                { player, payload -> ServerPlayNetworking.send(player, payload) }
            )
        }

        ServerPlayNetworking.registerGlobalReceiver(ConfigUpdateC2SCustomPayload.type){ payload, context ->
            SyncedConfigRegistry.receiveConfigUpdate(
                payload.updates,
                context.player().server,
                context.player(),
                payload.changeHistory,
                { player, id -> ServerPlayNetworking.canSend(player, id) },
                { player, pl -> ServerPlayNetworking.send(player, pl) }
            )
        }

        ServerPlayNetworking.registerGlobalReceiver(SettingForwardCustomPayload.type){ payload, context ->
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
    }
}