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
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.registry.SyncedConfigRegistry
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.networking.v1.*
import net.minecraft.server.network.ServerPlayNetworkHandler
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

    fun syncConfigs(handler: ServerPlayNetworkHandler) {
        for ((id, config) in SyncedConfigRegistry.syncedConfigs()) {
            val syncErrors = mutableListOf<String>()
            val payload = ConfigSyncS2CCustomPayload(id, ConfigApi.serializeConfig(config, syncErrors, 0)) //Don't ignore NonSync on a synchronization action
            if (syncErrors.isNotEmpty()) {
                val syncError = ValidationResult.error(true, "Error encountered while serializing config for S2C configuration stage sync.")
                syncError.writeError(syncErrors)
            }
            val buf = PacketByteBufs.create()
            payload.write(buf)
            ServerPlayNetworking.send(handler.player, ConfigSyncS2CCustomPayload.id, buf)
        }
    }


    fun registerServer() {

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
        }

        ServerPlayNetworking.registerGlobalReceiver(ConfigUpdateC2SCustomPayload.id) { server, serverPlayer, _, buf, _ ->
            val payload = ConfigUpdateC2SCustomPayload(buf)
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
        }

        ServerPlayNetworking.registerGlobalReceiver(SettingForwardCustomPayload.id) { _, serverPlayer, _, buf, _ ->
            val payload = SettingForwardCustomPayload(buf)
            SyncedConfigRegistry.receiveSettingForward(
                payload.player,
                serverPlayer,
                payload.scope,
                payload.update,
                payload.summary,
                { player, id -> ServerPlayNetworking.canSend(player, id) },
                { player, pl ->
                    val b = PacketByteBufs.create()
                    pl.write(b)
                    ServerPlayNetworking.send(player, pl.getId(), b)
                }
            )
        }
    }
}