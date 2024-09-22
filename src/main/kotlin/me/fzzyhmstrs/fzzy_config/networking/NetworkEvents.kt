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
import net.minecraft.util.Identifier


internal object NetworkEvents {

    fun canSend(playerEntity: ServerPlayerEntity, id: Identifier): Boolean {
        return ServerPlayNetworking.canSend(playerEntity, id)
    }

    fun send(playerEntity: ServerPlayerEntity, payload: FzzyPayload) {
        val buf = PacketByteBufs.create()
        payload.write(buf)
        ServerPlayNetworking.send(playerEntity, payload.getId(), buf)
    }

    private fun receiveUpdate(payload: ConfigUpdateC2SCustomPayload, context: ServerPlayNetworkContext) {
        SyncedConfigRegistry.receiveConfigUpdate(
            payload.updates,
            context.player().server,
            context.player(),
            payload.changeHistory,
            { _, id -> context.canReply(id) },
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
            { _, id -> context.canReply(id) },
            { _, pl -> context.reply(pl) }
        )
    }

    fun registerServer() {

        //PayloadTypeRegistry.configurationC2S().register(ConfigSyncS2CCustomPayload.type, ConfigSyncS2CCustomPayload.codec)
        //PayloadTypeRegistry.configurationS2C().register(ConfigSyncS2CCustomPayload.type, ConfigSyncS2CCustomPayload.codec)
        //PayloadTypeRegistry.playS2C().register(ConfigPermissionsS2CCustomPayload.type, ConfigPermissionsS2CCustomPayload.codec)
        ConfigApi.network().registerS2C(ConfigPermissionsS2CCustomPayload.id, ::ConfigPermissionsS2CCustomPayload, NetworkEventsClient::receivePerms)
        //PayloadTypeRegistry.playC2S().register(ConfigSyncS2CCustomPayload.type, ConfigSyncS2CCustomPayload.codec)
        //PayloadTypeRegistry.playS2C().register(ConfigSyncS2CCustomPayload.type, ConfigSyncS2CCustomPayload.codec)
        ConfigApi.network().registerS2C(ConfigSyncS2CCustomPayload.id, ::ConfigSyncS2CCustomPayload, NetworkEventsClient::receiveSync)
        //PayloadTypeRegistry.playC2S().register(ConfigUpdateS2CCustomPayload.type, ConfigUpdateS2CCustomPayload.codec)
        //PayloadTypeRegistry.playS2C().register(ConfigUpdateS2CCustomPayload.type, ConfigUpdateS2CCustomPayload.codec)
        ConfigApi.network().registerS2C(ConfigUpdateS2CCustomPayload.id, ::ConfigUpdateS2CCustomPayload, NetworkEventsClient::receiveUpdate)
        //PayloadTypeRegistry.playC2S().register(ConfigUpdateC2SCustomPayload.type, ConfigUpdateC2SCustomPayload.codec)
        ConfigApi.network().registerC2S(ConfigUpdateC2SCustomPayload.id, ::ConfigUpdateC2SCustomPayload, this::receiveUpdate)
        //PayloadTypeRegistry.playS2C().register(ConfigUpdateC2SCustomPayload.type, ConfigUpdateC2SCustomPayload.codec)
        //PayloadTypeRegistry.playC2S().register(SettingForwardCustomPayload.type, SettingForwardCustomPayload.codec)
        ConfigApi.network().registerC2S(SettingForwardCustomPayload.id, ::SettingForwardCustomPayload, this::receiveForward)
        //PayloadTypeRegistry.playS2C().register(SettingForwardCustomPayload.type, SettingForwardCustomPayload.codec)
        ConfigApi.network().registerS2C(SettingForwardCustomPayload.id, ::SettingForwardCustomPayload, NetworkEventsClient::receiveForward)

        ServerConfigurationConnectionEvents.CONFIGURE.register { handler, _ ->
            SyncedConfigRegistry.onConfigure(
                { id -> ServerConfigurationNetworking.canSend(handler, id) },
                { payload ->
                    val buf = PacketByteBufs.create()
                    payload.write(buf)
                    ServerConfigurationNetworking.send(handler, payload.getId(), buf)
                }
            )
        }

        ServerPlayConnectionEvents.JOIN.register { handler, sender, server ->
            SyncedConfigRegistry.onJoin(
                handler.player,
                server,
                { player, id -> ServerPlayNetworking.canSend(player, id) },
                { _, payload ->
                    val buf = PacketByteBufs.create()
                    payload.write(buf)
                    sender.sendPacket(payload.getId(), buf)
                }
            )
        }

        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register { server, _, _ ->
            SyncedConfigRegistry.onEndDataReload(
                server.playerManager.playerList,
                { player, id -> ServerPlayNetworking.canSend(player, id) },
                { player, payload ->
                    val buf = PacketByteBufs.create()
                    payload.write(buf)
                    ServerPlayNetworking.send(player, payload.getId(), buf)
                }
            )
            ConfigApiImpl.invalidateLookup()
        }

        /*ServerPlayNetworking.registerGlobalReceiver(ConfigUpdateC2SCustomPayload.type){ payload, context ->
            SyncedConfigRegistry.receiveConfigUpdate(
                payload.updates,
                server,
                serverPlayer,
                payload.changeHistory,
                { player, id -> ServerPlayNetworking.canSend(player, id) },
                { player, pl ->
                    val b = PacketByteBufs.create()
                    pl.write(b)
                    ServerPlayNetworking.send(player, pl.getId(), b)
                }
            )
        }*/

        /*ServerPlayNetworking.registerGlobalReceiver(SettingForwardCustomPayload.type){ payload, context ->

        }*/
    }
}