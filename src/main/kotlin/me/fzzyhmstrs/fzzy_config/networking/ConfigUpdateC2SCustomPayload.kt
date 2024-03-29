package me.fzzyhmstrs.fzzy_config.networking

import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.network.packet.CustomPayload.Id

class ConfigUpdateC2SCustomPayload(val updates: Map<String, String>, val changeHistory: List<String>, val playerPerm: Int): CustomPayload {

    constructor(buf: PacketByteBuf): this(readMap(buf),readList(buf), buf.readByte().toInt())

    fun write(buf: PacketByteBuf){
        writeMap(buf)
        writeList(buf)
        buf.writeByte(playerPerm)
    }

    private fun writeMap(buf: PacketByteBuf) {
        buf.writeVarInt(updates.size)
        for ((id, serializedConfig) in updates) {
            buf.writeString(id)
            buf.writeString(serializedConfig)
        }
    }
    private fun writeList(buf: PacketByteBuf) {
        buf.writeVarInt(changeHistory.size)
        for (str in changeHistory) {
            buf.writeString(str)
        }
    }

    override fun getId(): Id<out CustomPayload> {
        return type
    }

    companion object{
        val type: Id<ConfigUpdateC2SCustomPayload> = CustomPayload.id("fzzy_config:config_update_c2s")
        val codec: PacketCodec<PacketByteBuf, ConfigUpdateC2SCustomPayload> = CustomPayload.codecOf({ c, b -> c.write(b) }, { b -> ConfigUpdateC2SCustomPayload(b)})
        private fun readList(buf: PacketByteBuf): List<String>{
            val size = buf.readVarInt()
            val list: MutableList<String> = mutableListOf()
            for (i in 1..size){
                list.add(buf.readString())
            }
            return list
        }
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