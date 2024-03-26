package me.fzzyhmstrs.fzzy_config.impl

import me.fzzyhmstrs.fzzy_config.api.PopupWidgetScreen
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.registry.ClientConfigRegistry
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.PopupScreen
import java.util.*

object ConfigApiImplClient {

    internal fun registerConfig(config: Config){
        ClientConfigRegistry.registerConfig(config)
    }

    internal fun openScreen(scope: String){
        ClientConfigRegistry.openScreen(scope)
    }

    internal fun handleForwardedUpdate(update: String, player: UUID, scope: String){
        ClientConfigRegistry.handleForwardedUpdate(update, player, scope)
    }

    internal fun setPopup(popup: PopupWidget?){
        (MinecraftClient.getInstance().currentScreen as? PopupWidgetScreen)?.setPopup(popup)
    }

}