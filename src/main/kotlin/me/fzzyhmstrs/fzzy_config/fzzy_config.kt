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

import me.fzzyhmstrs.fzzy_config.networking.NetworkEvents
import me.fzzyhmstrs.fzzy_config.networking.NetworkEventsClient
import me.fzzyhmstrs.fzzy_config.util.PlatformUtils
import net.minecraft.util.Identifier
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.fml.common.Mod
import org.jetbrains.annotations.ApiStatus.Internal
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.function.Consumer

inline fun<reified T> Any?.cast(): T {
    return this as T
}

inline fun<reified T> Any?.nullCast(): T? {
    return this as? T
}

internal fun String.fcId(): Identifier {
    return Identifier(FC.MOD_ID, this)
}



@Mod(value = "fzzy_config")
class FzzyConfigNeoForge(bus: IEventBus) {
    init {
        MinecraftForge.EVENT_BUS.addListener(NetworkEvents::registerDataSync)
        NetworkEvents.registerPayloads()
        bus.addListener(NetworkEvents::registerConfigurations)
        PlatformUtils.registerCommands(bus)
    }
}


@Internal
object FC {
    internal const val MOD_ID = "fzzy_config"
    internal val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)

}

@Mod(value = "fzzy_config")
class FzzyConfigNeoForgeClient(bus: IEventBus, dist: Dist) {
    init {
        if (dist == Dist.CLIENT)
            NetworkEventsClient.registerClient()
    }
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

    fun withRestart(consumer: Consumer<Boolean>) {
        consumer.accept(openRestartScreen)
        openRestartScreen = false

    }
}