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
import net.minecraft.resources.Identifier

internal class DynamicIdsS2CCustomPayload(val key: Identifier, val ids: List<Identifier>): CustomPacketPayload {

    constructor(buf: FriendlyByteBuf): this(buf.readIdentifier(), readList(buf))

    fun write(buf: FriendlyByteBuf) {
        buf.writeIdentifier(key)
        buf.writeVarInt(ids.size)
        for (id in ids) {
            buf.writeIdentifier(id)
        }
    }

    override fun type(): Type<out CustomPacketPayload> {
        return type
    }

    companion object {

        private fun readList(buf: FriendlyByteBuf): List<Identifier> {
            val size = buf.readVarInt()
            val list: MutableList<Identifier> = mutableListOf()
            for (i in 1..size) {
                list.add(buf.readIdentifier())
            }
            return list
        }

        val type: Type<DynamicIdsS2CCustomPayload> = Type("dynamic_id_s2c".fcId())
        val codec: StreamCodec<FriendlyByteBuf, DynamicIdsS2CCustomPayload> = CustomPacketPayload.codec({ c, b -> c.write(b) }, { b -> DynamicIdsS2CCustomPayload(b)})
    }
}