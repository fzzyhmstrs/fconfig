/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.registry

import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.config.ConfigContext.Keys.RESTART_KEY
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImplClient
import me.fzzyhmstrs.fzzy_config.networking.ConfigSyncS2CCustomPayload
import me.fzzyhmstrs.fzzy_config.networking.ConfigUpdateC2SCustomPayload
import me.fzzyhmstrs.fzzy_config.networking.ConfigUpdateS2CCustomPayload
import me.fzzyhmstrs.fzzy_config.networking.SettingForwardCustomPayload
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.entity.player.PlayerEntity
import java.util.*

/**
 * Synchronization registry for [Config] instances. Handles syncing configs, sending updates to and from clients, and forwarding settings between users
 *
 * This is not a "true" registry in the Minecraft sense; as such there are not the typical helper methods like get(), getId(), etc. This registry's scope is much narrower, handling synchronization and updates of Configs.
 * @author fzzyhmstrs
 * @since 0.1.0
 */
internal object SyncedConfigRegistry {

    private val syncedConfigs : MutableMap<String, Config> = mutableMapOf()

    fun forwardSetting(update: String, player: UUID, scope: String, summary: String) {
        val buf = PacketByteBufs.create()
        SettingForwardCustomPayload(update,player,scope,summary).write(buf)
        ClientPlayNetworking.send(SettingForwardCustomPayload.id,buf)
    }

    fun updateServer(serializedConfigs: Map<String,String>, changeHistory: List<String>, playerPerm: Int){
        val buf = PacketByteBufs.create()
        ConfigUpdateC2SCustomPayload(serializedConfigs, changeHistory, playerPerm).write(buf)
        ClientPlayNetworking.send(ConfigUpdateC2SCustomPayload.id,buf)
    }

    internal fun registerClient() {
        //receives the entire NonSync config sent by the server during CONFIGURATION stage
        ClientPlayNetworking.registerGlobalReceiver(ConfigSyncS2CCustomPayload.id){ client, _, buf, _ ->
            val payload = ConfigSyncS2CCustomPayload(buf)
            val id = payload.id
            val configString = payload.serializedConfig
            if (syncedConfigs.containsKey(id)){
                val config = syncedConfigs[id] ?: return@registerGlobalReceiver
                val errors = mutableListOf<String>()
                val result = ConfigApi.deserializeConfig(config, configString, errors, 2) //0: Don't ignore NonSync on a synchronization action, 2: Watch for RequiresRestart
                val restart = result.get().getBoolean(RESTART_KEY)
                result.writeError(errors)
                result.get().config.save() //save config to the client
                if (restart) {
                    println("A RESTART IS NEEDED AAAAHHHHH")
                }
            }
        }
        //receives a config update from the server after another client pushes changes to it.
        ClientPlayNetworking.registerGlobalReceiver(ConfigUpdateS2CCustomPayload.id){ client, _,buf,_ ->
            val payload = ConfigUpdateS2CCustomPayload(buf)
            val serializedConfigs = payload.updates
            for ((id, configString) in serializedConfigs) {
                if (syncedConfigs.containsKey(id)) {
                    val config = syncedConfigs[id] ?: return@registerGlobalReceiver
                    val errors = mutableListOf<String>()
                    val result = ConfigApiImpl.deserializeUpdate(config, configString, errors)
                    val restart = result.get().getBoolean(RESTART_KEY)
                    result.writeError(errors)
                    result.get().config.save()
                    if (restart) {
                        println("A RESTART IS NEEDED AAAAHHHHH")
                        client.player?.sendMessage("fc.config.restart.update".translate())
                    }
                }
            }
        }
        //receives an update forwarded from another player and passes it to the client registry for handling.
        ClientPlayNetworking.registerGlobalReceiver(SettingForwardCustomPayload.id){ _,_,buf, _ ->
            val payload = SettingForwardCustomPayload(buf)
            val update = payload.update
            val sendingUuid = payload.player
            val scope = payload.scope
            val summary = payload.summary
            ConfigApiImplClient.handleForwardedUpdate(update, sendingUuid, scope, summary)
        }
    }

    internal fun registerAll() {

        //handles synchronization of configs to clients on CONFIGURE stage of login
        ServerPlayConnectionEvents.JOIN.register { handler, sender, server ->
            for ((id, config) in syncedConfigs) {
                val syncErrors = mutableListOf<String>()
                val payload = ConfigSyncS2CCustomPayload(id, ConfigApi.serializeConfig(config, syncErrors, 0)) //Don't ignore NonSync on a synchronization action
                if (syncErrors.isNotEmpty()){
                    val syncError = ValidationResult.error(true,"Error encountered while serializing config for S2C configuration stage sync.")
                    syncError.writeError(syncErrors)
                }
                val buf = PacketByteBufs.create()
                payload.write(buf)
                ServerPlayNetworking.send(handler.player,ConfigSyncS2CCustomPayload.id, buf)
            }
        }
        //syncs configs to clients after datapack reload
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register { server, _, _ ->
            val players = server.playerManager.playerList
            for (player in players) {
                for ((id, config) in syncedConfigs) {
                    val syncErrors = mutableListOf<String>()
                    val payload = ConfigSyncS2CCustomPayload(id, ConfigApi.serializeConfig(config, syncErrors, 0)) //Don't ignore NonSync on a synchronization action
                    if (syncErrors.isNotEmpty()){
                        val syncError = ValidationResult.error(true,"Error encountered while serializing config for S2C datapack reload sync.")
                        syncError.writeError(syncErrors)
                    }
                    val buf = PacketByteBufs.create()
                    payload.write(buf)
                    ServerPlayNetworking.send(player, ConfigSyncS2CCustomPayload.id, buf)
                }
            }
        }
        //receives an update from a permissible client, throwing a cheat warning to the logs and to online moderators if permissions don't match (and discarding the update)
        //deserializes the updates to server configs, then propagates the updates to other online clients
        ServerPlayNetworking.registerGlobalReceiver(ConfigUpdateC2SCustomPayload.id){ server, serverPlayer, context, buf, sender ->
            val payload = ConfigUpdateC2SCustomPayload(buf)
            val permLevel = payload.playerPerm
            val serializedConfigs = payload.updates
            if(!serverPlayer.hasPermissionLevel(permLevel)){
                FC.LOGGER.error("Player [${serverPlayer.name}] may have tried to cheat changes to the Server Config! Their perm level: ${getPlayerPermissionLevel(serverPlayer)}, perm level synced from client: $permLevel")
                val changes = payload.changeHistory
                ConfigApiImpl.printChangeHistory(changes, serializedConfigs.keys.toString(), serverPlayer)
                for (player in serverPlayer.server.playerManager.playerList){
                    if(player.hasPermissionLevel(2))
                        player.sendMessageToClient("fc.networking.permission.cheat".translate(serverPlayer.name), false)
                }
            }
            for ((id,configString) in serializedConfigs) {
                val config = syncedConfigs[id] ?: continue
                val errors = mutableListOf<String>()
                val result = ConfigApiImpl.deserializeUpdate(config, configString, errors)
                val restart = result.get().getBoolean(RESTART_KEY)
                result.writeError(errors)
                result.get().config.save()
                if (restart) {
                    FC.LOGGER.error("The server has received a config update that may require a restart, please consult the change history below for details. Connected clients have been automatically updated and notified of the potential for restart.")
                }
            }
            for (player in serverPlayer.server.playerManager.playerList) {
                if (player == serverPlayer) continue // don't push back to the player that just sent the update
                val newPayload = ConfigUpdateS2CCustomPayload(serializedConfigs)
                val newBuf = PacketByteBufs.create()
                newPayload.write(newBuf)
                ServerPlayNetworking.send(player, ConfigUpdateS2CCustomPayload.id, newBuf)
            }
            val changes = payload.changeHistory
            ConfigApiImpl.printChangeHistory(changes, serializedConfigs.keys.toString(), serverPlayer)
        }
        //receives a forwarded client update and passes it along to the recipient
        ServerPlayNetworking.registerGlobalReceiver(SettingForwardCustomPayload.id){ server, serverPlayer, context, buf, sender ->
            val payload = SettingForwardCustomPayload(buf)
            val uuid = payload.player
            val receivingPlayer = serverPlayer.server.playerManager.getPlayer(uuid) ?: return@registerGlobalReceiver
            val scope = payload.scope
            val update = payload.update
            val summary = payload.summary
            val sendingPlayer = serverPlayer
            val newPayload = SettingForwardCustomPayload(update,sendingPlayer.uuid,scope,summary)
            val newBuf = PacketByteBufs.create()
            newPayload.write(newBuf)
            ServerPlayNetworking.send(receivingPlayer,SettingForwardCustomPayload.id,newBuf)
        }
    }

    private fun getPlayerPermissionLevel(player: PlayerEntity): Int{
        var i = 0
        while(player.hasPermissionLevel(i)){
            i++
        }
        return i
    }

    internal fun hasConfig(id: String): Boolean{
        return syncedConfigs.containsKey(id)
    }

    internal fun registerConfig(config: Config){
        syncedConfigs[config.getId().toTranslationKey()] = config
    }
}