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

internal object FzzyNetworkEvents {

    fun onConfigure(syncedConfigs : Map<String, Config>, sender: Consumer<CustomPayload<*>>) {
        for ((id, config) in syncedConfigs) {
            val syncErrors = mutableListOf<String>()
            val payload = ConfigSyncS2CCustomPayload(id, ConfigApi.serializeConfig(config, syncErrors, 0)) //Don't ignore NonSync on a synchronization action
            if (syncErrors.isNotEmpty()) {
                val syncError = ValidationResult.error(true, "Error encountered while serializing config for S2C configuration stage sync.")
                syncError.writeError(syncErrors)
            }
            sender.accept(payload)
        }
    }

    fun onJoin(syncedConfigs : Map<String, Config>, player: ServerPlayerEntity, sender: BiConsumer<ServerPlayerEntity, CustomPayload<*>>) {
        for ((id, config) in syncedConfigs) {
            val perms = ConfigApiImpl.generatePermissionsReport(player, config, 0)
            val payload = ConfigPermissionsS2CCustomPayload(id, perms)
            sender.sendPacket(player, payload)
        }
    }

    fun endDataReload(players: List<ServerPlayerEntity>, canSender: BiPredicate<ServerPlayerEntity, CustomPayload<*>>, sender: BiConsumer<ServerPlayerEntity, CustomPayload<*>>) {
        for (player in players) {
            if (!canSender.test(player, ConfigSyncS2CCustomPayload.type)) continue
            for ((id, config) in syncedConfigs) {
                val syncErrors = mutableListOf<String>()
                val syncPayload = ConfigSyncS2CCustomPayload(id, ConfigApi.serializeConfig(config, syncErrors, 0)) //Don't ignore NonSync on a synchronization action
                if (syncErrors.isNotEmpty()) {
                    val syncError = ValidationResult.error(true, "Error encountered while serializing config for S2C datapack reload sync.")
                    syncError.writeError(syncErrors)
                }
                sender.accept(player, syncPayload)
                if (!ServerPlayNetworking.canSend(player, ConfigPermissionsS2CCustomPayload.type)) continue
                val perms = ConfigApiImpl.generatePermissionsReport(player, config, 0)
                val permsPayload = ConfigPermissionsS2CCustomPayload(id, perms)
                ServerPlayNetworking.send(player, permsPayload)
            }
        }
    }

    fun receiveConfigUpdate(serializedConfigs: Map<String, String>) {
        
    }
}
