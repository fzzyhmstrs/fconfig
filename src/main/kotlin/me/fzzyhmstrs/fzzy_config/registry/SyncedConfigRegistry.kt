package me.fzzyhmstrs.fzzy_config.registry

import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.config.ConfigHelper
import me.fzzyhmstrs.fzzy_config.config.ValidationResult
import me.fzzyhmstrs.fzzy_config.networking.ConfigS2CSyncCustomPayload
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking
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

    internal fun registerClient() {
        ClientConfigurationNetworking.registerGlobalReceiver(ConfigS2CSyncCustomPayload.type){payload, _ ->
            val id = payload.id
            val configString = payload.serializedConfig
            if (newConfigs.containsKey(id)){
                val config = newConfigs[id] ?: return@registerGlobalReceiver
                val errors = mutableListOf<String>()
                val result = ConfigHelper.deserializeConfig(config, configString, errors, false)
                result.first.writeError(errors)
            }
        }
    }

    internal fun registerAll() {
        PayloadTypeRegistry.configurationC2S().register(ConfigS2CSyncCustomPayload.type, ConfigS2CSyncCustomPayload.codec)
        PayloadTypeRegistry.configurationS2C().register(ConfigS2CSyncCustomPayload.type, ConfigS2CSyncCustomPayload.codec)
        PayloadTypeRegistry.playC2S().register(ConfigS2CSyncCustomPayload.type, ConfigS2CSyncCustomPayload.codec)
        PayloadTypeRegistry.playS2C().register(ConfigS2CSyncCustomPayload.type, ConfigS2CSyncCustomPayload.codec)
        ServerConfigurationConnectionEvents.CONFIGURE.register { handler, _ ->
            for ((id, config) in newConfigs) {
                val syncErrors = mutableListOf<String>()
                val payload = ConfigS2CSyncCustomPayload(id, ConfigHelper.serializeConfig(config, syncErrors, false))
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
                    val payload = ConfigS2CSyncCustomPayload(id, ConfigHelper.serializeConfig(config, syncErrors, false))
                    if (syncErrors.isNotEmpty()){
                        val syncError = ValidationResult.error(true,"Error encountered while serializing config for S2C datapack reload sync.")
                        syncError.writeError(syncErrors)
                    }
                    ServerPlayNetworking.send(player, payload)
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
    fun registerConfig(id: String, config: Config){
        newConfigs[id] = config
    }
}