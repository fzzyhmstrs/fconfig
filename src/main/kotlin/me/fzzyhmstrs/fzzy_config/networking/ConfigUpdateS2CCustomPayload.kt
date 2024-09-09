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

import me.fzzyhmstrs.fzzy_config.networking.ConfigPermissionsS2CCustomPayload.Companion
import net.minecraft.network.PacketByteBuf
import net.minecraft.util.Identifier

internal class ConfigUpdateS2CCustomPayload(val updates: Map<String, String>): FzzyPayload {

    constructor(buf: PacketByteBuf): this(readMap(buf))

    override fun write(buf: PacketByteBuf) {
        writeMap(buf)
    }

    override fun id(): Identifier {
        return Companion.id
    }

    override fun getId(): Identifier {
        return Companion.id
    }

    private fun writeMap(buf: PacketByteBuf) {
        buf.writeVarInt(updates.size)
        for ((id, serializedConfig) in updates) {
            buf.writeString(id)
            buf.writeString(serializedConfig)
        }
    }

    companion object {

        val id = Identifier("fzzy_config:config_update_s2c")

        private fun readMap(buf: PacketByteBuf): Map<String, String> {
            val size = buf.readVarInt()
            val map: MutableMap<String, String> = mutableMapOf()
            for (i in 1..size) {
                map[buf.readString()] = buf.readString()
            }
            return map
        }
    }
}