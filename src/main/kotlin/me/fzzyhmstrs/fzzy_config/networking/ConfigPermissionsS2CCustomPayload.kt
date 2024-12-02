/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.networking

import io.netty.buffer.ByteBuf
import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.util.PortingUtils
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.codec.PacketCodecs
import net.minecraft.network.packet.CustomPayload
import net.minecraft.network.packet.CustomPayload.Id

class ConfigPermissionsS2CCustomPayload(val id: String, val permissions: MutableMap<String, Boolean>): CustomPayload {

    override fun getId(): Id<out CustomPayload> {
        return type
    }

    companion object {
        val type: Id<ConfigPermissionsS2CCustomPayload> = Id("config_perms_s2c".fcId())
        val codec: PacketCodec<ByteBuf, ConfigPermissionsS2CCustomPayload> = PacketCodec.tuple(
            PortingUtils.Codecs.STRING,
            ConfigPermissionsS2CCustomPayload::id,
            PacketCodecs.map({ mutableMapOf() }, PortingUtils.Codecs.STRING, PortingUtils.Codecs.BOOL),
            ConfigPermissionsS2CCustomPayload::permissions,
            ::ConfigPermissionsS2CCustomPayload
        )
    }

}