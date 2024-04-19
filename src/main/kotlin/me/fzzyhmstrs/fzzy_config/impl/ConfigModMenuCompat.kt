/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

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