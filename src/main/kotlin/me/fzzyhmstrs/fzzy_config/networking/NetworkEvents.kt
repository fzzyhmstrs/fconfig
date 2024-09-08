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
import me.fzzyhmstrs.fzzy_config.cast
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

    fun registerDataSync(event: OnDatapackSyncEvent) {
        val serverPlayer = event.player
        if (serverPlayer == null) {
            SyncedConfigRegistry.onEndDataReload(
                event.players,
                { player, id -> canSend(player, id) },
                { player, payload ->
                    val buf = PacketByteBuf(Unpooled.buffer())
                    payload.write(buf)
                    send(player, CustomPayloadS2CPacket(buf))
                }
            )
        } else {
            SyncedConfigRegistry.onJoin(
                serverPlayer,
                serverPlayer.server,
                { player, id -> this.canSend(player, id) },
                { player, payload ->
                    val buf = PacketByteBuf(Unpooled.buffer())
                    payload.write(buf)
                    send(player, CustomPayloadS2CPacket(payload.getId(), buf))
                }
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

    private var configIndex = 0
    private var reloadIndex = 0
    private var permissionIndex = 0
    private var updateS2CIndex = 0
    private var updateC2SIndex = 0
    private var forwardIndex = 0


    fun registerPayloads() {

        val configNetworkVersion = "1.0"
        val configChannel = NetworkRegistry.newSimpleChannel(ConfigSyncS2CCustomPayload.id, { configNetworkVersion }, {serverVersion -> serverVersion == configNetworkVersion}, { clientVersion -> clientVersion == configNetworkVersion })
        @Suppress("INACCESSIBLE_TYPE")
        configChannel.registerMessage(
            configIndex++,
            ConfigSyncS2CCustomPayload::class.java,
            { payload: ConfigSyncS2CCustomPayload, buf: PacketByteBuf -> payload.write(buf) },
            { buf: PacketByteBuf -> ConfigSyncS2CCustomPayload(buf) },
            NetworkEventsClient::handleConfigurationConfigSync)


        //registrar.configuration(ConfigSyncS2CCustomPayload.id, ::ConfigSyncS2CCustomPayload, NetworkEventsClient::handleConfigurationConfigSync)

        val reloadNetworkVersion = "1.0"
        val reloadChannel = NetworkRegistry.newSimpleChannel(ConfigReloadSyncS2CCustomPayload.id, { reloadNetworkVersion }, {serverVersion -> serverVersion == reloadNetworkVersion}, { clientVersion -> clientVersion == reloadNetworkVersion })
        @Suppress("INACCESSIBLE_TYPE")
        reloadChannel.registerMessage(
            reloadIndex++,
            ConfigReloadSyncS2CCustomPayload::class.java,
            { payload: ConfigReloadSyncS2CCustomPayload, buf: PacketByteBuf -> payload.write(buf) },
            { buf: PacketByteBuf -> ConfigReloadSyncS2CCustomPayload(buf) },
            NetworkEventsClient::handleReloadConfigSync)

        //registrar.play(ConfigReloadSyncS2CCustomPayload.id, ::ConfigSyncS2CCustomPayload, NetworkEventsClient::handleReloadConfigSync)

        val permNetworkVersion = "1.0"
        val permChannel = NetworkRegistry.newSimpleChannel(ConfigPermissionsS2CCustomPayload.id, { permNetworkVersion }, {serverVersion -> serverVersion == permNetworkVersion}, { clientVersion -> clientVersion == permNetworkVersion })
        @Suppress("INACCESSIBLE_TYPE")
        permChannel.registerMessage(
            permissionIndex++,
            ConfigPermissionsS2CCustomPayload::class.java,
            { payload: ConfigPermissionsS2CCustomPayload, buf: PacketByteBuf -> payload.write(buf) },
            { buf: PacketByteBuf -> ConfigPermissionsS2CCustomPayload(buf) },
            NetworkEventsClient::handlePermsUpdate)

        //registrar.play(ConfigPermissionsS2CCustomPayload.id, ::ConfigPermissionsS2CCustomPayload, NetworkEventsClient::handlePermsUpdate)

        val updateS2CNetworkVersion = "1.0"
        val updateS2CChannel = NetworkRegistry.newSimpleChannel(ConfigUpdateS2CCustomPayload.id, { updateS2CNetworkVersion }, {serverVersion -> serverVersion == updateS2CNetworkVersion}, { clientVersion -> clientVersion == updateS2CNetworkVersion })
        @Suppress("INACCESSIBLE_TYPE")
        updateS2CChannel.registerMessage(
            updateS2CIndex++,
            ConfigUpdateS2CCustomPayload::class.java,
            { payload: ConfigUpdateS2CCustomPayload, buf: PacketByteBuf -> payload.write(buf) },
            { buf: PacketByteBuf -> ConfigUpdateS2CCustomPayload(buf) },
            NetworkEventsClient::handleUpdate)

        //registrar.play(ConfigUpdateS2CCustomPayload.id, ::ConfigUpdateS2CCustomPayload, NetworkEventsClient::handleUpdate)

        val updateC2SNetworkVersion = "1.0"
        val updateC2SChannel = NetworkRegistry.newSimpleChannel(ConfigUpdateC2SCustomPayload.id, { updateC2SNetworkVersion }, {serverVersion -> serverVersion == updateC2SNetworkVersion}, { clientVersion -> clientVersion == updateC2SNetworkVersion })
        @Suppress("INACCESSIBLE_TYPE")
        updateC2SChannel.registerMessage(
            updateC2SIndex++,
            ConfigUpdateC2SCustomPayload::class.java,
            { payload: ConfigUpdateC2SCustomPayload, buf: PacketByteBuf -> payload.write(buf) },
            { buf: PacketByteBuf -> ConfigUpdateC2SCustomPayload(buf) },
            this::handleUpdate)

        //registrar.play(ConfigUpdateC2SCustomPayload.id, ::ConfigUpdateC2SCustomPayload, this::handleUpdate)

        val forwardNetworkVersion = "1.0"
        val forwardChannel = NetworkRegistry.newSimpleChannel(SettingForwardCustomPayload.id, { forwardNetworkVersion }, {serverVersion -> serverVersion == forwardNetworkVersion}, { clientVersion -> clientVersion == forwardNetworkVersion })
        @Suppress("INACCESSIBLE_TYPE")
        forwardChannel.registerMessage(
            forwardIndex++,
            SettingForwardCustomPayload::class.java,
            { payload: SettingForwardCustomPayload, buf: PacketByteBuf -> payload.write(buf) },
            { buf: PacketByteBuf -> SettingForwardCustomPayload(buf) },
            this::handleSettingForwardBidirectional)

        //registrar.play(SettingForwardCustomPayload.id, ::SettingForwardCustomPayload, this::handleSettingForwardBidirectional)

        //PayloadTypeRegistry.configurationC2S().register(ConfigSyncS2CCustomPayload.type, ConfigSyncS2CCustomPayload.codec)
        //PayloadTypeRegistry.configurationS2C().register(ConfigSyncS2CCustomPayload.type, ConfigSyncS2CCustomPayload.codec)
        //PayloadTypeRegistry.playS2C().register(ConfigPermissionsS2CCustomPayload.type, ConfigPermissionsS2CCustomPayload.codec)
        //PayloadTypeRegistry.playC2S().register(ConfigSyncS2CCustomPayload.type, ConfigSyncS2CCustomPayload.codec)
        //PayloadTypeRegistry.playS2C().register(ConfigSyncS2CCustomPayload.type, ConfigSyncS2CCustomPayload.codec)
        //PayloadTypeRegistry.playC2S().register(ConfigUpdateS2CCustomPayload.type, ConfigUpdateS2CCustomPayload.codec)
        //PayloadTypeRegistry.playS2C().register(ConfigUpdateS2CCustomPayload.type, ConfigUpdateS2CCustomPayload.codec)
        //PayloadTypeRegistry.playC2S().register(ConfigUpdateC2SCustomPayload.type, ConfigUpdateC2SCustomPayload.codec)
        //PayloadTypeRegistry.playS2C().register(ConfigUpdateC2SCustomPayload.type, ConfigUpdateC2SCustomPayload.codec)
        //PayloadTypeRegistry.playC2S().register(SettingForwardCustomPayload.type, SettingForwardCustomPayload.codec)
        //PayloadTypeRegistry.playS2C().register(SettingForwardCustomPayload.type, SettingForwardCustomPayload.codec)
    }
}