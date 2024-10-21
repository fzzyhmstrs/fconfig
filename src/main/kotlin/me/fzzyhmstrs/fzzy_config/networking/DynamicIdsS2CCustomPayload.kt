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
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.network.packet.CustomPayload.Id
import net.minecraft.util.Identifier

internal class DynamicIdsS2CCustomPayload(val key: Identifier, val ids: List<Identifier>): CustomPayload {

    constructor(buf: PacketByteBuf): this(buf.readIdentifier(), readList(buf))

    fun write(buf: PacketByteBuf) {
        buf.writeIdentifier(key)
        buf.writeVarInt(ids.size)
        for (id in ids) {
            buf.writeIdentifier(id)
        }
    }

    override fun getId(): Id<out CustomPayload> {
        return type
    }

    companion object {

        private fun readList(buf: PacketByteBuf): List<Identifier> {
            val size = buf.readVarInt()
            val list: MutableList<Identifier> = mutableListOf()
            for (i in 1..size) {
                list.add(buf.readIdentifier())
            }
            return list
        }

        val type: Id<DynamicIdsS2CCustomPayload> = Id("dynamic_id_s2c".fcId())
        val codec: PacketCodec<PacketByteBuf, DynamicIdsS2CCustomPayload> = CustomPayload.codecOf({ c, b -> c.write(b) }, { b -> DynamicIdsS2CCustomPayload(b)})
    }
}