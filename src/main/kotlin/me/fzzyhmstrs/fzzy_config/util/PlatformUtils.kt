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
import me.fzzyhmstrs.fzzy_config.registry.ClientConfigRegistry
import me.fzzyhmstrs.fzzy_config.registry.SyncedConfigRegistry
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.lucko.fabric.api.permissions.v0.Permissions
import net.fabricmc.api.EnvType
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.ModContainer
import net.fabricmc.loader.api.metadata.CustomValue
import net.minecraft.client.gui.screen.Screen
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.resource.ResourceManager
import net.minecraft.resource.ResourceType
import net.minecraft.resource.SynchronousResourceReloader
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.util.Identifier
import java.io.File
import java.util.function.BiFunction

internal object PlatformUtils {

    fun isClient(): Boolean {
        return FabricLoader.getInstance().environmentType == EnvType.CLIENT
    }

    fun configDir(): File {
        return FabricLoader.getInstance().configDir.toFile()
    }

    fun gameDir(): File {
        return FabricLoader.getInstance().gameDir.toFile()
    }

    fun isModLoaded(mod: String): Boolean {
        return FabricLoader.getInstance().isModLoaded(mod)
    }

    fun configName(scope: String, fallback: String): String {
        return FabricLoader.getInstance().getModContainer(scope)?.get()?.metadata?.name ?: fallback
    } //ConfigScreenManager

    fun customScopes(): List<String> {
        val list: MutableList<String> = mutableListOf()
        for (container in FabricLoader.getInstance().allMods) {
            val customValue = container.metadata.getCustomValue("fzzy_config") ?: continue
            if (customValue.type != CustomValue.CvType.ARRAY) continue
            val arrayValue = customValue.asArray
            for (thing in arrayValue) {
                if (thing.type != CustomValue.CvType.STRING) continue
                list.add(thing.asString)
            }
        }
        return list
    } //ClientConfigRegistry

    fun hasPermission(player: PlayerEntity, permission: String): Boolean {
        return Permissions.check(player, permission)
    } //COnfigApiImpl, elsewhere??

    @JvmStatic
    fun createConfigFactoryProvider(): Map<String, BiFunction<Screen, ModContainer, Screen?>> {
        return ClientConfigRegistry.getScreenScopes().associateWith { scope -> BiFunction { _: Screen, _: ModContainer -> ClientConfigRegistry.provideScreen(scope) } }
    }

    fun registerClientReloadListener(id: Identifier, listener: SynchronousResourceReloader) {
        if (listener !is IdentifiableResourceReloadListener) {
            ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(object: SimpleSynchronousResourceReloadListener {

                override fun reload(manager: ResourceManager?) {
                    listener.reload(manager)
                }

                override fun getFabricId(): Identifier {
                    return id
                }

            })
        } else {
            ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(listener)
        }
    }

    fun registerServerReloadListener(id: Identifier, listener: SynchronousResourceReloader) {
        if (listener !is IdentifiableResourceReloadListener) {
            ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(object: SimpleSynchronousResourceReloadListener {

                override fun reload(manager: ResourceManager?) {
                    listener.reload(manager)
                }

                override fun getFabricId(): Identifier {
                    return id
                }

            })
        } else {
            ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(listener)
        }
    }

    fun registerCommands() {
        /*ArgumentTypeRegistry.registerArgumentType(
            Identifier.of(FC.MOD_ID, "quarantined_updates"),
            QuarantinedUpdatesArgumentType::class.java,
            ConstantArgumentSerializer.of { _ -> QuarantinedUpdatesArgumentType() }
        )*/

        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            registerCommands(dispatcher)
        }
    }

    private fun registerCommands(dispatcher: CommandDispatcher<ServerCommandSource>) {

        dispatcher.register(
            CommandManager.literal("configure_update")
                .requires { source -> source.hasPermissionLevel(3) }
                .then(CommandManager.argument("id", StringArgumentType.string())
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