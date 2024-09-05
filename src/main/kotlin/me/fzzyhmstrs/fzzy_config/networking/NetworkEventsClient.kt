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
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImplClient
import me.fzzyhmstrs.fzzy_config.impl.ValidScopesArgumentType
import me.fzzyhmstrs.fzzy_config.impl.ValidSubScopesArgumentType
import me.fzzyhmstrs.fzzy_config.registry.ClientConfigRegistry
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.MinecraftClient
import java.util.*

internal object NetworkEventsClient {

    fun forwardSetting(update: String, player: UUID, scope: String, summary: String) {
        if (!ClientPlayNetworking.canSend(SettingForwardCustomPayload.type)) {
            MinecraftClient.getInstance().player?.sendMessage("fc.config.forwarded_error.c2s".translate())
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

    fun registerClient() {

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
                }
            }
        }

        ClientPlayNetworking.registerGlobalReceiver(ConfigPermissionsS2CCustomPayload.type) { payload, _ ->
            ClientConfigRegistry.receivePerms(payload.id, payload.permissions)
        }


        ClientConfigurationNetworking.registerGlobalReceiver(ConfigSyncS2CCustomPayload.type) { payload, handler ->
            ClientConfigRegistry.receiveSync(
                payload.id,
                payload.serializedConfig
            ) { text -> handler.responseSender().disconnect(text) }
        }

        ClientPlayNetworking.registerGlobalReceiver(ConfigUpdateS2CCustomPayload.type) { payload, context ->
            ClientConfigRegistry.receiveUpdate(payload.updates, context.player())
        }

        ClientPlayNetworking.registerGlobalReceiver(SettingForwardCustomPayload.type) { payload, _ ->
            ClientConfigRegistry.handleForwardedUpdate(payload.update, payload.player, payload.scope, payload.summary)
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
    }
}