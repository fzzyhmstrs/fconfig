package me.fzzyhmstrs.fzzy_config.registry

import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.config.ReadMeBuilder
import me.fzzyhmstrs.fzzy_config.config.SyncedConfigHelperV1
import me.fzzyhmstrs.fzzy_config.interfaces.SyncedConfig
import me.fzzyhmstrs.fzzy_config.networking.ConfigS2CSyncCustomPayload
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.packet.CustomPayload
import net.minecraft.resource.ResourceReloader
import net.minecraft.util.Identifier

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

    private val newConfigs : MutableMap<String, SyncedConfig> = mutableMapOf()

    internal fun registerClient() {
        ClientConfigurationNetworking.registerGlobalReceiver(ConfigS2CSyncCustomPayload.type){payload, context ->
            val id = payload.id
            val configString = payload.serializedConfig
            if (newConfigs.containsKey(id)){
                val config = newConfigs[id] ?: return@registerGlobalReceiver
                val errors = mutableListOf<String>()
                val result = SyncedConfigHelperV1.deserializeConfig(config, configString, errors, false)
                result.writeError(errors)
            }
        }
    }

    internal fun registerAll() {
        PayloadTypeRegistry.configurationC2S().register(ConfigS2CSyncCustomPayload.type, ConfigS2CSyncCustomPayload.codec)
        PayloadTypeRegistry.configurationS2C().register(ConfigS2CSyncCustomPayload.type, ConfigS2CSyncCustomPayload.codec)
        ServerConfigurationConnectionEvents.CONFIGURE.register { handler, _ ->
            for ((id, config) in newConfigs) {
                val payload = ConfigS2CSyncCustomPayload(id, SyncedConfigHelperV1.serializeConfig(config, false))
                ServerConfigurationNetworking.send(handler, payload)
            }
        }


        ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
            val player = handler.player
            newConfigs.forEach {
                val buf = PacketByteBufs.create()
                buf.writeString(it.key)
                it.value.writeToClient(buf)
                //ServerPlayNetworking.send(player, SYNC_CONFIG_PACKET, buf)
            }
        }
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register { server, _, _ ->
            val players = server.playerManager.playerList
            for (player in players) {
                newConfigs.forEach {
                    if (it.value is ResourceReloader) {
                        val buf = PacketByteBufs.create()
                        buf.writeString(it.key)
                        it.value.writeToClient(buf)
                        //ServerPlayNetworking.send(player, SYNC_CONFIG_PACKET, buf)
                    }
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
    fun registerConfig(id: String, config: SyncedConfig){
        newConfigs[id] = config
    }
}