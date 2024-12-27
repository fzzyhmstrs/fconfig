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
import me.fzzyhmstrs.fzzy_config.networking.api.ClientPlayNetworkContext
import me.fzzyhmstrs.fzzy_config.registry.ClientConfigRegistry
import me.fzzyhmstrs.fzzy_config.screen.context.ContextHandler
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.PortingUtils.sendChat
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedIdentifier
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.TitleScreen
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen
import net.minecraft.client.realms.gui.screen.RealmsMainScreen
import java.util.*

internal object NetworkEventsClient {

    fun forwardSetting(update: String, player: UUID, scope: String, summary: String) {
        if (!ConfigApi.network().canSend(SettingForwardCustomPayload.type.id, null)) {
            MinecraftClient.getInstance().player?.sendChat("fc.config.forwarded_error.c2s".translate())
            FC.LOGGER.error("Can't forward setting; not connected to a server or server isn't accepting this type of data")
            FC.LOGGER.error("Setting not sent:")
            FC.LOGGER.warn(scope)
            FC.LOGGER.warn(summary)
            return
        }
        ConfigApi.network().send(SettingForwardCustomPayload(update, player, scope, summary), null)
    }

    fun updateServer(serializedConfigs: Map<String, String>, changeHistory: List<String>, playerPerm: Int) {
        if (!ConfigApi.network().canSend(ConfigUpdateC2SCustomPayload.type.id, null)) {
            FC.LOGGER.error("Can't send Config Update; not connected to a server or server isn't accepting this type of data")
            FC.LOGGER.error("changes not sent:")
            for (change in changeHistory) {
                FC.LOGGER.warn(change)
            }
            return
        }
        ConfigApi.network().send(ConfigUpdateC2SCustomPayload(serializedConfigs, changeHistory, playerPerm), null)
    }

    fun receiveSync(payload: ConfigSyncS2CCustomPayload, context: ClientPlayNetworkContext) {
        ClientConfigRegistry.receiveSync(
            payload.id,
            payload.serializedConfig
        ) { text -> context.disconnect(text) }
    }

    fun receivePerms(payload: ConfigPermissionsS2CCustomPayload, context: ClientPlayNetworkContext) {
        ClientConfigRegistry.receivePerms(payload.id, payload.permissions)
    }

    fun receiveUpdate(payload: ConfigUpdateS2CCustomPayload, context: ClientPlayNetworkContext) {
        ClientConfigRegistry.receiveUpdate(payload.updates, context.player())
    }

    fun receiveForward(payload: SettingForwardCustomPayload, context: ClientPlayNetworkContext) {
        ClientConfigRegistry.handleForwardedUpdate(payload.update, payload.player, payload.scope, payload.summary)
    }

    fun receiveDynamicIds(payload: DynamicIdsS2CCustomPayload, context: ClientPlayNetworkContext) {
        ValidatedIdentifier.receiveSync(payload)
    }

    fun registerClient() {

        ContextHandler.init()

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            registerClientCommands(dispatcher)
        }

        ClientTickEvents.START_CLIENT_TICK.register { _ ->
            FCC.withScope { scopeToOpen ->
                if (scopeToOpen != "") {
                    ClientConfigRegistry.openScreen(scopeToOpen)
                }
            }

            FCC.withRestart { openRestartScreen ->
                if (openRestartScreen) {
                    ConfigApiImplClient.openRestartScreen()
                } else
                    false
            }
        }

        ClientConfigurationNetworking.registerGlobalReceiver(ConfigSyncS2CCustomPayload.type) { payload, handler ->
            ClientConfigRegistry.receiveSync(
                payload.id,
                payload.serializedConfig
            ) { text -> handler.responseSender().disconnect(text) }
        }
    }

    private fun registerClientCommands(dispatcher: CommandDispatcher<FabricClientCommandSource>) {
        dispatcher.register(
            ClientCommandManager.literal("configure")
                .then(ClientCommandManager.argument("base_scope", ValidScopesArgumentType())
                    .executes{ context ->
                        val scope = ValidScopesArgumentType.getValidScope(context, "base_scope")
                        FCC.openScopedScreen(scope ?: "")
                        1
                    }
                    .then(
                        ClientCommandManager.argument("sub_scope", ValidSubScopesArgumentType())
                        .executes{ context ->
                            val scope = ValidScopesArgumentType.getValidScope(context, "base_scope")
                            val subScope = ValidSubScopesArgumentType.getValidSubScope(context, "sub_scope")
                            FCC.openScopedScreen(scope?.plus(subScope?.let { ".$it" } ?: "") ?: "")
                            1
                        }
                    )
                )
        )
        dispatcher.register(
            ClientCommandManager.literal("fzzy_config_restart")
                .executes{ _ ->
                    MinecraftClient.getInstance().scheduleStop()
                    1
                }
        )
        dispatcher.register(
            ClientCommandManager.literal("fzzy_config_leave_game")
                .executes{ _ ->
                    val c = MinecraftClient.getInstance()
                    val sp = c.isInSingleplayer
                    val serverInfo = c.currentServerEntry
                    c.world?.disconnect()
                    c.disconnect()
                    val titleScreen = TitleScreen()
                    if (sp) {
                        c.setScreen(titleScreen)
                    } else if (serverInfo != null && serverInfo.isRealm) {
                        c.setScreen(RealmsMainScreen(titleScreen))
                    } else {
                        c.setScreen(MultiplayerScreen(titleScreen))
                    }
                    1
                }
        )
        dispatcher.register(
            ClientCommandManager.literal("fzzy_config_reload_resources")
                .executes{ _ ->
                    MinecraftClient.getInstance().reloadResources()
                    1
                }
        )
    }
}