/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config

import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.networking.NetworkEvents
import me.fzzyhmstrs.fzzy_config.networking.NetworkEventsClient
import me.fzzyhmstrs.fzzy_config.util.platform.impl.PlatformUtils
import net.minecraft.util.Identifier
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
import net.minecraftforge.network.NetworkEvent
import org.jetbrains.annotations.ApiStatus.Internal
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.function.Consumer
import java.util.function.Function

inline fun<reified T> Any?.cast(): T {
    return this as T
}

inline fun<reified T> Any?.nullCast(): T? {
    return this as? T
}

internal fun String.fcId(): Identifier {
    return Identifier(FC.MOD_ID, this)
}

internal fun String.simpleId(): Identifier {
    return Identifier(this)
}

internal fun String.nsId(path: String): Identifier {
    return Identifier(this, path)
}



@Mod(value = "fzzy_config")
class FzzyConfigForge() {
    init {
        MinecraftForge.EVENT_BUS.addListener(NetworkEvents::registerDataSync)
        NetworkEvents.registerPayloads()
        PlatformUtils.registerCommands(FMLJavaModLoadingContext.get().modEventBus)

        if (PlatformUtils.isClient())
            NetworkEventsClient.registerClient()
    }

    fun registerLoginPayloads(event: NetworkEvent.GatherLoginPayloadsEvent) {
        NetworkEvents.registerConfigurations(event)
    }
}



@Internal
object FC {
    internal const val MOD_ID = "fzzy_config"
    internal val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)
    internal val DEVLOG: Logger = ConfigApi.platform().devLogger(MOD_ID)

}


@Internal
object FCC {

    private var scopeToOpen = ""
    private var openRestartScreen = false

    fun openScopedScreen(scope: String) {
        this.scopeToOpen = scope
    }

    fun withScope(consumer: Consumer<String>) {
        consumer.accept(scopeToOpen)
        scopeToOpen = ""
    }

    fun openRestartScreen() {
        this.openRestartScreen = true
    }

    fun withRestart(function: Function<Boolean, Boolean>) {
        openRestartScreen = function.apply(openRestartScreen)
    }
}