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
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type
import java.util.*

internal class SettingForwardCustomPayload(val update: String, val player: UUID, val scope: String, val summary: String): CustomPacketPayload {

    constructor(buf: FriendlyByteBuf): this(buf.readUtf(), buf.readUUID(), buf.readUtf(), buf.readUtf())

    fun write(buf: FriendlyByteBuf) {
        buf.writeUtf(update)
        buf.writeUUID(player)
        buf.writeUtf(scope)
        buf.writeUtf(summary)
    }

    override fun type(): Type<out CustomPacketPayload> {
        return type
    }

    companion object {
        val type: Type<SettingForwardCustomPayload> = Type("setting_forward".fcId())
        val codec: StreamCodec<FriendlyByteBuf, SettingForwardCustomPayload> = CustomPacketPayload.codec({ c, b -> c.write(b) }, { b -> SettingForwardCustomPayload(b)})

    }
}