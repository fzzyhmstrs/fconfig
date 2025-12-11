/*
 * Copyright (c) 2025 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.event.api

import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity

class ServerUpdateContext(private val server: MinecraftServer, private val player: ServerPlayerEntity?) {
    fun getServer(): MinecraftServer {
        return server
    }

    fun getPlayer(): ServerPlayerEntity? {
        return player
    }
}