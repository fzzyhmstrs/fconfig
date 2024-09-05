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

import com.google.common.collect.HashMultimap
import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.config.ConfigContext.Keys.ACTIONS
import me.fzzyhmstrs.fzzy_config.config.ConfigContext.Keys.RESTART_RECORDS
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.impl.ConfigSet
import me.fzzyhmstrs.fzzy_config.screen.internal.ConfigScreenManager
import me.fzzyhmstrs.fzzy_config.updates.UpdateManager
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.PlatformUtils
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.text.Text
import java.util.*
import java.util.function.Consumer

/**
 * Client registry for [Config] instances. Handles GUIs.
 *
 * This is not a "true" registry in the Minecraft since; as such there are not the typical helper methods like get(), getId(), etc. This registry's scope is much narrower, handling synchronization and updates of Configs.
 */
//client
internal object ClientConfigRegistry {

    private val clientConfigs : MutableMap<String, ConfigPair> = mutableMapOf()
    private val configScreenManagers: MutableMap<String, ConfigScreenManager> = mutableMapOf()
    private val customPermissions: MutableMap<String, Map<String, Boolean>> = mutableMapOf()
    private var validScopes: MutableSet<String> = mutableSetOf() //configs are sorted into Managers by namespace
    private var validSubScopes: HashMultimap<String, String> = HashMultimap.create()
    private var hasScrapedMetadata = false

    //client
    internal fun receiveSync(id: String, configString: String, disconnector: Consumer<Text>) {
        if (SyncedConfigRegistry.syncedConfigs().containsKey(id)) {
            val config = SyncedConfigRegistry.syncedConfigs()[id] ?: return
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
                    disconnector.accept(FcText.translatable("fc.networking.restart"))
                    ConfigApiImpl.openRestartScreen()
                }
            }
        }
    }

    //client
    internal fun receivePerms(id: String, perms: Map<String, Boolean>) {
        updatePerms(id, perms)
    }

    //client
    internal fun receiveUpdate(serializedConfigs: Map<String, String>, player: PlayerEntity) {
        for ((id, configString) in serializedConfigs) {
            if (SyncedConfigRegistry.syncedConfigs().containsKey(id)) {
                val config = SyncedConfigRegistry.syncedConfigs()[id] ?: return
                val errors = mutableListOf<String>()
                val result = ConfigApiImpl.deserializeUpdate(config, configString, errors, ConfigApiImpl.CHECK_ACTIONS)
                val actions = result.get().getOrDefault(ACTIONS, setOf())
                result.writeError(errors)
                result.get().config.save()
                for (action in actions) {
                    player.sendMessage(action.clientPrompt)
                }
            }
        }
    }

    //client
    internal fun getScreenScopes(): Set<String> {
        if (!hasScrapedMetadata) {
            val set = mutableSetOf(*validScopes.toTypedArray())
            for (scope in PlatformUtils.customScopes()) {
                set.add(scope)
            }
            hasScrapedMetadata = true
            return set.toSet()
        } else {
            return validScopes
        }
    }

    //client
    internal fun getSubScreenScopes(parentScope: String): Set<String> {
        return validSubScopes.get(parentScope)
    }

    //client
    internal fun openScreen(scope: String) {
        val namespaceScope = getValidScope(scope)
        if (namespaceScope == null) {
            FC.LOGGER.error("Failed to open a FzzyConfig screen. Invalid scope provided: [$scope]")
            return
        }
        val manager = configScreenManagers.computeIfAbsent(namespaceScope) {
            ConfigScreenManager(
                namespaceScope,
                clientConfigs.filterKeys { s -> s.startsWith(namespaceScope) }.map { ConfigSet(it.value.active, it.value.base, !SyncedConfigRegistry.hasConfig(it.key)) })
        }
        manager.openScreen(scope)
    }

    //client
    internal fun provideScreen(scope: String): Screen? {
        val namespaceScope = getValidScope(scope)
        if (namespaceScope == null) {
            FC.LOGGER.error("Failed to provide a FzzyConfig screen. Invalid scope provided: [$scope]")
            return null
        }
        val manager = configScreenManagers.computeIfAbsent(namespaceScope) {
            ConfigScreenManager(
                namespaceScope,
                clientConfigs.filterKeys { s -> s.startsWith(namespaceScope) }.map { ConfigSet(it.value.active, it.value.base, !SyncedConfigRegistry.hasConfig(it.key)) })
        }
        return manager.provideScreen(scope)
    }

    //client
    internal fun getPerms(): Map<String, Map<String, Boolean>> {
        return HashMap(customPermissions)
    }

    //client
    internal fun updatePerms(id: String, perms: Map<String, Boolean>) {
        customPermissions[id] = perms
        println("received perms")
        println(customPermissions)
    }

    //client
    internal fun handleForwardedUpdate(update: String, player: UUID, scope: String, summary: String) {
        val namespaceScope = getValidScope(scope)
        if (namespaceScope == null) {
            FC.LOGGER.error("Failed to handle a forwarded setting. Invalid scope provided: [$scope]")
            return
        }
        val manager = configScreenManagers[namespaceScope]
        if (manager == null) {
            FC.LOGGER.error("Failed to handle a forwarded setting. Unknown scope provided: [$scope]")
            return
        }
        manager.receiveForwardedUpdate(update, player, scope, summary)
    }

    //client
    private fun getValidScope(scope: String): String? {
        if(validScopes.contains(scope)) return scope
        var validScopeTry = scope.substringBeforeLast('.')
        if (validScopeTry == scope) return null
        while(!validScopes.contains(validScopeTry) && validScopeTry.contains('.')) {
            validScopeTry = validScopeTry.substringBeforeLast('.')
        }
        return if(validScopes.contains(validScopeTry)) validScopeTry else null
    }

    //client
    internal fun registerConfig(config: Config, baseConfig: Config) {
        validScopes.add(config.getId().namespace)
        validSubScopes.put(config.getId().namespace, config.getId().path)
        UpdateManager.applyKeys(config)
        clientConfigs[config.getId().toTranslationKey()] = ConfigPair(config, baseConfig)
    }

    private class ConfigPair(val active: Config, val base: Config)
}