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

import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.network.packet.CustomPayload.Id
import net.minecraft.util.Identifier

internal class ConfigSyncS2CCustomPayload(val id: String, val serializedConfig: String): CustomPayload {

    constructor(buf: PacketByteBuf): this(buf.readString(),buf.readString())

    fun write(buf: PacketByteBuf){
        buf.writeString(id)
        buf.writeString(serializedConfig)
    }

    override fun getId(): Id<out CustomPayload> {
        return type
    }

    companion object{
        val type: Id<ConfigSyncS2CCustomPayload> = Id(Identifier.of("fzzy_config:config_sync_s2c"))
        val codec: PacketCodec<PacketByteBuf, ConfigSyncS2CCustomPayload> = CustomPayload.codecOf({ c, b -> c.write(b) }, { b -> ConfigSyncS2CCustomPayload(b)})
    }
}