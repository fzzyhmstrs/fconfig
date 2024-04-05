package me.fzzyhmstrs.fzzy_config.impl

import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.registry.ClientConfigRegistry
import net.minecraft.client.MinecraftClient
import java.util.*

object ConfigApiImplClient {

    internal fun registerConfig(config: Config, baseConfig: Config){
        ClientConfigRegistry.registerConfig(config, baseConfig)
    }

    internal fun openScreen(scope: String) {
        ClientConfigRegistry.openScreen(scope)
    }

    internal fun handleForwardedUpdate(update: String, player: UUID, scope: String) {
        ClientConfigRegistry.handleForwardedUpdate(update, player, scope)
    }

    internal fun getPlayerPermissionLevel(): Int{
        val client = MinecraftClient.getInstance()
        if(client.server != null && client?.server?.isRemote != true) return 4 // single player game, they can change whatever they want
        var i = 0
        while(client.player?.hasPermissionLevel(i) == true){
            i++
        }
        return i
    }
}