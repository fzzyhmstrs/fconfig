/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.networking

import com.mojang.brigadier.CommandDispatcher
import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.FCC
import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImplClient
import me.fzzyhmstrs.fzzy_config.impl.ValidScopesArgumentType
import me.fzzyhmstrs.fzzy_config.impl.ValidSubScopesArgumentType
import me.fzzyhmstrs.fzzy_config.registry.ClientConfigRegistry
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import net.minecraft.client.MinecraftClient
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.util.Identifier
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.TickEvent
import net.neoforged.neoforge.network.handling.ConfigurationPayloadContext
import net.neoforged.neoforge.network.handling.IPayloadContext
import net.neoforged.neoforge.network.registration.NetworkRegistry
import java.util.*

internal object NetworkEventsClient {

    fun canSend(id: Identifier): Boolean {
        val handler = MinecraftClient.getInstance().networkHandler ?: return false
        return NetworkRegistry.getInstance().isConnected(handler, id)
    }

    fun forwardSetting(update: String, player: UUID, scope: String, summary: String) {
        if (!canSend(SettingForwardCustomPayload.id)) {
            MinecraftClient.getInstance().player?.sendMessage("fc.config.forwarded_error.c2s".translate())
            FC.LOGGER.error("Can't forward setting; not connected to a server or server isn't accepting this type of data")
            FC.LOGGER.error("Setting not sent:")
            FC.LOGGER.warn(scope)
            FC.LOGGER.warn(summary)
            return
        }
        ConfigApi.network().send(SettingForwardCustomPayload(update, player, scope, summary), null)
    }

    fun updateServer(serializedConfigs: Map<String, String>, changeHistory: List<String>, playerPerm: Int) {
        if (!canSend(ConfigUpdateC2SCustomPayload.id)) {
            FC.LOGGER.error("Can't send Config Update; not connected to a server or server isn't accepting this type of data")
            FC.LOGGER.error("changes not sent:")
            for (change in changeHistory) {
                FC.LOGGER.warn(change)
            }
            return
        }
        ConfigApi.network().send(ConfigUpdateC2SCustomPayload(serializedConfigs, changeHistory, playerPerm), null)
    }

    fun handleConfigurationConfigSync(payload: ConfigSyncS2CCustomPayload, context: ConfigurationPayloadContext) {
        ClientConfigRegistry.receiveSync(
            payload.id,
            payload.serializedConfig
        ) { _ -> context.channelHandlerContext.disconnect() }
    }

    fun handleReloadConfigSync(payload: ConfigSyncS2CCustomPayload, context: IPayloadContext) {
        val player = context.player().orElse(null) ?: return
        ClientConfigRegistry.receiveReloadSync(
            payload.id,
            payload.serializedConfig,
            player
        )
    }

    fun handlePermsUpdate(payload: ConfigPermissionsS2CCustomPayload, context: IPayloadContext) {
        ClientConfigRegistry.receivePerms(payload.id, payload.permissions)
    }

    fun handleUpdate(payload: ConfigUpdateS2CCustomPayload, context: IPayloadContext) {
        val player = context.player().orElse(null) ?: return
        ClientConfigRegistry.receiveUpdate(payload.updates, player)
    }

    fun handleSettingForward(payload: SettingForwardCustomPayload, context: IPayloadContext) {
        ClientConfigRegistry.handleForwardedUpdate(payload.update, payload.player, payload.scope, payload.summary)
    }

    fun handleTick(event: TickEvent.ClientTickEvent) {
        if (event.phase == TickEvent.Phase.START) return
        FCC.withScope { scopeToOpen ->
            if (scopeToOpen != "") {
                ClientConfigRegistry.openScreen(scopeToOpen)
            }
        }

        FCC.withRestart { openRestartScreen ->
            if (openRestartScreen) {
                ConfigApiImplClient.openRestartScreen()
            }
        }
    }

    fun registerClient() {
        NeoForge.EVENT_BUS.addListener(this::registerCommands)
        NeoForge.EVENT_BUS.addListener(this::handleTick)
    }

    private fun registerCommands(event: RegisterClientCommandsEvent) {
        registerClientCommands(event.dispatcher)
    }

    private fun registerClientCommands(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            CommandManager.literal("configure")
                .then(CommandManager.argument("base_scope", ValidScopesArgumentType())
                    .executes{ context ->
                        val scope = ValidScopesArgumentType.getValidScope(context, "base_scope")
                        FCC.openScopedScreen(scope ?: "")
                        1
                    }
                    .then(
                        CommandManager.argument("sub_scope", ValidSubScopesArgumentType())
                        .executes{ context ->
                            val scope = ValidScopesArgumentType.getValidScope(context, "base_scope")
                            val subScope = ValidSubScopesArgumentType.getValidSubScope(context, "sub_scope")
                            FCC.openScopedScreen(scope?.plus(subScope?.let { ".$it" } ?: "") ?: "")
                            1
                        }
                    )
                )
        )
    }
}