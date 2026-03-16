/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.networking

import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type

internal class ConfigSyncS2CCustomPayload(val id: String, val serializedConfig: String): CustomPacketPayload {

    constructor(buf: FriendlyByteBuf): this(buf.readUtf(), buf.readUtf())

    fun write(buf: FriendlyByteBuf) {
        buf.writeUtf(id)
        buf.writeUtf(serializedConfig, ConfigApiImpl.MAX_CONFIG_SERIALIZATION_LENGTH)
    }

    override fun type(): Type<out CustomPacketPayload> {
        return type
    }

    companion object {
        val type: Type<ConfigSyncS2CCustomPayload> = Type("config_sync_s2c".fcId())
        val codec: StreamCodec<FriendlyByteBuf, ConfigSyncS2CCustomPayload> = CustomPacketPayload.codec({ c, b -> c.write(b) }, { b -> ConfigSyncS2CCustomPayload(b)})
    }
}