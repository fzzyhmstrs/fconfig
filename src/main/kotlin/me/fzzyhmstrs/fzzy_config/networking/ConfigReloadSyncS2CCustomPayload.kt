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

import net.minecraft.network.PacketByteBuf
import net.minecraft.util.Identifier

internal class ConfigReloadSyncS2CCustomPayload(val id: String, val serializedConfig: String): FzzyPayload {

    constructor(buf: PacketByteBuf): this(buf.readString(), buf.readString())

    override fun write(buf: PacketByteBuf) {
        buf.writeString(id)
        buf.writeString(serializedConfig)
    }

    override fun getId(): Identifier {
        return Companion.id
    }

    companion object {
        val id = Identifier("fzzy_config:config_reload_sync_s2c")
    }
}