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
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.network.packet.CustomPayload.Id

internal class ConfigUpdateS2CCustomPayload(val updates: Map<String, String>): CustomPayload {

    constructor(buf: PacketByteBuf): this(readMap(buf))

    fun write(buf: PacketByteBuf) {
        writeMap(buf)
    }
    private fun writeMap(buf: PacketByteBuf) {
        buf.writeVarInt(updates.size)
        for ((id, serializedConfig) in updates) {
            buf.writeString(id)
            buf.writeString(serializedConfig, ConfigApiImpl.MAX_CONFIG_SERIALIZATION_LENGTH)
        }
    }

    override fun getId(): Id<out CustomPayload> {
        return type
    }

    companion object {
        val type: Id<ConfigUpdateS2CCustomPayload> = Id("config_update_s2c".fcId())
        val codec: PacketCodec<PacketByteBuf, ConfigUpdateS2CCustomPayload> = CustomPayload.codecOf({ c, b -> c.write(b) }, { b -> ConfigUpdateS2CCustomPayload(b)})

        private fun readMap(buf: PacketByteBuf): Map<String, String> {
            val size = buf.readVarInt()
            val map: MutableMap<String, String> = hashMapOf()
            for (i in 1..size) {
                map[buf.readString()] = buf.readString()
            }
            return map
        }
    }
}