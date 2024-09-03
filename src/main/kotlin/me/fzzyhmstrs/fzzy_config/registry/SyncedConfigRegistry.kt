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
import me.fzzyhmstrs.fzzy_config.annotations.Action
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
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
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
        if (!ClientPlayNetworking.canSend(SettingForwardCustomPayload.type)) {
            MinecraftClient.getInstance().player?.sendMessage("fc.config.forwarded_error".translate())
            FC.LOGGER.error("Can't forward setting; not connected to a server or server isn't accepting this type of data")
            FC.LOGGER.error("Setting not sent:")
            FC.LOGGER.warn(scope)
            FC.LOGGER.warn(summary)
            return
        }
        ClientPlayNetworking.send(SettingForwardCustomPayload(update, player, scope, summary))
    }

    fun updateServer(serializedConfigs: Map<String, String>, changeHistory: List<String>, playerPerm: Int) {
        if (!ClientPlayNetworking.canSend(ConfigUpdateC2SCustomPayload.type)) {
            FC.LOGGER.error("Can't send Config Update; not connected to a server or server isn't accepting this type of data")
            FC.LOGGER.error("changes not sent:")
            for (change in changeHistory) {
                FC.LOGGER.warn(change)
            }
            return
        }
        ClientPlayNetworking.send(ConfigUpdateC2SCustomPayload(serializedConfigs, changeHistory, playerPerm))
    }

    internal fun registerClient() {
        //receives the entire NonSync config sent by the server during CONFIGURATION stage
        ClientConfigurationNetworking.registerGlobalReceiver(ConfigSyncS2CCustomPayload.type){ payload, handler ->
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
                    MinecraftClient.getInstance().execute {
                        val records = result.get().get(RESTART_RECORDS)
                        if (!records.isNullOrEmpty()) {
                            FC.LOGGER.info("Client prompted for a restart due to received config updates")
                            FC.LOGGER.info("Restart-prompting updates:")
                            for (record in records) {
                                FC.LOGGER.info(record)
                            }
                        }
                        handler.responseSender().disconnect(FcText.translatable("fc.networking.restart"))
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
        ClientPlayNetworking.registerGlobalReceiver(ConfigUpdateS2CCustomPayload.type){ payload, context ->
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
                        context.player().sendMessage(action.clientPrompt)
                    }
                }
            }
        }

        //receives an update forwarded from another player and passes it to the client registry for handling.
        ClientPlayNetworking.registerGlobalReceiver(SettingForwardCustomPayload.type){ payload, _ ->
            val update = payload.update
            val sendingUuid = payload.player
            val scope = payload.scope
            val summary = payload.summary
            ConfigApiImplClient.handleForwardedUpdate(update, sendingUuid, scope, summary)
        }

    }

    internal fun registerAll() {
        //PayloadTypeRegistry.configurationC2S().register(ConfigSyncS2CCustomPayload.type, ConfigSyncS2CCustomPayload.codec)
        PayloadTypeRegistry.configurationS2C().register(ConfigSyncS2CCustomPayload.type, ConfigSyncS2CCustomPayload.codec)
        PayloadTypeRegistry.playS2C().register(ConfigPermissionsS2CCustomPayload.type, ConfigPermissionsS2CCustomPayload.codec)
        //PayloadTypeRegistry.playC2S().register(ConfigSyncS2CCustomPayload.type, ConfigSyncS2CCustomPayload.codec)
        PayloadTypeRegistry.playS2C().register(ConfigSyncS2CCustomPayload.type, ConfigSyncS2CCustomPayload.codec)
        //PayloadTypeRegistry.playC2S().register(ConfigUpdateS2CCustomPayload.type, ConfigUpdateS2CCustomPayload.codec)
        PayloadTypeRegistry.playS2C().register(ConfigUpdateS2CCustomPayload.type, ConfigUpdateS2CCustomPayload.codec)
        PayloadTypeRegistry.playC2S().register(ConfigUpdateC2SCustomPayload.type, ConfigUpdateC2SCustomPayload.codec)
        //PayloadTypeRegistry.playS2C().register(ConfigUpdateC2SCustomPayload.type, ConfigUpdateC2SCustomPayload.codec)
        PayloadTypeRegistry.playC2S().register(SettingForwardCustomPayload.type, SettingForwardCustomPayload.codec)
        PayloadTypeRegistry.playS2C().register(SettingForwardCustomPayload.type, SettingForwardCustomPayload.codec)

        //handles synchronization of configs to clients on CONFIGURE stage of login
        ServerConfigurationConnectionEvents.CONFIGURE.register { handler, _ ->
            if (!ServerConfigurationNetworking.canSend(handler, ConfigSyncS2CCustomPayload.type)) return@register
            for ((id, config) in syncedConfigs) {
                val syncErrors = mutableListOf<String>()
                val payload = ConfigSyncS2CCustomPayload(id, ConfigApi.serializeConfig(config, syncErrors, 0)) //Don't ignore NonSync on a synchronization action
                if (syncErrors.isNotEmpty()) {
                    val syncError = ValidationResult.error(true, "Error encountered while serializing config for S2C configuration stage sync.")
                    syncError.writeError(syncErrors)
                }
                ServerConfigurationNetworking.send(handler, payload)
            }
        }

        ServerPlayConnectionEvents.JOIN.register { handler, sender, server ->
            if (server.isSingleplayer) return@register
            if (!ServerPlayNetworking.canSend(handler.player, ConfigPermissionsS2CCustomPayload.type)) return@register
            val player = handler.player
            for ((id, config) in syncedConfigs) {
                val perms = ConfigApiImpl.generatePermissionsReport(player, config, 0)
                val payload = ConfigPermissionsS2CCustomPayload(id, perms)
                sender.sendPacket(payload)
            }
        }

        //syncs configs to clients after data pack reload, also re-syncs permissions
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
                    ServerPlayNetworking.send(player, syncPayload)
                    if (!ServerPlayNetworking.canSend(player, ConfigPermissionsS2CCustomPayload.type)) continue
                    val perms = ConfigApiImpl.generatePermissionsReport(player, config, 0)
                    val permsPayload = ConfigPermissionsS2CCustomPayload(id, perms)
                    ServerPlayNetworking.send(player, permsPayload)
                }
            }
        }

        ServerConfigurationNetworking.registerGlobalReceiver(ConfigSyncS2CCustomPayload.type){ packet, handler ->

        }
        //receives an update from a permissible client, throwing a cheat warning to the logs and to online moderators if permissions don't match (and discarding the update)
        //deserializes the updates to server configs, then propagates the updates to other online clients
        ServerPlayNetworking.registerGlobalReceiver(ConfigUpdateC2SCustomPayload.type){ payload, context ->
            val serializedConfigs = payload.updates
            val successfulUpdates: MutableMap<String, String> = mutableMapOf()
            val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")

            for ((id, configString) in serializedConfigs) {
                val config = syncedConfigs[id]
                if (config == null) {
                    FC.LOGGER.error("Config $id wasn't found!, Skipping update")
                    continue
                }

                if (!context.server().isSingleplayer) {
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
            if (!context.server().isSingleplayer) {
                for (player in context.player().server.playerManager.playerList) {
                    if (player == context.player()) continue // don't push back to the player that just sent the update
                    val newPayload = ConfigUpdateS2CCustomPayload(successfulUpdates)
                    ServerPlayNetworking.send(player, newPayload)
                }
            }
            val changes = payload.changeHistory
            ConfigApiImpl.printChangeHistory(changes, serializedConfigs.keys.toString(), context.player())
        }

        //receives a forwarded client update and passes it along to the recipient
        ServerPlayNetworking.registerGlobalReceiver(SettingForwardCustomPayload.type){ payload, context ->
            val uuid = payload.player
            val receivingPlayer = context.player().server.playerManager.getPlayer(uuid) ?: return@registerGlobalReceiver
            val scope = payload.scope
            val update = payload.update
            val summary = payload.summary
            val sendingPlayer = context.player()
            ServerPlayNetworking.send(receivingPlayer, SettingForwardCustomPayload(update, sendingPlayer.uuid, scope, summary))
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
                if (!ServerPlayNetworking.canSend(p, ConfigUpdateS2CCustomPayload.type)) continue
                val newPayload = ConfigUpdateS2CCustomPayload(mapOf(quarantinedUpdate.configId to quarantinedUpdate.configString))
                ServerPlayNetworking.send(player, newPayload)
            }
        }
        player?.let {
            for ((id2, config2) in syncedConfigs) {
                val perms = ConfigApiImpl.generatePermissionsReport(player, config2, 0)
                val payload = ConfigPermissionsS2CCustomPayload(id2, perms)
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
