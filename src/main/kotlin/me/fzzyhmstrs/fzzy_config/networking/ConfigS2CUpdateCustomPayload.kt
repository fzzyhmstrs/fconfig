package me.fzzyhmstrs.fzzy_config.networking

import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.network.packet.CustomPayload.Id

class ConfigS2CUpdateCustomPayload(val id: String, val serializedConfig: String): CustomPayload {

    constructor(buf: PacketByteBuf): this(buf.readString(),buf.readString())

    fun write(buf: PacketByteBuf){
        buf.writeString(id)
        buf.writeString(serializedConfig)
    }

    override fun getId(): CustomPayload.Id<out CustomPayload> {
        return type
    }

    companion object{
        val type: Id<ConfigS2CSyncCustomPayload> = CustomPayload.id("fzzy_config:config_s2c_update")
        val codec: PacketCodec<PacketByteBuf, ConfigS2CUpdateCustomPayload> = CustomPayload.codecOf({c, b -> c.write(b) }, {b -> ConfigS2CUpdateCustomPayload(b)})
    }
}
