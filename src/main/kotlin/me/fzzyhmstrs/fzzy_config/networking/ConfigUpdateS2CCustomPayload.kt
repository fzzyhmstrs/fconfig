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

internal class ConfigUpdateS2CCustomPayload(val updates: Map<String, String>): CustomPacketPayload {

    constructor(buf: FriendlyByteBuf): this(readMap(buf))

    fun write(buf: FriendlyByteBuf) {
        writeMap(buf)
    }
    private fun writeMap(buf: FriendlyByteBuf) {
        buf.writeVarInt(updates.size)
        for ((id, serializedConfig) in updates) {
            buf.writeUtf(id)
            buf.writeUtf(serializedConfig, ConfigApiImpl.MAX_CONFIG_SERIALIZATION_LENGTH)
        }
    }

    override fun type(): Type<out CustomPacketPayload> {
        return type
    }

    companion object {
        val type: Type<ConfigUpdateS2CCustomPayload> = Type("config_update_s2c".fcId())
        val codec: StreamCodec<FriendlyByteBuf, ConfigUpdateS2CCustomPayload> = CustomPacketPayload.codec({ c, b -> c.write(b) }, { b -> ConfigUpdateS2CCustomPayload(b)})

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