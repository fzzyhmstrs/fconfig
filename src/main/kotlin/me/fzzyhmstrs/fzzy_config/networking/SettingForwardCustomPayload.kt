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
import java.util.*

internal class SettingForwardCustomPayload(val update: String, val player: UUID, val scope: String, val summary: String): CustomPayload {

    constructor(buf: PacketByteBuf): this(buf.readString(), buf.readUuid(), buf.readString(), buf.readString())

    fun write(buf: PacketByteBuf) {
        buf.writeString(update)
        buf.writeUuid(player)
        buf.writeString(scope)
        buf.writeString(summary)
    }

    override fun getId(): Id<out CustomPayload> {
        return type
    }

    companion object {
        val type: Id<SettingForwardCustomPayload> = Id(Identifier.of("fzzy_config:setting_forward"))
        val codec: PacketCodec<PacketByteBuf, SettingForwardCustomPayload> = CustomPayload.codecOf({ c, b -> c.write(b) }, { b -> SettingForwardCustomPayload(b)})

    }
}