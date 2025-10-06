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
import net.minecraft.util.Identifier

internal class ConfigUpdateC2SCustomPayload(val updates: Map<String, String>, val changeHistory: List<String>, val playerPerm: Int): FzzyPayload {

    constructor(buf: PacketByteBuf): this(readMap(buf), readList(buf), buf.readByte().toInt())

    override fun write(buf: PacketByteBuf) {
        writeMap(buf)
        writeList(buf)
        buf.writeByte(playerPerm)
    }

    override fun getId(): Identifier {
        return Companion.id
    }

    private fun writeMap(buf: PacketByteBuf) {
        buf.writeVarInt(updates.size)
        for ((id, serializedConfig) in updates) {
            buf.writeString(id)
            buf.writeString(serializedConfig, ConfigApiImpl.MAX_CONFIG_SERIALIZATION_LENGTH)
        }
    }
    private fun writeList(buf: PacketByteBuf) {
        buf.writeVarInt(changeHistory.size)
        for (str in changeHistory) {
            buf.writeString(str, ConfigApiImpl.MAX_CONFIG_SERIALIZATION_LENGTH)
        }
    }

    companion object {
        val id = "config_update_c2s".fcId()

        private fun readList(buf: PacketByteBuf): List<String> {
            val size = buf.readVarInt()
            val list: MutableList<String> = mutableListOf()
            for (i in 1..size) {
                list.add(buf.readString())
            }
            return list
        }
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