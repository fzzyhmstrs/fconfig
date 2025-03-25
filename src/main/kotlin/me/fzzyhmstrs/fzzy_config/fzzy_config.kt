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
import me.fzzyhmstrs.fzzy_config.networking.impl.NetworkApiImpl
import me.fzzyhmstrs.fzzy_config.util.platform.impl.PlatformUtils
import net.minecraft.util.Identifier
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.common.NeoForge
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
    return Identifier.of(FC.MOD_ID, this)
}

internal fun String.simpleId(): Identifier {
    return Identifier.of(this)
}

internal fun String.nsId(path: String): Identifier {
    return Identifier.of(this, path)
}



@Mod(value = "fzzy_config")
class FzzyConfigNeoForge(bus: IEventBus) {
    init {
        NeoForge.EVENT_BUS.addListener(NetworkEvents::registerDataSync)
        bus.addListener(NetworkEvents::registerPayloads)
        bus.addListener(NetworkApiImpl::onRegister)
        bus.addListener(NetworkEvents::registerConfigurations)
        PlatformUtils.registerCommands(bus)
    }
}


@Internal
object FC {
    internal const val MOD_ID = "fzzy_config"
    internal val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)
    internal val DEVLOG: Logger = ConfigApi.platform().devLogger(MOD_ID)

}

@Mod(value = "fzzy_config", dist = [Dist.CLIENT])
class FzzyConfigNeoForgeClient() {
    init {
        NetworkEventsClient.registerClient()
    }
}


@Internal
object FCC {

    @Volatile
    private var scopeToOpen = ""
    @Volatile
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