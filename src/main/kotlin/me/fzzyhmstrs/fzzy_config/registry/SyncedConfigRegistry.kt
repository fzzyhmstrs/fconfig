package me.fzzyhmstrs.fzzy_config.registry

import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import me.fzzyhmstrs.fzzy_config.networking.ConfigC2SUpdateCustomPayload
import me.fzzyhmstrs.fzzy_config.networking.ConfigS2CSyncCustomPayload
import me.fzzyhmstrs.fzzy_config.networking.ConfigS2CUpdateCustomPayload
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking

/**
 * The registry for [SyncedConfig] instances.
 *
 * This is not a "true" registry in the Minecraft since; as such there are not the typical helper methods like get(), getId(), etc.
 *
 * This registry's scope is much narrower, with its only intended goal being automatic synchronization of configurations on the JOIN event.
 *
 * Most users will not have to directly interact with this registry at all, as this is handled in the background of helper classes
 */
object SyncedConfigRegistry {

    private val newConfigs : MutableMap<String, Config> = mutableMapOf()

    fun <T: Config> updateServer(config: T, changeHistory: List<String>){
        val errors = mutableListOf<String>()
        val configString = ConfigApi.serializeDirty(config,errors)
        if (errors.isNotEmpty()){
            val errorsResult = ValidationResult.error(true, "Critical error(s) encountered while serializing client-updated Config Class! Output may not be complete.")
            errorsResult.writeError(errors)
        }
        val payload = ConfigC2SUpdateCustomPayload(config.getId().toString(),configString, changeHistory)
        ClientPlayNetworking.send(payload)
    }

    internal fun registerClient() {
        //receives the entire NonSync config sent by the server during CONFIGURATION stage
        ClientConfigurationNetworking.registerGlobalReceiver(ConfigS2CSyncCustomPayload.type){payload, _ ->
            val id = payload.id
            val configString = payload.serializedConfig
            if (newConfigs.containsKey(id)){
                val config = newConfigs[id] ?: return@registerGlobalReceiver
                val errors = mutableListOf<String>()
                val result = ConfigApi.deserializeConfig(config, configString, errors, false) //Don't ignore NonSync on a syncronization action
                result.first.writeError(errors)
            }
        }
        //receives the dirty config update from the server after a client pushes changes to it.
        ClientPlayNetworking.registerGlobalReceiver(ConfigS2CUpdateCustomPayload.type){ payload, _ ->
            val id = payload.id
            val configString = payload.serializedConfig
            if (newConfigs.containsKey(id)){
                val config = newConfigs[id] ?: return@registerGlobalReceiver
                val errors = mutableListOf<String>()
                val result = ConfigApi.deserializeDirty(config, configString, errors)
                result.writeError(errors)
            }
        }
    }

    internal fun registerAll() {
        PayloadTypeRegistry.configurationC2S().register(ConfigS2CSyncCustomPayload.type, ConfigS2CSyncCustomPayload.codec)
        PayloadTypeRegistry.configurationS2C().register(ConfigS2CSyncCustomPayload.type, ConfigS2CSyncCustomPayload.codec)
        PayloadTypeRegistry.playC2S().register(ConfigS2CSyncCustomPayload.type, ConfigS2CSyncCustomPayload.codec)
        PayloadTypeRegistry.playS2C().register(ConfigS2CSyncCustomPayload.type, ConfigS2CSyncCustomPayload.codec)
        PayloadTypeRegistry.playC2S().register(ConfigS2CUpdateCustomPayload.type, ConfigS2CUpdateCustomPayload.codec)
        PayloadTypeRegistry.playS2C().register(ConfigS2CUpdateCustomPayload.type, ConfigS2CUpdateCustomPayload.codec)
        PayloadTypeRegistry.playC2S().register(ConfigC2SUpdateCustomPayload.type, ConfigC2SUpdateCustomPayload.codec)
        PayloadTypeRegistry.playS2C().register(ConfigC2SUpdateCustomPayload.type, ConfigC2SUpdateCustomPayload.codec)
        ServerConfigurationConnectionEvents.CONFIGURE.register { handler, _ ->
            for ((id, config) in newConfigs) {
                val syncErrors = mutableListOf<String>()
                val payload = ConfigS2CSyncCustomPayload(id, ConfigApi.serializeConfig(config, syncErrors, false)) //Don't ignore NonSync on a syncronization action
                if (syncErrors.isNotEmpty()){
                    val syncError = ValidationResult.error(true,"Error encountered while serializing config for S2C configuration stage sync.")
                    syncError.writeError(syncErrors)
                }
                ServerConfigurationNetworking.send(handler, payload)
            }
        }


        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register { server, _, _ ->
            val players = server.playerManager.playerList
            for (player in players) {
                for ((id, config) in newConfigs) {
                    val syncErrors = mutableListOf<String>()
                    val payload = ConfigS2CSyncCustomPayload(id, ConfigApi.serializeConfig(config, syncErrors, false)) //Don't ignore NonSync on a syncronization action
                    if (syncErrors.isNotEmpty()){
                        val syncError = ValidationResult.error(true,"Error encountered while serializing config for S2C datapack reload sync.")
                        syncError.writeError(syncErrors)
                    }
                    ServerPlayNetworking.send(player, payload)
                }
            }
        }
        ServerPlayNetworking.registerGlobalReceiver(ConfigC2SUpdateCustomPayload.type){ payload, context ->
            val id = payload.id
            val configString = payload.serializedConfig
            if (newConfigs.containsKey(id)){
                val config = newConfigs[id] ?: return@registerGlobalReceiver
                val errors = mutableListOf<String>()
                val result = ConfigApi.deserializeDirty(config, configString, errors)
                result.writeError(errors)
                val changes = payload.changeHistory
                ConfigApiImpl.printChangeHistory(changes, id, context.player())
                for (player in context.player().server.playerManager.playerList){
                    if (player == context.player()) continue // don't push back to the player that just sent the update
                    val newPayload = ConfigS2CSyncCustomPayload(id, configString)
                    ServerPlayNetworking.send(player, newPayload)
                }
            }
        }

    }

    /**
     * Synced Configurations are registered here. If using a [SyncedConfigWithReadme](me.fzzyhmstrs.fzzy_config.config_util.SyncedConfigWithReadMe) or similar helper class, this registration is done automatically in their init methods.
     *
     * @param id the unique string ID of this config. using identifier notation (namespace:path) may help ensure uniqueness
     * @param config a [SyncedConfig] to pass into the registry map
     */
    fun registerConfig(config: Config){
        newConfigs[config.getId().toString()] = config
    }
}
