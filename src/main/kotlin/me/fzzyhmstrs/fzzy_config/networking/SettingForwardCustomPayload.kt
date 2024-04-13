package me.fzzyhmstrs.fzzy_config.networking

import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.network.packet.CustomPayload.Id
import java.util.*

internal class SettingForwardCustomPayload(val update: String, val player: UUID, val scope: String, val summary: String): CustomPayload {

    constructor(buf: PacketByteBuf): this(buf.readString(), buf.readUuid(), buf.readString(), buf.readString())

    fun write(buf: PacketByteBuf){
        buf.writeString(update)
        buf.writeUuid(player)
        buf.writeString(scope)
        buf.writeString(summary)
    }

    override fun getId(): Id<out CustomPayload> {
        return type
    }

    companion object{
        val type: Id<SettingForwardCustomPayload> = CustomPayload.id("fzzy_config:setting_forward")
        val codec: PacketCodec<PacketByteBuf, SettingForwardCustomPayload> = CustomPayload.codecOf({ c, b -> c.write(b) }, { b -> SettingForwardCustomPayload(b)})

    }
}