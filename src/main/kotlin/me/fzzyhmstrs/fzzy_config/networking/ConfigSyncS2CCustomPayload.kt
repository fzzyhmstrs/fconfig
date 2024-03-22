package me.fzzyhmstrs.fzzy_config.networking

import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.network.packet.CustomPayload.Id

class ConfigSyncS2CCustomPayload(val id: String, val serializedConfig: String): CustomPayload {

    constructor(buf: PacketByteBuf): this(buf.readString(),buf.readString())

    fun write(buf: PacketByteBuf){
        buf.writeString(id)
        buf.writeString(serializedConfig)
    }

    override fun getId(): Id<out CustomPayload> {
        return type
    }

    companion object{
        val type: Id<ConfigSyncS2CCustomPayload> = CustomPayload.id("fzzy_config:config_sync_s2c")
        val codec: PacketCodec<PacketByteBuf, ConfigSyncS2CCustomPayload> = CustomPayload.codecOf({ c, b -> c.write(b) }, { b -> ConfigSyncS2CCustomPayload(b)})
    }
}