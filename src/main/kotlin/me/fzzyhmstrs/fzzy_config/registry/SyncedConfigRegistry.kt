package me.fzzyhmstrs.fzzy_config.registry

import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImplClient
import me.fzzyhmstrs.fzzy_config.networking.ConfigSyncS2CCustomPayload
import me.fzzyhmstrs.fzzy_config.networking.ConfigUpdateC2SCustomPayload
import me.fzzyhmstrs.fzzy_config.networking.ConfigUpdateS2CCustomPayload
import me.fzzyhmstrs.fzzy_config.networking.SettingForwardCustomPayload
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.entity.player.PlayerEntity
import java.util.*

/**
 * The registry for [Config] instances.
 *
 * This is not a "true" registry in the Minecraft since; as such there are not the typical helper methods like get(), getId(), etc. This registry's scope is much narrower, handling synchronization and updates of Configs.
 */
object SyncedConfigRegistry {

    private val syncedConfigs : MutableMap<String, Config> = mutableMapOf()

    fun forwardSetting(update: String, player: UUID, scope: String, summary: String) {
        ClientPlayNetworking.send(SettingForwardCustomPayload(update,player,scope,summary))
    }

    fun updateServer(serializedConfigs: Map<String,String>, changeHistory: List<String>, playerPerm: Int){
        ClientPlayNetworking.send(ConfigUpdateC2SCustomPayload(serializedConfigs, changeHistory, playerPerm))
    }

    internal fun registerClient() {
        //receives the entire NonSync config sent by the server during CONFIGURATION stage
        ClientConfigurationNetworking.registerGlobalReceiver(ConfigSyncS2CCustomPayload.type){ payload, _ ->
            val id = payload.id
            val configString = payload.serializedConfig
            if (syncedConfigs.containsKey(id)){
                val config = syncedConfigs[id] ?: return@registerGlobalReceiver
                val errors = mutableListOf<String>()
                val result = ConfigApi.deserializeConfig(config, configString, errors, 2) //0: Don't ignore NonSync on a synchronization action, 2: Watch for RequiresRestart
                result.first.writeError(errors)
                result.first.get().save() //save config to the client
            }
        }
        //receives a config update from the server after another client pushes changes to it.
        ClientPlayNetworking.registerGlobalReceiver(ConfigUpdateS2CCustomPayload.type){ payload, _ ->
            val serializedConfigs = payload.updates
            for ((id, configString) in serializedConfigs) {
                if (syncedConfigs.containsKey(id)) {
                    val config = syncedConfigs[id] ?: return@registerGlobalReceiver
                    val errors = mutableListOf<String>()
                    val result = ConfigApiImpl.deserializeUpdate(config, configString, errors)
                    result.writeError(errors)
                    result.get().save()
                }
            }
        }
        //receives an update forwarded from another player and passes it to the client registry for handling.
        ClientPlayNetworking.registerGlobalReceiver(SettingForwardCustomPayload.type){ payload, _ ->
            val update = payload.update
            val sendingUuid = payload.player
            val scope = payload.scope
            val summary = payload.summary
            ConfigApiImplClient.handleForwardedUpdate(update, sendingUuid, scope, summary)
        }
    }

    internal fun registerAll() {
        PayloadTypeRegistry.configurationC2S().register(ConfigSyncS2CCustomPayload.type, ConfigSyncS2CCustomPayload.codec)
        PayloadTypeRegistry.configurationS2C().register(ConfigSyncS2CCustomPayload.type, ConfigSyncS2CCustomPayload.codec)
        PayloadTypeRegistry.playC2S().register(ConfigSyncS2CCustomPayload.type, ConfigSyncS2CCustomPayload.codec)
        PayloadTypeRegistry.playS2C().register(ConfigSyncS2CCustomPayload.type, ConfigSyncS2CCustomPayload.codec)
        PayloadTypeRegistry.playC2S().register(ConfigUpdateS2CCustomPayload.type, ConfigUpdateS2CCustomPayload.codec)
        PayloadTypeRegistry.playS2C().register(ConfigUpdateS2CCustomPayload.type, ConfigUpdateS2CCustomPayload.codec)
        PayloadTypeRegistry.playC2S().register(ConfigUpdateC2SCustomPayload.type, ConfigUpdateC2SCustomPayload.codec)
        PayloadTypeRegistry.playS2C().register(ConfigUpdateC2SCustomPayload.type, ConfigUpdateC2SCustomPayload.codec)
        PayloadTypeRegistry.playC2S().register(SettingForwardCustomPayload.type, SettingForwardCustomPayload.codec)
        PayloadTypeRegistry.playS2C().register(SettingForwardCustomPayload.type, SettingForwardCustomPayload.codec)

        //handles synchronization of configs to clients on CONFIGURE stage of login
        ServerConfigurationConnectionEvents.CONFIGURE.register { handler, _ ->
            for ((id, config) in syncedConfigs) {
                val syncErrors = mutableListOf<String>()
                val payload = ConfigSyncS2CCustomPayload(id, ConfigApi.serializeConfig(config, syncErrors, 0)) //Don't ignore NonSync on a synchronization action
                if (syncErrors.isNotEmpty()){
                    val syncError = ValidationResult.error(true,"Error encountered while serializing config for S2C configuration stage sync.")
                    syncError.writeError(syncErrors)
                }
                ServerConfigurationNetworking.send(handler, payload)
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
                    ServerPlayNetworking.send(player, payload)
                }
            }
        }
        //receives an update from a permissible client, throwing a cheat warning to the logs and to online moderators if permissions don't match (and discarding the update)
        //deserializes the updates to server configs, then propagates the updates to other online clients
        ServerPlayNetworking.registerGlobalReceiver(ConfigUpdateC2SCustomPayload.type){ payload, context ->
            val permLevel = payload.playerPerm
            val serializedConfigs = payload.updates
            if(!context.player().hasPermissionLevel(permLevel)){
                FC.LOGGER.error("Player [${context.player().name}] may have tried to cheat changes to the Server Config! Their perm level: ${getPlayerPermissionLevel(context.player())}, perm level synced from client: $permLevel")
                val changes = payload.changeHistory
                ConfigApiImpl.printChangeHistory(changes, serializedConfigs.keys.toString(), context.player())
                for (player in context.player().server.playerManager.playerList){
                    if(player.hasPermissionLevel(2))
                        player.sendMessageToClient("fc.networking.permission.cheat".translate(context.player().name), false)
                }
            }
            for ((id,configString) in serializedConfigs) {
                val config = syncedConfigs[id] ?: continue
                val errors = mutableListOf<String>()
                val result = ConfigApiImpl.deserializeUpdate(config, configString, errors)
                result.writeError(errors)
                result.get().save()
            }
            for (player in context.player().server.playerManager.playerList) {
                if (player == context.player()) continue // don't push back to the player that just sent the update
                val newPayload = ConfigUpdateS2CCustomPayload(serializedConfigs)
                ServerPlayNetworking.send(player, newPayload)
            }
            val changes = payload.changeHistory
            ConfigApiImpl.printChangeHistory(changes, serializedConfigs.keys.toString(), context.player())
        }
        //receives a forwarded client update and passes it along to the recipient
        ServerPlayNetworking.registerGlobalReceiver(SettingForwardCustomPayload.type){ payload, context ->
            val uuid = payload.player
            val receivingPlayer = context.player().server.playerManager.getPlayer(uuid) ?: return@registerGlobalReceiver
            val scope = payload.scope
            val update = payload.update
            val summary = payload.summary
            val sendingPlayer = context.player()
            ServerPlayNetworking.send(receivingPlayer,SettingForwardCustomPayload(update,sendingPlayer.uuid,scope,summary))
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