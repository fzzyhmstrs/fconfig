/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.util

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import me.fzzyhmstrs.fzzy_config.nullCast
import me.fzzyhmstrs.fzzy_config.registry.SyncedConfigRegistry
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.RegisterCommandsEvent
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.fml.ModList
import net.minecraftforge.fml.loading.FMLPaths
import net.minecraftforge.forgespi.Environment
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.server.permission.PermissionAPI
import java.io.File

internal object PlatformUtils {

    fun isClient(): Boolean {
        return Environment.get().dist == Dist.CLIENT
    }

    fun configDir(): File {
        return FMLPaths.CONFIGDIR.get().toFile()
    }

    fun configName(scope: String, fallback: String): String {
        return ModList.get().getModContainerById(scope).map { it.modInfo.displayName }.orElse(fallback)
    } //ConfigScreenManager

    //only one entry allowed
    fun customScopes(): List<String> {
        val list: MutableList<String> = mutableListOf()
        for (container in ModList.get().mods) {
            val customValue = container.modProperties["fzzy_config"]
            if (customValue is String) {
                list.add(customValue)
            } else if (customValue is List<*>) {
                val values = customValue.nullCast<List<String>>() ?: continue
                list.addAll(values)
            }
        }
        return list
    } //ClientConfigRegistry

    fun hasPermission(player: ServerPlayerEntity, permission: String): Boolean {
        val node = PermissionAPI.getRegisteredNodes().firstOrNull { it.nodeName == permission } ?: return false
        return PermissionAPI.getPermission(player, node) == true
    } //COnfigApiImpl, elsewhere??

    fun registerCommands() {
        MinecraftForge.EVENT_BUS.addListener { event: RegisterCommandsEvent -> registerCommands(event) }
        //val commandArgumentTypes = DeferredRegister.create(RegistryKeys.COMMAND_ARGUMENT_TYPE, FC.MOD_ID)
        //commandArgumentTypes.register(bus)
        //commandArgumentTypes.register("quarantined_updates", Supplier { ArgumentTypes.registerByClass(QuarantinedUpdatesArgumentType::class.java, ConstantArgumentSerializer.of { _ -> QuarantinedUpdatesArgumentType() })  })
    }

    internal fun registerCommands(event: RegisterCommandsEvent) {
        registerCommands(event.dispatcher)
    }

    private fun registerCommands(dispatcher: CommandDispatcher<ServerCommandSource>) {

        dispatcher.register(
            CommandManager.literal("configure_update")
                .requires { source -> source.hasPermissionLevel(3) }
                .then(CommandManager.argument("id", StringArgumentType.string())
                    .then(
                        CommandManager.literal("list")
                        .executes { context ->
                            SyncedConfigRegistry.listQuarantines { message -> context.source.sendMessage(message) }
                            1
                        }
                    )
                    .then(
                        CommandManager.literal("inspect")
                        .executes { context ->
                            val id = StringArgumentType.getString(context, "id")
                            if (id == null) {
                                context.source.sendError("fc.command.error.no_id".translate())
                                return@executes 0
                            }
                            SyncedConfigRegistry.inspectQuarantine(id, { uuid -> context.source.server.playerManager.getPlayer(uuid)?.name }, { message -> context.source.sendMessage(message) })
                            1
                        }
                    )
                    .then(
                        CommandManager.literal("accept")
                        .executes { context ->
                            val id = StringArgumentType.getString(context, "id")
                            if (id == null) {
                                context.source.sendError("fc.command.error.no_id".translate())
                                return@executes 0
                            }
                            SyncedConfigRegistry.acceptQuarantine(id, context.source.server)
                            context.source.sendFeedback({ "fc.command.accepted".translate(id) }, true)
                            1
                        }
                    )
                    .then(
                        CommandManager.literal("reject")
                        .executes { context ->
                            val id = StringArgumentType.getString(context, "id")
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