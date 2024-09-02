/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config

import com.mojang.brigadier.CommandDispatcher
import com.nimbusds.openid.connect.sdk.assurance.claims.ISO3166_1Alpha2CountryCode.MC
import me.fzzyhmstrs.fzzy_config.impl.*
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImplClient
import me.fzzyhmstrs.fzzy_config.impl.QuarantinedUpdatesArgumentType
import me.fzzyhmstrs.fzzy_config.impl.ValidScopesArgumentType
import me.fzzyhmstrs.fzzy_config.impl.ValidSubScopesArgumentType
import me.fzzyhmstrs.fzzy_config.registry.ClientConfigRegistry
import me.fzzyhmstrs.fzzy_config.registry.SyncedConfigRegistry
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.util.Identifier
import org.jetbrains.annotations.ApiStatus.Internal
import org.slf4j.Logger
import org.slf4j.LoggerFactory

inline fun<reified T> Any?.cast(): T {
    return this as T
}

inline fun<reified T> Any?.nullCast(): T? {
    return this as? T
}

@Internal
object FC: ModInitializer {
    internal const val MOD_ID = "fzzy_config"
    internal val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)

    override fun onInitialize() {
        SyncedConfigRegistry.registerAll()

        ArgumentTypeRegistry.registerArgumentType(
            Identifier.of(MOD_ID, "quarantined_updates"),
            QuarantinedUpdatesArgumentType::class.java,
            ConstantArgumentSerializer.of { _ -> QuarantinedUpdatesArgumentType() }
        )

        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            registerCommands(dispatcher)
        }
    }

    private fun registerCommands(dispatcher: CommandDispatcher<ServerCommandSource>) {

        dispatcher.register(
            CommandManager.literal("configure_update")
                .requires { source -> source.hasPermissionLevel(3) }
                .then(CommandManager.argument("id", QuarantinedUpdatesArgumentType())
                    .then(CommandManager.literal("inspect")
                        .executes { context ->
                            val id = QuarantinedUpdatesArgumentType.getQuarantineId(context, "id")
                            if (id == null) {
                                context.source.sendError("fc.command.error.no_id".translate())
                                return@executes 0
                            }
                            SyncedConfigRegistry.inspectQuarantine(id, { uuid -> context.source.server.playerManager.getPlayer(uuid)?.name }, { message -> context.source.sendMessage(message) })
                            1
                        }
                    )
                    .then(CommandManager.literal("accept")
                        .executes { context ->
                            val id = QuarantinedUpdatesArgumentType.getQuarantineId(context, "id")
                            if (id == null) {
                                context.source.sendError("fc.command.error.no_id".translate())
                                return@executes 0
                            }
                            SyncedConfigRegistry.acceptQuarantine(id, context.source.server)
                            context.source.sendFeedback({ "fc.command.accepted".translate(id) }, true)
                            1
                        }
                    )
                    .then(CommandManager.literal("reject")
                        .executes { context ->
                            val id = QuarantinedUpdatesArgumentType.getQuarantineId(context, "id")
                            if (id == null) {
                                context.source.sendError("fc.command.error.no_id".translate())
                                return@executes 0
                            }
                            SyncedConfigRegistry.rejectQuarantine(id, context.source.server)
                            context.source.sendFeedback({ "fc.command.rejected".translate(id) }, true)
                            1
                        }
                    )
                )
        )
    }
}


@Internal
object FCC: ClientModInitializer {

    private var scopeToOpen = ""
    private var openRestartScreen = false

    override fun onInitializeClient() {
        SyncedConfigRegistry.registerClient()
        ClientCommandRegistrationCallback.EVENT.register{ dispatcher, _ ->
            registerClientCommands(dispatcher)
        }
        ClientTickEvents.START_CLIENT_TICK.register{ _ ->
            if (scopeToOpen != "") {
                ClientConfigRegistry.openScreen(scopeToOpen)
                scopeToOpen = ""
            }
            if (openRestartScreen) {
                ConfigApiImplClient.openRestartScreen()
            }
        }
    }

    fun openRestartScreen() {
        this.openRestartScreen = true
    }

    private fun registerClientCommands(dispatcher: CommandDispatcher<FabricClientCommandSource>) {
        dispatcher.register(
            ClientCommandManager.literal("configure")
                .then(ClientCommandManager.argument("base_scope", ValidScopesArgumentType())
                    .executes{ context ->
                        val scope = ValidScopesArgumentType.getValidScope(context, "base_scope")
                        scopeToOpen = scope ?: ""
                        1
                    }
                    .then(ClientCommandManager.argument("sub_scope", ValidSubScopesArgumentType())
                        .executes{ context ->
                            val scope = ValidScopesArgumentType.getValidScope(context, "base_scope")
                            val subScope = ValidSubScopesArgumentType.getValidSubScope(context, "sub_scope")
                            scopeToOpen = scope?.plus(subScope?.let { ".$it" } ?: "") ?: ""
                            1
                        }
                    )
                )
        )
    }
}

internal fun String.fcId(): Identifier {
    return Identifier(FC.MOD_ID, this)
}