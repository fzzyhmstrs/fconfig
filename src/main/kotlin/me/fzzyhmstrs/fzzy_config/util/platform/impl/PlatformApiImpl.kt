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

import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.util.PlatformApi
import me.fzzyhmstrs.fzzy_config.util.platform.Registrar
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.Version
import net.minecraft.registry.Registry
import org.slf4j.Logger
import java.io.File
import java.util.*

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

    override fun devLogger(name: String): Logger {
        return DevLogger(name)
    }

    override fun <T> createRegistrar(namespace: String, registry: Registry<T>): Registrar<T> {
        return RegistrarImpl(namespace, registry)
    }

    override fun testVersion(id: String, version: String): Optional<Int> {
        return try {
            FabricLoader.getInstance().getModContainer(id).map { container ->
                val v = Version.parse(version)
                container.metadata.version.compareTo(v)
            }
        } catch (e: Throwable) {
            FC.DEVLOG.error("Critical error encountered parsing version in PlatformApi", e)
            Optional.empty()
        }
    }
}