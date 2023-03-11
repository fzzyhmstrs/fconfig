package me.fzzyhmstrs.fzzy_config.registry

import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.config_util.ReadMeBuilder
import me.fzzyhmstrs.fzzy_config.interfaces.SyncedConfig
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
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

    private val SYNC_CONFIG_PACKET = Identifier(FC.MOD_ID,"sync_config_packet")
    private val newConfigs : MutableMap<String, SyncedConfig> = mutableMapOf()

    internal fun registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(SYNC_CONFIG_PACKET) { _, _, buf, _ ->
            val id = buf.readString()
            if (newConfigs.containsKey(id)){
                val newConfig = newConfigs[id]?:return@registerGlobalReceiver
                newConfig.readFromServer(buf)
                if (newConfig is ReadMeBuilder){
                    newConfig.build()
                    newConfig.writeReadMe()
                }
            }
        }
    }

    internal fun registerServer() {
        ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
            val player = handler.player
            newConfigs.forEach {
                val buf = PacketByteBufs.create()
                buf.writeString(it.key)
                it.value.writeToClient(buf)
                ServerPlayNetworking.send(player, SYNC_CONFIG_PACKET, buf)
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