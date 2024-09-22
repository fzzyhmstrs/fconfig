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

import io.netty.buffer.Unpooled
import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.cast
import me.fzzyhmstrs.fzzy_config.networking.api.ServerPlayNetworkContext
import me.fzzyhmstrs.fzzy_config.registry.SyncedConfigRegistry
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier
import net.minecraftforge.event.OnDatapackSyncEvent
import net.minecraftforge.network.NetworkDirection
import net.minecraftforge.network.NetworkEvent
import net.minecraftforge.network.NetworkRegistry
import net.minecraftforge.network.PacketDistributor
import java.util.function.Supplier


internal object NetworkEvents {

    @Suppress("UNUSED_PARAMETER")
    fun canSend(playerEntity: ServerPlayerEntity, id: Identifier): Boolean {
        return playerEntity.networkHandler.isConnectionOpen
    }

    fun send(playerEntity: ServerPlayerEntity, packet: Packet<*>) {
        PacketDistributor.PLAYER.with { playerEntity }.send(packet)
    }

    private fun handleUpdate(payload: ConfigUpdateC2SCustomPayload, context: Supplier<NetworkEvent.Context>) {
        val serverPlayer = context.get().sender
        if (serverPlayer == null) {
            context.get().packetHandled = true
            return
        }
        SyncedConfigRegistry.receiveConfigUpdate(
            payload.updates,
            serverPlayer.server,
            serverPlayer,
            payload.changeHistory,
            { player, id -> canSend(player, id) },
            { player, pl ->
                val buf = PacketByteBuf(Unpooled.buffer())
                pl.write(buf)
                send(player, CustomPayloadS2CPacket(pl.getId(), buf))
            }
        )
        context.get().packetHandled = true
    }

    private fun handleSettingForwardBidirectional(payload: SettingForwardCustomPayload, context: Supplier<NetworkEvent.Context>) {
        if (context.get().direction == NetworkDirection.PLAY_TO_CLIENT) {
            NetworkEventsClient.handleSettingForward(payload, context)
        } else {
            this.handleSettingForward(payload, context)
        }
        context.get().packetHandled = true
    }

    private fun handleSettingForward(payload: SettingForwardCustomPayload, context: Supplier<NetworkEvent.Context>) {
        val serverPlayer = context.get().sender
        if (serverPlayer == null) {
            context.get().packetHandled = true
            return
        }
        SyncedConfigRegistry.receiveSettingForward(
            payload.player,
            serverPlayer,
            payload.scope,
            payload.update,
            payload.summary,
            { player, id -> canSend(player, id) },
            { player, pl ->
                val buf = PacketByteBuf(Unpooled.buffer())
                pl.write(buf)
                send(player, CustomPayloadS2CPacket(pl.getId(), buf))
            }
        )
        context.get().packetHandled = true
    }

    private fun receiveUpdate(payload: ConfigUpdateC2SCustomPayload, context: ServerPlayNetworkContext) {
        SyncedConfigRegistry.receiveConfigUpdate(
            payload.updates,
            context.player().server,
            context.player(),
            payload.changeHistory,
            { _, id -> context.canReply(id) },
            { _, pl -> context.reply(pl) }
        )
    }

    private fun receiveForward(payload: SettingForwardCustomPayload, context: ServerPlayNetworkContext) {
        SyncedConfigRegistry.receiveSettingForward(
            payload.player,
            context.player(),
            payload.scope,
            payload.update,
            payload.summary,
            { _, id -> context.canReply(id) },
            { _, pl -> context.reply(pl) }
        )
    }

    fun registerDataSync(event: OnDatapackSyncEvent) {
        val serverPlayer = event.player
        if (serverPlayer == null) {
            SyncedConfigRegistry.onEndDataReload(
                event.players,
                { player, id -> ConfigApi.network().canSend(id, player) },
                { player, payload -> ConfigApi.network().send(payload, player) }
            )
            ConfigApiImpl.invalidateLookup()
        } else {
            SyncedConfigRegistry.onJoin(
                serverPlayer,
                serverPlayer.server,
                { player, id -> ConfigApi.network().canSend(id, player) },
                { player, payload -> ConfigApi.network().send(payload, player) }
            )
        }
    }

    fun registerConfigurations(event: NetworkEvent.GatherLoginPayloadsEvent) {
        SyncedConfigRegistry.onConfigure(
            { _ -> true },
            { payload ->
                val buf = PacketByteBuf(Unpooled.buffer())
                payload.write(buf)
                event.add(buf, ConfigSyncS2CCustomPayload.id, "Fzzy Config login config sync", false)
            }
        )
    }


    fun registerPayloads() {

        ConfigApi.network().registerS2C(ConfigPermissionsS2CCustomPayload.id, ConfigPermissionsS2CCustomPayload::class.java,
            ::ConfigPermissionsS2CCustomPayload, NetworkEventsClient::receivePerms)
        //PayloadTypeRegistry.playC2S().register(ConfigSyncS2CCustomPayload.type, ConfigSyncS2CCustomPayload.codec)
        //PayloadTypeRegistry.playS2C().register(ConfigSyncS2CCustomPayload.type, ConfigSyncS2CCustomPayload.codec)
        ConfigApi.network().registerS2C(ConfigSyncS2CCustomPayload.id, ConfigSyncS2CCustomPayload::class.java,
            ::ConfigSyncS2CCustomPayload, NetworkEventsClient::receiveSync)
        //PayloadTypeRegistry.playC2S().register(ConfigUpdateS2CCustomPayload.type, ConfigUpdateS2CCustomPayload.codec)
        //PayloadTypeRegistry.playS2C().register(ConfigUpdateS2CCustomPayload.type, ConfigUpdateS2CCustomPayload.codec)
        ConfigApi.network().registerS2C(ConfigUpdateS2CCustomPayload.id, ConfigUpdateS2CCustomPayload::class.java,
            ::ConfigUpdateS2CCustomPayload, NetworkEventsClient::receiveUpdate)
        //PayloadTypeRegistry.playC2S().register(ConfigUpdateC2SCustomPayload.type, ConfigUpdateC2SCustomPayload.codec)
        ConfigApi.network().registerC2S(ConfigUpdateC2SCustomPayload.id, ConfigUpdateC2SCustomPayload::class.java,
            ::ConfigUpdateC2SCustomPayload, this::receiveUpdate)
        //PayloadTypeRegistry.playS2C().register(ConfigUpdateC2SCustomPayload.type, ConfigUpdateC2SCustomPayload.codec)
        //PayloadTypeRegistry.playC2S().register(SettingForwardCustomPayload.type, SettingForwardCustomPayload.codec)
        ConfigApi.network().registerC2S(SettingForwardCustomPayload.id, SettingForwardCustomPayload::class.java,
            ::SettingForwardCustomPayload, this::receiveForward)
        //PayloadTypeRegistry.playS2C().register(SettingForwardCustomPayload.type, SettingForwardCustomPayload.codec)
        ConfigApi.network().registerS2C(SettingForwardCustomPayload.id, SettingForwardCustomPayload::class.java,
            ::SettingForwardCustomPayload, NetworkEventsClient::receiveForward)
    }
}