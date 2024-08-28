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

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.config.ConfigContext.Keys.RESTART_KEY
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImplClient
import me.fzzyhmstrs.fzzy_config.networking.*
import me.fzzyhmstrs.fzzy_config.networking.ConfigSyncS2CCustomPayload
import me.fzzyhmstrs.fzzy_config.networking.ConfigUpdateC2SCustomPayload
import me.fzzyhmstrs.fzzy_config.networking.ConfigUpdateS2CCustomPayload
import me.fzzyhmstrs.fzzy_config.networking.SettingForwardCustomPayload
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.client.MinecraftClient
import net.minecraft.server.MinecraftServer
import net.minecraft.text.ClickEvent
import net.minecraft.text.Text
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.function.Consumer
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
    private val quarantinedUpdates : Object2ObjectLinkedOpenHashMap<String, QuarantinedUpdate> = Object2ObjectLinkedOpenHashMap()

    fun forwardSetting(update: String, player: UUID, scope: String, summary: String) {
        val buf = PacketByteBufs.create()
        SettingForwardCustomPayload(update, player, scope, summary).write(buf)
        ClientPlayNetworking.send(SettingForwardCustomPayload.id, buf)
    }

    fun updateServer(serializedConfigs: Map<String, String>, changeHistory: List<String>, playerPerm: Int) {
        val buf = PacketByteBufs.create()
        ConfigUpdateC2SCustomPayload(serializedConfigs, changeHistory, playerPerm).write(buf)
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
                val result = ConfigApi.deserializeConfig(config, configString, errors, ConfigApiImpl.CHECK_RESTART) //0: Don't ignore NonSync on a synchronization action, 2: Watch for RequiresRestart
                val restart = result.get().getBoolean(RESTART_KEY)
                result.writeError(errors)
                result.get().config.save() //save config to the client
                if (restart) {
                    client.execute {
                        client.world?.disconnect()
                        client.disconnect()
                        ConfigApiImpl.openRestartScreen()
                    }
                }
            }
        }

        //receives a permission report for a config for the client player.
        ClientPlayNetworking.registerGlobalReceiver(ConfigPermissionsS2CCustomPayload.type) { payload, _ ->
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
                    val result = ConfigApiImpl.deserializeUpdate(config, configString, errors, ConfigApiImpl.CHECK_RESTART)
                    val restart = result.get().getBoolean(RESTART_KEY)
                    result.writeError(errors)
                    result.get().config.save()
                    if (restart) {
                        client.player?.sendMessage("fc.config.restart.update".translate())
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

        //handles synchronization of configs to clients on CONFIGURE stage of login
        //syncs configs to clients after datapack reload
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register { server, _, _ ->
            val players = server.playerManager.playerList
            for (player in players) {
                if (!ServerPlayNetworking.canSend(player, ConfigSyncS2CCustomPayload.type)) continue
                for ((id, config) in syncedConfigs) {
                    val syncErrors = mutableListOf<String>()
                    val syncPayload = ConfigSyncS2CCustomPayload(id, ConfigApi.serializeConfig(config, syncErrors, 0)) //Don't ignore NonSync on a synchronization action
                    if (syncErrors.isNotEmpty()) {
                        val syncError = ValidationResult.error(true, "Error encountered while serializing config for S2C datapack reload sync.")
                        syncError.writeError(syncErrors)
                    }
                    val buf = PacketByteBufs.create()
                    payload.write(buf)
                    ServerPlayNetworking.send(player, ConfigSyncS2CCustomPayload.id, buf)
                    if (!ServerPlayNetworking.canSend(player, ConfigPermissionsS2CCustomPayload.type)) continue
                    val perms = ConfigApiImpl.generatePermissionsReport(player, config, 0)
                    val permsPayload = ConfigPermissionsS2CCustomPayload(id, perms)
                    ServerPlayNetworking.send(player, permsPayload)
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

                val validationResult = ConfigApiImpl.validatePermissions(context.player(), id, config, configString)

                if(validationResult.isError()) {

                    FC.LOGGER.error("Player [${context.player().name}] may have tried to cheat changes onto the Server Config! Problem settings found: ${validationResult.get().joinToString(" | ")}")
                    FC.LOGGER.error("This update has not been applied, and has been moved to quarantine. Use the configure_update command to inspect and permit or deny the update.")
                    FC.LOGGER.warn("If no action is taken, the quarantined update will be flushed on the next server restart, and its changes will not be applied")

                    val changes = payload.changeHistory
                    ConfigApiImpl.printChangeHistory(changes, serializedConfigs.keys.toString(), context.player())

                    val quarantine = QuarantinedUpdate(context.player().uuid, changes, id, configString)
                    val quarantineId = id + " @" + context.player().name.string + " @" + formatter.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(System.currentTimeMillis()), ZoneId.systemDefault()))
                    quarantinedUpdates[quarantineId] = quarantine
                    if (quarantinedUpdates.size > 128) {
                        quarantinedUpdates.pollFirstEntry()
                    }

                    for (player in context.player().server.playerManager.playerList) {
                        if(ConfigApiImpl.isConfigAdmin(player, config))
                            player.sendMessageToClient("fc.networking.permission.cheat".translate(context.player().name), false)
                        player.sendMessageToClient("fc.command.accept".translate().styled { s -> s.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/configure_update \"$id\" inspect")) }, false)
                        player.sendMessageToClient("fc.command.accept".translate().styled { s -> s.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/configure_update \"$id\" accept")) }, false)
                        player.sendMessageToClient("fc.command.accept".translate().styled { s -> s.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/configure_update \"$id\" reject")) }, false)
                    }
                    continue
                }
                val errors = mutableListOf<String>()
                val result = ConfigApiImpl.deserializeUpdate(config, configString, errors, ConfigApiImpl.CHECK_RESTART)
                val restart = result.get().getBoolean(RESTART_KEY)
                result.writeError(errors)
                result.get().config.save()
                if (restart) {
                    FC.LOGGER.warn("The server has received a config update that may require a restart, please consult the change history below for details. Connected clients have been automatically updated and notified of the potential for restart.")
                }
                successfulUpdates[id] = configString
            }
            for (player in serverPlayer.server.playerManager.playerList) {
                if (player == serverPlayer) continue // don't push back to the player that just sent the update
                val newPayload = ConfigUpdateS2CCustomPayload(serializedConfigs)
                val newBuf = PacketByteBufs.create()
                newPayload.write(newBuf)
                ServerPlayNetworking.send(player, ConfigUpdateS2CCustomPayload.id, newBuf)
            }
            val changes = payload.changeHistory
            ConfigApiImpl.printChangeHistory(changes, serializedConfigs.keys.toString(), serverPlayer)
        }

        //receives a forwarded client update and passes it along to the recipient
        ServerPlayNetworking.registerGlobalReceiver(SettingForwardCustomPayload.id){ server, serverPlayer, context, buf, sender ->
            val payload = SettingForwardCustomPayload(buf)
            val uuid = payload.player
            val receivingPlayer = serverPlayer.server.playerManager.getPlayer(uuid) ?: return@registerGlobalReceiver
            val scope = payload.scope
            val update = payload.update
            val summary = payload.summary
            val sendingPlayer = serverPlayer
            val newPayload = SettingForwardCustomPayload(update, sendingPlayer.uuid, scope, summary)
            val newBuf = PacketByteBufs.create()
            newPayload.write(newBuf)
            ServerPlayNetworking.send(receivingPlayer, SettingForwardCustomPayload.id, newBuf)
        }
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
        val player = server.playerManager.getPlayer(uuid)
        val errors = mutableListOf<String>()
        
        val result = ConfigApiImpl.deserializeUpdate(config, configString, errors, ConfigApiImpl.CHECK_RESTART)
        val restart = result.get().getBoolean(RESTART_KEY)
        result.writeError(errors)
        result.get().config.save()
        if (restart) {
            FC.LOGGER.warn("The server accepted a quarantined config update that may require a restart, please consult the change history below for details. Connected clients have been automatically updated and notified of the potential for restart.")
        }
        for (p in context.player().server.playerManager.playerList) {
            if (p == player) continue // don't push back to the player that just sent the update
            if (!ServerPlayNetworking.canSend(p, ConfigUpdateS2CCustomPayload.type)) continue
            val newPayload = ConfigUpdateS2CCustomPayload(mapOf(quarantinedUpdate.id to quarantinedUpdate.configString))
            ServerPlayNetworking.send(player, newPayload)
        }
        player?.let {
            for ((id, config) in syncedConfigs) {
                val perms = ConfigApiImpl.generatePermissionsReport(player, config, 0)
                val payload = ConfigPermissionsS2CCustomPayload(id, perms)
                ServerPlayNetworking.send(player, payload)
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

    private class QuarantinedUpdate(val playerUuid: UUID, val changeHistory: List<String>, val configId: String, val configString: String)
}
