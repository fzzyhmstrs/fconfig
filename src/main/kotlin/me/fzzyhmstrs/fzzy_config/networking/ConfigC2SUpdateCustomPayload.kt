package me.fzzyhmstrs.fzzy_config.networking

import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.network.packet.CustomPayload.Id

class ConfigC2SUpdateCustomPayload(val id: String, val serializedConfig: String, val changeHistory: List<String>): CustomPayload {

    constructor(buf: PacketByteBuf): this(buf.readString(),buf.readString(),readList(buf))

    fun write(buf: PacketByteBuf){
        buf.writeString(id)
        buf.writeString(serializedConfig)
        writeList(buf)
    }

    private fun writeList(buf: PacketByteBuf) {
        buf.writeVarInt(changeHistory.size)
        for (str in changeHistory) {
            buf.writeString(str)
        }
    }

    override fun getId(): CustomPayload.Id<out CustomPayload> {
        return type
    }

    companion object{
        val type: Id<ConfigC2SUpdateCustomPayload> = CustomPayload.id("fzzy_config:config_c2s_update")
        val codec: PacketCodec<PacketByteBuf, ConfigC2SUpdateCustomPayload> = CustomPayload.codecOf({c, b -> c.write(b) }, {b -> ConfigC2SUpdateCustomPayload(b)})
        private fun readList(buf: PacketByteBuf): List<String>{
            val size = buf.readVarInt()
            val list: MutableList<String> = mutableListOf()
            for (i in 1..size){
                list.add(buf.readString())
            }
            return list
        }
    }
}