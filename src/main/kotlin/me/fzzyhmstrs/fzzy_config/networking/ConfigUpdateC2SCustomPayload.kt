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

internal class ConfigUpdateC2SCustomPayload(val updates: Map<String, String>, val changeHistory: List<String>, val playerPerm: Int):
    CustomPacketPayload {

    constructor(buf: FriendlyByteBuf): this(readMap(buf), readList(buf), buf.readByte().toInt())

    fun write(buf: FriendlyByteBuf) {
        writeMap(buf)
        writeList(buf)
        buf.writeByte(playerPerm)
    }

    private fun writeMap(buf: FriendlyByteBuf) {
        buf.writeVarInt(updates.size)
        for ((id, serializedConfig) in updates) {
            buf.writeUtf(id)
            buf.writeUtf(serializedConfig, ConfigApiImpl.MAX_CONFIG_SERIALIZATION_LENGTH)
        }
    }
    private fun writeList(buf: FriendlyByteBuf) {
        buf.writeVarInt(changeHistory.size)
        for (str in changeHistory) {
            buf.writeUtf(str, ConfigApiImpl.MAX_CONFIG_SERIALIZATION_LENGTH)
        }
    }

    override fun type(): Type<out CustomPacketPayload> {
        return type
    }

    companion object {
        val type: Type<ConfigUpdateC2SCustomPayload> = Type("config_update_c2s".fcId())
        val codec: StreamCodec<FriendlyByteBuf, ConfigUpdateC2SCustomPayload> = CustomPacketPayload.codec({ c, b -> c.write(b) }, { b -> ConfigUpdateC2SCustomPayload(b)})
        private fun readList(buf: FriendlyByteBuf): List<String> {
            val size = buf.readVarInt()
            val list: MutableList<String> = mutableListOf()
            for (i in 1..size) {
                list.add(buf.readUtf())
            }
            return list
        }
        private fun readMap(buf: FriendlyByteBuf): Map<String, String> {
            val size = buf.readVarInt()
            val map: MutableMap<String, String> = hashMapOf()
            for (i in 1..size) {
                map[buf.readUtf()] = buf.readUtf()
            }
            return map
        }
    }
}