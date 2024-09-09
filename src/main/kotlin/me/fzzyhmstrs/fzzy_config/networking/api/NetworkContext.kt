/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.networking.api

import me.fzzyhmstrs.fzzy_config.networking.FzzyPayload
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.NetworkSide
import net.minecraft.text.Text
import net.minecraft.util.Identifier

interface NetworkContext<T: PlayerEntity> {
    fun execute(runnable: Runnable)
    fun disconnect(reason: Text)
    fun canReply(id: Identifier): Boolean
    fun reply(payload: FzzyPayload)
    fun player(): T
    fun networkSide(): NetworkSide
}