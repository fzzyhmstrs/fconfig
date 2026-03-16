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

import net.minecraft.world.entity.player.Player
import net.minecraft.network.ConnectionProtocol
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

interface NetworkContext<T: Player> {
    fun execute(runnable: Runnable)
    fun disconnect(reason: Component)
    fun canReply(id: Identifier): Boolean
    fun reply(payload: CustomPacketPayload)
    fun player(): T
    fun networkPhase(): ConnectionProtocol
    fun networkSide(): PacketFlow
}