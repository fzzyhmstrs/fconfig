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
import me.fzzyhmstrs.fzzy_config.networking.*
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.ClickEvent
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.function.*
import java.util.function.Function

/**
 * Synchronization registry for [Config] instances. Handles syncing configs, sending updates to and from clients, and forwarding settings between users
 *
 * This is not a "true" registry in the Minecraft sense; as such there are not the typical helper methods like get(), getId(), etc. This registry's scope is much narrower, handling synchronization and updates of Configs.
 * @author fzzyhmstrs
 * @since 0.1.0
 */
internal object SyncedConfigRegistry {

    private val syncedConfigs : MutableMap<String, Config> = mutableMapOf()
    private val quarantinedUpdates : LimitedHashMap<String, QuarantinedUpdate> = LimitedHashMap()

    private class LimitedHashMap<K, V> : LinkedHashMap<K, V>() {

        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
            return this.size > 128
        }

    }

    internal fun syncedConfigs(): Map<String, Config> {
        return syncedConfigs
    }

    internal fun onConfigure(canSender: Predicate<Identifier>, sender: Consumer<FzzyPayload>) {
        if (!canSender.test(ConfigSyncS2CCustomPayload.id))
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

    internal fun onJoin(player: ServerPlayerEntity, server: MinecraftServer, canSender: BiPredicate<ServerPlayerEntity, Identifier>, sender: BiConsumer<ServerPlayerEntity, FzzyPayload>) {
        if (server.isSingleplayer) return
        if (!canSender.test(player, ConfigPermissionsS2CCustomPayload.id)) return
        for ((id, config) in syncedConfigs) {
            val perms = ConfigApiImpl.generatePermissionsReport(player, config, 0)
            val payload = ConfigPermissionsS2CCustomPayload(id, perms)
            sender.accept(player, payload)
        }
    }

    internal fun onEndDataReload(players: List<ServerPlayerEntity>, canSender: BiPredicate<ServerPlayerEntity, Identifier>, sender: BiConsumer<ServerPlayerEntity, FzzyPayload>) {
        for (player in players) {
            if (!canSender.test(player, ConfigSyncS2CCustomPayload.id)) continue
            for ((id, config) in syncedConfigs) {
                val syncErrors = mutableListOf<String>()
                val syncPayload = ConfigSyncS2CCustomPayload(id, ConfigApi.serializeConfig(config, syncErrors, 0)) //Don't ignore NonSync on a synchronization action
                if (syncErrors.isNotEmpty()) {
                    val syncError = ValidationResult.error(true, "Error encountered while serializing config for S2C datapack reload sync.")
                    syncError.writeError(syncErrors)
                }
                sender.accept(player, syncPayload)
                if (!canSender.test(player, ConfigPermissionsS2CCustomPayload.id)) continue
                val perms = ConfigApiImpl.generatePermissionsReport(player, config, 0)
                val permsPayload = ConfigPermissionsS2CCustomPayload(id, perms)
                sender.accept(player, permsPayload)
            }
        }
    }

    internal fun receiveConfigUpdate(serializedConfigs: Map<String, String>, server: MinecraftServer, serverPlayer: ServerPlayerEntity, changes: List<String>, canSender: BiPredicate<ServerPlayerEntity, Identifier>, sender: BiConsumer<ServerPlayerEntity, FzzyPayload>) {
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
                if (!canSender.test(player, ConfigUpdateS2CCustomPayload.id)) continue
                val newPayload = ConfigUpdateS2CCustomPayload(successfulUpdates)
                sender.accept(player, newPayload)
            }
        }
        ConfigApiImpl.printChangeHistory(changes, serializedConfigs.keys.toString(), serverPlayer)
    }

    internal fun receiveSettingForward(uuid: UUID, player: ServerPlayerEntity, scope: String, update: String, summary: String, canSender: BiPredicate<ServerPlayerEntity, Identifier>, sender: BiConsumer<ServerPlayerEntity, FzzyPayload>) {
        val receivingPlayer = player.server.playerManager.getPlayer(uuid) ?: return
        if (!canSender.test(receivingPlayer, SettingForwardCustomPayload.id)) {
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
                if (!NetworkEvents.canSend(p, ConfigUpdateS2CCustomPayload.id)) continue
                val newPayload = ConfigUpdateS2CCustomPayload(mapOf(quarantinedUpdate.configId to quarantinedUpdate.configString))
                NetworkEvents.send(p, newPayload)
            }
        }
        player?.let {
            if (NetworkEvents.canSend(player, ConfigPermissionsS2CCustomPayload.id)) {
                for ((id2, config2) in syncedConfigs) {
                    val perms = ConfigApiImpl.generatePermissionsReport(player, config2, 0)
                    val buf = PacketByteBufs.create()
                    val payload = ConfigPermissionsS2CCustomPayload(id2, perms)
                    NetworkEvents.send(player, payload)
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