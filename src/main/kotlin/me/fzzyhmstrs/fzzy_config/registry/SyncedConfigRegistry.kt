/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.registry

import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.config.ConfigContext.Keys.ACTIONS
import me.fzzyhmstrs.fzzy_config.config.ConfigContext.Keys.RESTART_RECORDS
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImplClient
import me.fzzyhmstrs.fzzy_config.networking.*
import me.fzzyhmstrs.fzzy_config.networking.ConfigSyncS2CCustomPayload
import me.fzzyhmstrs.fzzy_config.networking.ConfigUpdateC2SCustomPayload
import me.fzzyhmstrs.fzzy_config.networking.ConfigUpdateS2CCustomPayload
import me.fzzyhmstrs.fzzy_config.networking.SettingForwardCustomPayload
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.client.MinecraftClient
import net.minecraft.network.packet.CustomPayload
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.network.ServerPlayNetworkHandler
import net.minecraft.text.ClickEvent
import net.minecraft.text.Text
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.function.BiConsumer
import java.util.function.BiPredicate
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Predicate

/**
 * Synchronization registry for [Config] instances. Handles syncing configs, sending updates to and from clients, and forwarding settings between users
 *
 * This is not a "true" registry in the Minecraft sense; as such there are not the typical helper methods like get(), getId(), etc. This registry's scope is much narrower, handling synchronization and updates of Configs.
 * @author fzzyhmstrs
 * @since 0.1.0
 */
internal object SyncedConfigRegistry {

    private val syncedConfigs : MutableMap<String, Config> = mutableMapOf()
    private val quarantinedUpdates : LinkedHashMap<String, QuarantinedUpdate> = LimitedHashMap()

    private class LimitedHashMap<K, V> : LinkedHashMap<K, V>() {

        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
            return this.size > 128
        }

    }

    fun forwardSetting(update: String, player: UUID, scope: String, summary: String) {
        if (!ClientPlayNetworking.canSend(SettingForwardCustomPayload.id)) {
            MinecraftClient.getInstance().player?.sendMessage("fc.config.forwarded_error.c2s".translate())
            FC.LOGGER.error("Can't forward setting; not connected to a server or server isn't accepting this type of data")
            FC.LOGGER.error("Setting not sent:")
            FC.LOGGER.warn(scope)
            FC.LOGGER.warn(summary)
            return
        }
        val buf = PacketByteBufs.create()
        val payload = SettingForwardCustomPayload(update, player, scope, summary)
        payload.write(buf)

        ClientPlayNetworking.send(SettingForwardCustomPayload.id, buf)
    }

    fun updateServer(serializedConfigs: Map<String, String>, changeHistory: List<String>, playerPerm: Int) {
        if (!ClientPlayNetworking.canSend(ConfigUpdateC2SCustomPayload.id)) {
            FC.LOGGER.error("Can't send Config Update; not connected to a server or server isn't accepting this type of data")
            FC.LOGGER.error("changes not sent:")
            for (change in changeHistory) {
                FC.LOGGER.warn(change)
            }
            return
        }
        val buf = PacketByteBufs.create()
        val payload = ConfigUpdateC2SCustomPayload(serializedConfigs, changeHistory, playerPerm)
        payload.write(buf)

        ClientPlayNetworking.send(ConfigUpdateC2SCustomPayload.id, buf)
    }

    internal fun registerClient() {
        //receives the entire NonSync config sent by the server during CONFIGURATION stage
        ClientPlayNetworking.registerGlobalReceiver(ConfigSyncS2CCustomPayload.id){ client, _, buf, _ ->
            val payload = ConfigSyncS2CCustomPayload(buf)
            val id = payload.id
            val configString = payload.serializedConfig
            if (syncedConfigs.containsKey(id)) {
                val config = syncedConfigs[id] ?: return@registerGlobalReceiver
                val errors = mutableListOf<String>()
                val result = ConfigApi.deserializeConfig(config, configString, errors, ConfigApiImpl.CHECK_ACTIONS_AND_RECORD_RESTARTS) //0: Don't ignore NonSync on a synchronization action, 2: Watch for RequiresRestart
                val actions = result.get().getOrDefault(ACTIONS, setOf())
                result.writeError(errors)
                result.get().config.save() //save config to the client
                if (actions.any { it.restartPrompt }) {
                    client.execute {
                        val records = result.get().get(RESTART_RECORDS)
                        if (!records.isNullOrEmpty()) {
                            FC.LOGGER.info("Client prompted for a restart due to received config updates")
                            FC.LOGGER.info("Restart-prompting updates:")
                            for (record in records) {
                                FC.LOGGER.info(record)
                            }
                        }
                        client.world?.disconnect()
                        client.disconnect()
                        ConfigApiImpl.openRestartScreen()
                    }
                }
            }
        }

        //receives a permission report for a config for the client player.
        ClientPlayNetworking.registerGlobalReceiver(ConfigPermissionsS2CCustomPayload.id) { _, _, buf, _ ->
            val payload = ConfigPermissionsS2CCustomPayload(buf)

            val id = payload.id
            val perms = payload.permissions
            ConfigApiImplClient.updatePerms(id, perms)
        }

        //receives a config update from the server after another client pushes changes to it.
        ClientPlayNetworking.registerGlobalReceiver(ConfigUpdateS2CCustomPayload.id){ client, _, buf, _ ->
            val payload = ConfigUpdateS2CCustomPayload(buf)
            val serializedConfigs = payload.updates
            for ((id, configString) in serializedConfigs) {
                if (syncedConfigs.containsKey(id)) {
                    val config = syncedConfigs[id] ?: return@registerGlobalReceiver
                    val errors = mutableListOf<String>()
                    val result = ConfigApiImpl.deserializeUpdate(config, configString, errors, ConfigApiImpl.CHECK_ACTIONS)
                    val actions = result.get().getOrDefault(ACTIONS, setOf())
                    result.writeError(errors)
                    result.get().config.save()
                    for (action in actions) {
                        client.player?.sendMessage(action.clientPrompt)
                    }
                }
            }
        }

        //receives an update forwarded from another player and passes it to the client registry for handling.
        ClientPlayNetworking.registerGlobalReceiver(SettingForwardCustomPayload.id){ _, _, buf, _ ->
            val payload = SettingForwardCustomPayload(buf)
            val update = payload.update
            val sendingUuid = payload.player
            val scope = payload.scope
            val summary = payload.summary
            ConfigApiImplClient.handleForwardedUpdate(update, sendingUuid, scope, summary)
        }

    }

    fun syncConfigs(handler: ServerPlayNetworkHandler) {
        for ((id, config) in syncedConfigs) {
            val syncErrors = mutableListOf<String>()
            val payload = ConfigSyncS2CCustomPayload(id, ConfigApi.serializeConfig(config, syncErrors, 0)) //Don't ignore NonSync on a synchronization action
            if (syncErrors.isNotEmpty()) {
                val syncError = ValidationResult.error(true, "Error encountered while serializing config for S2C configuration stage sync.")
                syncError.writeError(syncErrors)
            }
            val buf = PacketByteBufs.create()
            payload.write(buf)
            ServerPlayNetworking.send(handler.player, ConfigSyncS2CCustomPayload.id, buf)
        }
    }

    internal fun registerAll() {
        ServerPlayConnectionEvents.JOIN.register { handler, sender, server ->
            if (server.isSingleplayer) return@register
            if (!ServerPlayNetworking.canSend(handler.player, ConfigPermissionsS2CCustomPayload.id)) return@register
            val player = handler.player
            for ((id, config) in syncedConfigs) {
                val perms = ConfigApiImpl.generatePermissionsReport(player, config, 0)
                val buf = PacketByteBufs.create()
                val payload = ConfigPermissionsS2CCustomPayload(id, perms)
                payload.write(buf)
                sender.sendPacket(ConfigPermissionsS2CCustomPayload.id, buf)
            }
        }

        //handles synchronization of configs to clients on CONFIGURE stage of login
        //syncs configs to clients after datapack reload
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register { server, _, _ ->
            val players = server.playerManager.playerList
            for (player in players) {
                if (!ServerPlayNetworking.canSend(player, ConfigSyncS2CCustomPayload.id)) continue
                for ((id, config) in syncedConfigs) {
                    val syncErrors = mutableListOf<String>()
                    val syncPayload = ConfigSyncS2CCustomPayload(id, ConfigApi.serializeConfig(config, syncErrors, 0)) //Don't ignore NonSync on a synchronization action
                    if (syncErrors.isNotEmpty()) {
                        val syncError = ValidationResult.error(true, "Error encountered while serializing config for S2C datapack reload sync.")
                        syncError.writeError(syncErrors)
                    }
                    val buf = PacketByteBufs.create()
                    syncPayload.write(buf)
                    ServerPlayNetworking.send(player, ConfigSyncS2CCustomPayload.id, buf)
                    if (!ServerPlayNetworking.canSend(player, ConfigPermissionsS2CCustomPayload.id)) continue
                    val perms = ConfigApiImpl.generatePermissionsReport(player, config, 0)
                    val buf2 = PacketByteBufs.create()
                    val permsPayload = ConfigPermissionsS2CCustomPayload(id, perms)
                    permsPayload.write(buf2)
                    ServerPlayNetworking.send(player, ConfigPermissionsS2CCustomPayload.id, buf2)
                }
            }
        }

        //receives an update from a permissible client, throwing a cheat warning to the logs and to online moderators if permissions don't match (and discarding the update)
        //deserializes the updates to server configs, then propagates the updates to other online clients
        ServerPlayNetworking.registerGlobalReceiver(ConfigUpdateC2SCustomPayload.id){ server, serverPlayer, context, buf, sender ->
            val payload = ConfigUpdateC2SCustomPayload(buf)
            val permLevel = payload.playerPerm
            val serializedConfigs = payload.updates
            val successfulUpdates: MutableMap<String, String> = mutableMapOf()
            val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")

            for ((id, configString) in serializedConfigs) {
                val config = syncedConfigs[id]
                if (config == null) {
                    FC.LOGGER.error("Config $id wasn't found!, Skipping update")
                    continue
                }

                if (!serverPlayer.server.isSingleplayer) {
                    val validationResult = ConfigApiImpl.validatePermissions(serverPlayer, id, config, configString)

                    if(validationResult.isError()) {

                        FC.LOGGER.error("Player [${serverPlayer.name}] may have tried to cheat changes onto the Server Config! Problem settings found: ${validationResult.get().joinToString(" | ")}")
                        FC.LOGGER.error("This update has not been applied, and has been moved to quarantine. Use the configure_update command to inspect and permit or deny the update.")
                        FC.LOGGER.warn("If no action is taken, the quarantined update will be flushed on the next server restart, and its changes will not be applied")

                        val changes = payload.changeHistory
                        ConfigApiImpl.printChangeHistory(changes, serializedConfigs.keys.toString(), serverPlayer)

                        val quarantine = QuarantinedUpdate(serverPlayer.uuid, changes, id, configString)
                        val quarantineId = id + " @" + serverPlayer.name.string + " @" + formatter.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(System.currentTimeMillis()), ZoneId.systemDefault()))
                        quarantinedUpdates[quarantineId] = quarantine

                        for (player in serverPlayer.server.playerManager.playerList) {
                            if(ConfigApiImpl.isConfigAdmin(player, config))
                                player.sendMessageToClient("fc.networking.permission.cheat".translate(serverPlayer.name), false)
                            player.sendMessageToClient("fc.command.accept".translate().styled { s -> s.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/configure_update \"$id\" inspect")) }, false)
                            player.sendMessageToClient("fc.command.accept".translate().styled { s -> s.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/configure_update \"$id\" accept")) }, false)
                            player.sendMessageToClient("fc.command.accept".translate().styled { s -> s.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/configure_update \"$id\" reject")) }, false)
                        }
                        continue
                    }
                }
                val errors = mutableListOf<String>()
                val result = ConfigApiImpl.deserializeUpdate(config, configString, errors, ConfigApiImpl.CHECK_ACTIONS_AND_RECORD_RESTARTS)
                val actions = result.get().getOrDefault(ACTIONS, setOf())
                result.writeError(errors)
                result.get().config.save()
                if (actions.any { it.restartPrompt }) {
                    FC.LOGGER.warn("The server has received a config update that may require a restart. Connected clients have been automatically updated and notified of the potential for restart.")
                    val records = result.get().get(RESTART_RECORDS)
                    if (!records.isNullOrEmpty()) {
                        FC.LOGGER.info("Server prompted for a restart due to received config changes")
                        FC.LOGGER.info("Restart-prompting changes:")
                        for (record in records) {
                            FC.LOGGER.info(record)
                        }
                    }
                }
                successfulUpdates[id] = configString
            }
            if (!serverPlayer.server.isSingleplayer) {
                for (player in serverPlayer.server.playerManager.playerList) {
                    if (player == serverPlayer) continue // don't push back to the player that just sent the update
                    if (!ServerPlayNetworking.canSend(player, ConfigUpdateS2CCustomPayload.id)) continue
                    val buf = PacketByteBufs.create()
                    val newPayload = ConfigUpdateS2CCustomPayload(successfulUpdates)
                    newPayload.write(buf)
                    ServerPlayNetworking.send(player, ConfigUpdateS2CCustomPayload.id, buf)
                }
            }
            val changes = payload.changeHistory
            ConfigApiImpl.printChangeHistory(changes, serializedConfigs.keys.toString(), serverPlayer)
        }

        //receives a forwarded client update and passes it along to the recipient
        ServerPlayNetworking.registerGlobalReceiver(SettingForwardCustomPayload.id){ server, serverPlayer, context, buf, sender ->
            val payload = SettingForwardCustomPayload(buf)
            val uuid = payload.player
            val receivingPlayer = serverPlayer.server.playerManager.getPlayer(uuid) ?: return@registerGlobalReceiver
            if (!ServerPlayNetworking.canSend(receivingPlayer, SettingForwardCustomPayload.id)) {
                serverPlayer.sendMessage("fc.config.forwarded_error.s2c".translate())
                return@registerGlobalReceiver
            }
            val scope = payload.scope
            val update = payload.update
            val summary = payload.summary
            val newPayload = SettingForwardCustomPayload(update, serverPlayer.uuid, scope, summary)
            val newBuf = PacketByteBufs.create()
            newPayload.write(newBuf)
            ServerPlayNetworking.send(receivingPlayer, SettingForwardCustomPayload.id, newBuf)
        }
    }

    fun onConfigure(canSender: Predicate<CustomPayload.Id<*>>, sender: Consumer<CustomPayload>) {
        if (!canSender.test(ConfigSyncS2CCustomPayload.type))
        for ((id, config) in syncedConfigs) {
            val syncErrors = mutableListOf<String>()
            val payload = ConfigSyncS2CCustomPayload(id, ConfigApi.serializeConfig(config, syncErrors, 0)) //Don't ignore NonSync on a synchronization action
            if (syncErrors.isNotEmpty()) {
                val syncError = ValidationResult.error(true, "Error encountered while serializing config for S2C configuration stage sync.")
                syncError.writeError(syncErrors)
            }
            sender.accept(payload)
        }
    }

    fun onJoin(player: ServerPlayerEntity, server: MinecraftServer, canSender: BiPredicate<ServerPlayerEntity, CustomPayload.Id<*>>, sender: BiConsumer<ServerPlayerEntity, CustomPayload>) {
        if (server.isSingleplayer) return
        if (!canSender.test(player, ConfigPermissionsS2CCustomPayload.type)) return
        for ((id, config) in syncedConfigs) {
            val perms = ConfigApiImpl.generatePermissionsReport(player, config, 0)
            val payload = ConfigPermissionsS2CCustomPayload(id, perms)
            sender.accept(player, payload)
        }
    }

    fun onEndDataReload(players: List<ServerPlayerEntity>, canSender: BiPredicate<ServerPlayerEntity, CustomPayload.Id<*>>, sender: BiConsumer<ServerPlayerEntity, CustomPayload>) {
        for (player in players) {
            if (!canSender.test(player, ConfigSyncS2CCustomPayload.type)) continue
            for ((id, config) in syncedConfigs) {
                val syncErrors = mutableListOf<String>()
                val syncPayload = ConfigSyncS2CCustomPayload(id, ConfigApi.serializeConfig(config, syncErrors, 0)) //Don't ignore NonSync on a synchronization action
                if (syncErrors.isNotEmpty()) {
                    val syncError = ValidationResult.error(true, "Error encountered while serializing config for S2C datapack reload sync.")
                    syncError.writeError(syncErrors)
                }
                sender.accept(player, syncPayload)
                if (!canSender.test(player, ConfigPermissionsS2CCustomPayload.type)) continue
                val perms = ConfigApiImpl.generatePermissionsReport(player, config, 0)
                val permsPayload = ConfigPermissionsS2CCustomPayload(id, perms)
                sender.accept(player, permsPayload)
            }
        }
    }

    fun receiveConfigUpdate(serializedConfigs: Map<String, String>, server: MinecraftServer, serverPlayer: ServerPlayerEntity, changes: List<String>, canSender: BiPredicate<ServerPlayerEntity, CustomPayload.Id<*>>, sender: BiConsumer<ServerPlayerEntity, CustomPayload>) {
        val successfulUpdates: MutableMap<String, String> = mutableMapOf()
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")

        for ((id, configString) in serializedConfigs) {
            val config = syncedConfigs[id]
            if (config == null) {
                FC.LOGGER.error("Config $id wasn't found!, Skipping update")
                continue
            }

            if (!server.isSingleplayer) {
                val validationResult = ConfigApiImpl.validatePermissions(serverPlayer, id, config, configString)

                if(validationResult.isError()) {

                    FC.LOGGER.error("Player [${serverPlayer.name}] may have tried to cheat changes onto the Server Config! Problem settings found: ${validationResult.get().joinToString(" | ")}")
                    FC.LOGGER.error("This update has not been applied, and has been moved to quarantine. Use the configure_update command to inspect and permit or deny the update.")
                    FC.LOGGER.warn("If no action is taken, the quarantined update will be flushed on the next server restart, and its changes will not be applied")

                    ConfigApiImpl.printChangeHistory(changes, serializedConfigs.keys.toString(), serverPlayer)

                    val quarantine = QuarantinedUpdate(serverPlayer.uuid, changes, id, configString)
                    val quarantineId = id + " @" + serverPlayer.name.string + " @" + formatter.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(System.currentTimeMillis()), ZoneId.systemDefault()))
                    quarantinedUpdates[quarantineId] = quarantine
                    if (quarantinedUpdates.size > 128) {
                        quarantinedUpdates.pollFirstEntry()
                    }

                    for (player in server.playerManager.playerList) {
                        if(ConfigApiImpl.isConfigAdmin(player, config))
                            player.sendMessageToClient("fc.networking.permission.cheat".translate(serverPlayer.name), false)
                        player.sendMessageToClient("fc.command.accept".translate().styled { s -> s.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/configure_update \"$id\" inspect")) }, false)
                        player.sendMessageToClient("fc.command.accept".translate().styled { s -> s.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/configure_update \"$id\" accept")) }, false)
                        player.sendMessageToClient("fc.command.accept".translate().styled { s -> s.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/configure_update \"$id\" reject")) }, false)
                    }
                    continue
                }
            }
            val errors = mutableListOf<String>()
            val result = ConfigApiImpl.deserializeUpdate(config, configString, errors, ConfigApiImpl.CHECK_ACTIONS_AND_RECORD_RESTARTS)
            val actions = result.get().getOrDefault(ACTIONS, setOf())
            result.writeError(errors)
            result.get().config.save()
            if (actions.any { it.restartPrompt }) {
                FC.LOGGER.warn("The server has received a config update that may require a restart. Connected clients have been automatically updated and notified of the potential for restart.")
                val records = result.get().get(RESTART_RECORDS)
                if (!records.isNullOrEmpty()) {
                    FC.LOGGER.info("Server prompted for a restart due to received config changes")
                    FC.LOGGER.info("Restart-prompting changes:")
                    for (record in records) {
                        FC.LOGGER.info(record)
                    }
                }
            }
            successfulUpdates[id] = configString
        }
        if (!server.isSingleplayer) {
            for (player in serverPlayer.server.playerManager.playerList) {
                if (player == serverPlayer) continue // don't push back to the player that just sent the update
                if (!canSender.test(player, ConfigUpdateS2CCustomPayload.type)) continue
                val newPayload = ConfigUpdateS2CCustomPayload(successfulUpdates)
                sender.accept(player, newPayload)
            }
        }
        ConfigApiImpl.printChangeHistory(changes, serializedConfigs.keys.toString(), serverPlayer)
    }

    fun receiveSettingForward(uuid: UUID, player: ServerPlayerEntity, scope: String, update: String, summary: String, canSender: BiPredicate<ServerPlayerEntity, CustomPayload.Id<*>>, sender: BiConsumer<ServerPlayerEntity, CustomPayload>) {
        val receivingPlayer = player.server.playerManager.getPlayer(uuid) ?: return
        if (!canSender.test(receivingPlayer, SettingForwardCustomPayload.type)) {
            player.sendMessage("fc.config.forwarded_error.s2c".translate())
            return
        }
        sender.accept(receivingPlayer, SettingForwardCustomPayload(update, player.uuid, scope, summary))
    }

    internal fun quarantineList(): Set<String> {
        return quarantinedUpdates.keys
    }

    internal fun inspectQuarantine(id: String, nameFinder: Function<UUID, Text?>, messageSender: Consumer<Text>) {
        val quarantinedUpdate = quarantinedUpdates[id] ?: return
        messageSender.accept("fc.command.config".translate())
        messageSender.accept(quarantinedUpdate.configId.translate())
        messageSender.accept("fc.command.player".translate())
        messageSender.accept(quarantinedUpdate.playerUuid.toString().lit())
        nameFinder.apply(quarantinedUpdate.playerUuid)?.let {
            messageSender.accept(it)
        }
        messageSender.accept("fc.command.history".translate())
        for (str in quarantinedUpdate.changeHistory) {
            messageSender.accept(str.lit())
        }
        messageSender.accept("fc.command.accept".translate().styled { s -> s.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/configure_update \"$id\" accept")) })
        messageSender.accept("fc.command.reject".translate().styled { s -> s.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/configure_update \"$id\" reject")) })
    }

    internal fun acceptQuarantine(id: String, server: MinecraftServer) {
        val quarantinedUpdate = quarantinedUpdates[id] ?: return
        val config = syncedConfigs[quarantinedUpdate.configId]
        val player = server.playerManager.getPlayer(quarantinedUpdate.playerUuid)
        if (config != null) {
            val errors = mutableListOf<String>()

            val result = ConfigApiImpl.deserializeUpdate(config, quarantinedUpdate.configString, errors, ConfigApiImpl.CHECK_ACTIONS)
            val actions = result.get().getOrDefault(ACTIONS, setOf())
            result.writeError(errors)
            result.get().config.save()
            if (actions.any { it.restartPrompt }) {
                FC.LOGGER.warn("The server accepted a quarantined config update that may require a restart, please consult the change history below for details. Connected clients have been automatically updated and notified of the potential for restart.")
            }
            for (p in server.playerManager.playerList) {
                if (p == player) continue // don't push back to the player that just sent the update
                if (!ServerPlayNetworking.canSend(p, ConfigUpdateS2CCustomPayload.id)) continue
                val buf = PacketByteBufs.create()
                val newPayload = ConfigUpdateS2CCustomPayload(mapOf(quarantinedUpdate.configId to quarantinedUpdate.configString))
                newPayload.write(buf)
                ServerPlayNetworking.send(player, ConfigUpdateS2CCustomPayload.id, buf)
            }
        }
        player?.let {
            if (ServerPlayNetworking.canSend(player, ConfigPermissionsS2CCustomPayload.id)) {
                for ((id2, config2) in syncedConfigs) {
                    val perms = ConfigApiImpl.generatePermissionsReport(player, config2, 0)
                    val buf = PacketByteBufs.create()
                    val payload = ConfigPermissionsS2CCustomPayload(id2, perms)
                    payload.write(buf)
                    ServerPlayNetworking.send(player, ConfigPermissionsS2CCustomPayload.id, buf)
                }
            }
        }
        quarantinedUpdates.remove(id)
    }

    internal fun rejectQuarantine(id: String, server: MinecraftServer) {
        quarantinedUpdates.remove(id)
    }

    internal fun hasConfig(id: String): Boolean {
        return syncedConfigs.containsKey(id)
    }

    internal fun registerConfig(config: Config) {
        syncedConfigs[config.getId().toTranslationKey()] = config
    }

    internal class QuarantinedUpdate(val playerUuid: UUID, val changeHistory: List<String>, val configId: String, val configString: String)
}