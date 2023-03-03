package me.fzzyhmstrs.fzzy_config.registry

import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.config.FcTestConfig
import me.fzzyhmstrs.fzzy_config.config_util.ReadMeBuilder
import me.fzzyhmstrs.fzzy_config.interfaces.SyncedConfig
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.util.Identifier


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
                if (id == "fc_test_config"){
                    FcTestConfig.printTest()
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

    fun registerConfig(id: String, config: SyncedConfig){
        newConfigs[id] = config
    }
}