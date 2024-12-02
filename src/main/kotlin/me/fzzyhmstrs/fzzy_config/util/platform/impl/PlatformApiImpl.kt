/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.util.platform.impl

import me.fzzyhmstrs.fzzy_config.util.PlatformApi
import me.fzzyhmstrs.fzzy_config.util.platform.Registrar
import net.minecraft.registry.Registry
import java.io.File

internal object PlatformApiImpl: PlatformApi {

    override fun isClient(): Boolean {
        return PlatformUtils.isClient()
    }

    override fun configDir(): File {
        return PlatformUtils.configDir()
    }

    override fun gameDir(): File {
        return PlatformUtils.gameDir()
    }

    override fun isModLoaded(mod: String): Boolean {
        return PlatformUtils.isModLoaded(mod)
    }

    override fun isDev(): Boolean {
        return PlatformUtils.isDev()
    }

    override fun <T> createRegistrar(namespace: String, registry: Registry<T>): Registrar<T> {
        return RegistrarImpl(namespace, registry)
    }
}