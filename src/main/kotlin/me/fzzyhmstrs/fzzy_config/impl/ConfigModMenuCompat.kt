package me.fzzyhmstrs.fzzy_config.impl

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import me.fzzyhmstrs.fzzy_config.registry.ClientConfigRegistry
import org.jetbrains.annotations.ApiStatus.Internal

@Internal
object ConfigModMenuCompat: ModMenuApi {
    override fun getProvidedConfigScreenFactories(): MutableMap<String, ConfigScreenFactory<*>> {
        println("I checked for mod menu stuff")
        return ClientConfigRegistry.getScreenScopes().associateWith { scope -> ConfigScreenFactory { _ -> ClientConfigRegistry.provideScreen(scope) } }.toMutableMap()
    }
}