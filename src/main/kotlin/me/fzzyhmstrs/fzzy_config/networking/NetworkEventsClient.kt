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
import me.fzzyhmstrs.fzzy_config.screen.context.ContextType
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.PortingUtils.sendChat
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedIdentifier
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen
import net.minecraft.client.realms.gui.screen.RealmsMainScreen
import net.minecraft.client.gui.screen.TitleScreen
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.util.Identifier
import net.neoforged.fml.ModList
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent
import net.neoforged.neoforge.client.event.ScreenEvent
import net.neoforged.neoforge.client.gui.IConfigScreenFactory
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.network.handling.IPayloadContext
import net.neoforged.neoforge.network.registration.NetworkRegistry
import java.util.*
import java.util.function.Supplier

internal object NetworkEventsClient {

    fun canSend(id: Identifier): Boolean {
        val handler = MinecraftClient.getInstance().networkHandler ?: return false
        return NetworkRegistry.hasChannel(handler, id)
    }

    fun forwardSetting(update: String, player: UUID, scope: String, summary: String) {
        if (!canSend(SettingForwardCustomPayload.type.id)) {
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
        if (!canSend(ConfigUpdateC2SCustomPayload.type.id)) {
            FC.LOGGER.error("Can't send Config Update; not connected to a server or server isn't accepting this type of data")
            FC.LOGGER.error("changes not sent:")
            for (change in changeHistory) {
                FC.LOGGER.warn(change)
            }
            return
        }
        ConfigApi.network().send(ConfigUpdateC2SCustomPayload(serializedConfigs, changeHistory, playerPerm), null)
    }

    fun handleConfigurationConfigSync(payload: ConfigSyncS2CCustomPayload, context: IPayloadContext) {
        ClientConfigRegistry.receiveSync(
            payload.id,
            payload.serializedConfig
        ) { text -> context.disconnect(text) }
    }

    fun handleReloadConfigSync(payload: ConfigSyncS2CCustomPayload, context: IPayloadContext) {
        ClientConfigRegistry.receiveReloadSync(
            payload.id,
            payload.serializedConfig,
            context.player()
        )
    }

    fun handleFcPermsUpdate(payload: ConfigPermissionsS2CCustomPayload, context: ClientPlayNetworkContext) {
        ClientConfigRegistry.receivePerms(payload.id, payload.permissions)
    }

    fun handlePermsUpdate(payload: ConfigPermissionsS2CCustomPayload, context: IPayloadContext) {
        ClientConfigRegistry.receivePerms(payload.id, payload.permissions)
    }

    fun handleUpdate(payload: ConfigUpdateS2CCustomPayload, context: IPayloadContext) {
        ClientConfigRegistry.receiveUpdate(payload.updates, context.player())
    }

    fun handleSettingForward(payload: SettingForwardCustomPayload, context: IPayloadContext) {
        ClientConfigRegistry.handleForwardedUpdate(payload.update, payload.player, payload.scope, payload.summary)
    }

    fun receiveDynamicIds(payload: DynamicIdsS2CCustomPayload, context: IPayloadContext) {
        ValidatedIdentifier.receiveSync(payload)
    }

    private fun handleTick(event: ClientTickEvent.Post) {
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

    private var initialized = false

    private fun registerConfigs(event: ScreenEvent.Init.Pre) {
        if (event.screen !is TitleScreen || initialized) return
        ModList.get().forEachModInOrder { modContainer ->
            val id = modContainer.modId
            if (ClientConfigRegistry.getScreenScopes().contains(id)) {
                if (modContainer.getCustomExtension(IConfigScreenFactory::class.java).isEmpty) {
                    modContainer.registerExtensionPoint(IConfigScreenFactory::class.java, Supplier {
                        IConfigScreenFactory { _, screen -> ClientConfigRegistry.provideScreen(id) ?: screen }
                    })
                }
            }
        }
        initialized = true
    }

    fun registerClient() {
        ContextType.init()
        NeoForge.EVENT_BUS.addListener(this::registerCommands)
        NeoForge.EVENT_BUS.addListener(this::registerConfigs)
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
        dispatcher.register(
            CommandManager.literal("fzzy_config_restart")
                .executes{ context ->
                    MinecraftClient.getInstance().scheduleStop()
                    1
                }
        )
        dispatcher.register(
            CommandManager.literal("fzzy_config_leave_game")
                .executes{ context ->
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
            CommandManager.literal("fzzy_config_reload_resources")
                .executes{ context ->
                    MinecraftClient.getInstance().reloadResources()
                    1
                }
        )
    }
}