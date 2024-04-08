package me.fzzyhmstrs.fzzy_config.impl

import me.fzzyhmstrs.fzzy_config.registry.ClientConfigRegistry
import net.fabricmc.loader.api.ModContainer
import net.minecraft.client.gui.screen.Screen
import java.util.function.BiFunction

object ConfigCatalogueCompat {
    @JvmStatic
    fun createConfigFactoryProvider(): Map<String, BiFunction<Screen, ModContainer, Screen?>> {
        return ClientConfigRegistry.getScreenScopes().associateWith { scope -> BiFunction { _: Screen,_: ModContainer -> ClientConfigRegistry.provideScreen(scope) } }
    }
}