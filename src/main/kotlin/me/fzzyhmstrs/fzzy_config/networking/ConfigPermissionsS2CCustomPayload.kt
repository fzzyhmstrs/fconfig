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
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.codec.PacketCodecs
import net.minecraft.network.packet.CustomPayload
import net.minecraft.network.packet.CustomPayload.Id
import net.minecraft.util.Identifier

class ConfigPermissionsS2CCustomPayload(private val permissions: MutableMap<String, Boolean>): CustomPayload {

    override fun getId(): CustomPayload.Id<out CustomPayload> {
        return type
    }

    companion object {
        val type: Id<ConfigPermissionsS2CCustomPayload> = Id(Identifier.of("fzzy_config:config_perms_s2c"))
        val codec: PacketCodec<ByteBuf, ConfigPermissionsS2CCustomPayload> = PacketCodecs.map({ mutableMapOf() }, PacketCodecs.STRING, PacketCodecs.BOOL).xmap(
            { m -> ConfigPermissionsS2CCustomPayload(m) },
            { p -> p.permissions }
        )
    }

}