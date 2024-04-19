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

import me.fzzyhmstrs.fzzy_config.registry.ClientConfigRegistry
import net.fabricmc.loader.api.ModContainer
import net.minecraft.client.gui.screen.Screen
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.function.BiFunction

@Internal
object ConfigCatalogueCompat {
    @JvmStatic
    fun createConfigFactoryProvider(): Map<String, BiFunction<Screen, ModContainer, Screen?>> {
        return ClientConfigRegistry.getScreenScopes().associateWith { scope -> BiFunction { _: Screen,_: ModContainer -> ClientConfigRegistry.provideScreen(scope) } }
    }
}