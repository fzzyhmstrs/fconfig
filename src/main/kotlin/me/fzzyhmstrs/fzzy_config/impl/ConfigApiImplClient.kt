package me.fzzyhmstrs.fzzy_config.impl

import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.registry.ClientConfigRegistry
import java.util.*

object ConfigApiImplClient {

    fun registerConfig(config: Config){
        ClientConfigRegistry.registerConfig(config)
    }

    fun openScreen(scope: String){
        ClientConfigRegistry.openScreen(scope)
    }

    fun handleForwardedUpdate(update: String, player: UUID, scope: String){
        ClientConfigRegistry.handleForwardedUpdate(update, player, scope)
    }

}