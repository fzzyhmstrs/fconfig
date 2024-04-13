package me.fzzyhmstrs.fzzy_config.networking

import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.network.packet.CustomPayload.Id

internal class ConfigUpdateS2CCustomPayload(val updates: Map<String, String>): CustomPayload {

    constructor(buf: PacketByteBuf): this(readMap(buf))

    fun write(buf: PacketByteBuf){
        writeMap(buf)
    }
    private fun writeMap(buf: PacketByteBuf) {
        buf.writeVarInt(updates.size)
        for ((id, serializedConfig) in updates) {
            buf.writeString(id)
            buf.writeString(serializedConfig)
        }
    }

    override fun getId(): Id<out CustomPayload> {
        return type
    }

    companion object{
        val type: Id<ConfigUpdateS2CCustomPayload> = CustomPayload.id("fzzy_config:config_update_s2c")
        val codec: PacketCodec<PacketByteBuf, ConfigUpdateS2CCustomPayload> = CustomPayload.codecOf({ c, b -> c.write(b) }, { b -> ConfigUpdateS2CCustomPayload(b)})

        private fun readMap(buf: PacketByteBuf): Map<String,String>{
            val size = buf.readVarInt()
            val map: MutableMap<String, String> = mutableMapOf()
            for (i in 1..size){
                map[buf.readString()] = buf.readString()
            }
            return map
        }
    }
}