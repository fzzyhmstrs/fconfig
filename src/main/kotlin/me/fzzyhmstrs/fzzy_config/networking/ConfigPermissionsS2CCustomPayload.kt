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
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type

class ConfigPermissionsS2CCustomPayload(val id: String, val permissions: MutableMap<String, Boolean>): CustomPacketPayload {

    override fun type(): Type<out CustomPacketPayload> {
        return type
    }

    companion object {
        val type: Type<ConfigPermissionsS2CCustomPayload> = Type("config_perms_s2c".fcId())
        val codec: StreamCodec<ByteBuf, ConfigPermissionsS2CCustomPayload> = StreamCodec.composite(
            PortingUtils.Codecs.STRING,
            ConfigPermissionsS2CCustomPayload::id,
            ByteBufCodecs.map({ hashMapOf() }, PortingUtils.Codecs.STRING, PortingUtils.Codecs.BOOL),
            ConfigPermissionsS2CCustomPayload::permissions,
            ::ConfigPermissionsS2CCustomPayload
        )
    }

}