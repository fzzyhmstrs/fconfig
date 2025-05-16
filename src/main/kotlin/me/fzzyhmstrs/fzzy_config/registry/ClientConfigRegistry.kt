/*
* Copyright (c) 2024-2025 Fzzyhmstrs
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
import me.fzzyhmstrs.fzzy_config.api.SaveType
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.config.ConfigContext.Keys.ACTIONS
import me.fzzyhmstrs.fzzy_config.config.ConfigContext.Keys.RESTART_ACTIONS
import me.fzzyhmstrs.fzzy_config.config.ConfigContext.Keys.RESTART_RECORDS
import me.fzzyhmstrs.fzzy_config.event.impl.EventApiImpl
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.impl.ConfigSet
import me.fzzyhmstrs.fzzy_config.screen.ConfigScreenProvider
import me.fzzyhmstrs.fzzy_config.screen.internal.ConfigBaseUpdateManager
import me.fzzyhmstrs.fzzy_config.screen.internal.ConfigScreenManager
import me.fzzyhmstrs.fzzy_config.updates.UpdateManager
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.PortingUtils.sendChat
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.platform.impl.PlatformUtils
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.text.Text
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
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
    private var validScopes: MutableSet<String> = Collections.synchronizedSet(mutableSetOf()) //configs are sorted into Managers by namespace
    private var validSubScopes: HashMultimap<String, String> = HashMultimap.create()
    private var validCustomScopes: MutableSet<String> = mutableSetOf()
    private var hasScrapedMetadata: AtomicBoolean = AtomicBoolean(false)
    private val screenProviders: HashMultimap<String, ConfigScreenProvider> = HashMultimap.create()

    internal fun hasClientConfig(scope: String): Boolean {
        return getClientConfig(scope) != null
    }

    internal fun getClientConfig(scope: String): Config? {
        return clientConfigs[scope]?.active
    }

    internal fun getValidClientConfig(scope: String): Pair<Config?, String> {
        val s = getValidConfigScope(scope) ?: return Pair(null, scope)
        return Pair(clientConfigs[s]?.active, s)
    }

    private fun getValidConfigScope(scope: String): String? {
        val configScopes = clientConfigs.keys
        if(configScopes.contains(scope)) return scope
        var validScopeTry = scope.substringBeforeLast('.')
        if (validScopeTry == scope) return null
        while(!configScopes.contains(validScopeTry) && validScopeTry.contains('.')) {
            validScopeTry = validScopeTry.substringBeforeLast('.')
        }
        return if(configScopes.contains(validScopeTry)) validScopeTry else null
    }

    //client
    internal fun receiveSync(id: String, configString: String, disconnector: Consumer<Text>) {
        if (SyncedConfigRegistry.syncedConfigs().containsKey(id)) {
            val configEntry = SyncedConfigRegistry.syncedConfigs()[id] ?: return
            val result = ConfigApiImpl.deserializeConfigSafe(configEntry.config, configString, "Error(s) encountered receiving sync for $id from server", ConfigApiImpl.CHECK_ACTIONS_AND_RECORD_RESTARTS).log(ValidationResult.ErrorEntry.ENTRY_ERROR_LOGGER) //0: Don't ignore NonSync on a synchronization action, 2: Watch for RequiresRestart
            MinecraftClient.getInstance().execute {
                getValidScope(id)?.let { //invalidate screen manager to refresh the state of widgets there. Should upgrade this functionality in the future.
                    configScreenManagers.remove(it)
                }
                val saveType = result.get().saveType()
                if (saveType == SaveType.OVERWRITE)
                    result.get().save() //save config to the client
                if (result.test(ValidationResult.Errors.ACTION) { it.content.restartPrompt }) {
                    if (result.has(ValidationResult.Errors.RESTART)) {
                        FC.LOGGER.info("Client prompted for a restart due to received config updates")
                        FC.LOGGER.info("Restart-prompting updates:")
                        for (record in result.iterate(ValidationResult.Errors.RESTART)) {
                            record.log(ValidationResult.ErrorEntry.ENTRY_INFO_LOGGER)
                        }
                    }
                    disconnector.accept(FcText.translatable("fc.networking.restart"))
                    ConfigApiImpl.openRestartScreen()
                } else {
                    try {
                        configEntry.config.onSyncClient()
                    } catch (e: Throwable) {
                        FC.LOGGER.error("Error encountered with onSyncClient method of config $id!", e)
                    }
                    try {
                        EventApiImpl.fireOnSyncClient(result.get().getId(), result.get())
                    } catch (e: Throwable) {
                        FC.LOGGER.error("Error encountered while running onSyncClient event for config $id!", e)
                    }
                }
            }
        }
    }

    internal fun receiveReloadSync(id: String, configString: String, player: PlayerEntity) {
        if (SyncedConfigRegistry.syncedConfigs().containsKey(id)) {
            val configEntry = SyncedConfigRegistry.syncedConfigs()[id] ?: return
            val errors = mutableListOf<String>()
            val result = ConfigApi.deserializeConfig(configEntry.config, configString, errors, ConfigApiImpl.CHECK_ACTIONS_AND_RECORD_RESTARTS) //0: Don't ignore NonSync on a synchronization action, 2: Watch for RequiresRestart
            val actions = result.get().getOrDefault(ACTIONS, setOf())
            result.writeError(errors)
            val saveType = result.get().config.saveType()
            if (saveType == SaveType.OVERWRITE)
                result.get().config.save()//save config to the client
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
                }
            }
            for (action in actions) {
                player.sendChat(action.clientPrompt)
            }
        }
    }

    //client
    internal fun receivePerms(id: String, perms: Map<String, Boolean>) {
        updatePerms(id, perms)
    }

    //client
    @Suppress("UNUSED_PARAMETER")
    internal fun receiveUpdate(serializedConfigs: Map<String, String>, player: PlayerEntity) {
        for ((id, configString) in serializedConfigs) {
            if (SyncedConfigRegistry.syncedConfigs().containsKey(id)) {
                val configEntry = SyncedConfigRegistry.syncedConfigs()[id] ?: return
                val result = ConfigApiImpl.deserializeUpdate(configEntry.config, configString, "Error(s) encountered receiving update for $id from server", ConfigApiImpl.CHECK_ACTIONS).log(ValidationResult.ErrorEntry.ENTRY_ERROR_LOGGER)
                MinecraftClient.getInstance().execute {
                    getValidScope(id)?.let { //invalidate screen manager to refresh the state of widgets there. Should upgrade this functionality in the future.
                        configScreenManagers.remove(it)
                    }
                    val saveType = result.get().config.saveType()
                    if (saveType == SaveType.OVERWRITE)
                        result.get().config.save()
                    for (action in result.iterate(ValidationResult.Errors.ACTION)) {
                        MinecraftClient.getInstance().player?.sendChat(action.content.clientPrompt)
                    }
                    try {
                        configEntry.config.onUpdateClient()
                    } catch (e: Throwable) {
                        FC.LOGGER.error("Error encountered with onUpdateClient method of config $id while receiving an update from the server!", e)
                    }
                    try {
                        EventApiImpl.fireOnUpdateClient(result.get().config.getId(), result.get().config)
                    } catch (e: Throwable) {
                        FC.LOGGER.error("Error encountered while running onUpdateClient event for config $id while receiving an update from the server!", e)
                    }
                }
            }
        }
    }

    private fun scrape() {
        if (!hasScrapedMetadata.get()) {
            for (scope in PlatformUtils.customScopes()) {
                if (scope.isEmpty()) continue
                if (scope.contains(".")) {
                    val scopes = scope.split(".")
                    validCustomScopes.add(scopes[0])
                    if (scopes.size > 1)
                        validSubScopes.put(scopes[0], scopes[1])
                } else {
                    validCustomScopes.add(scope)
                }
            }
            hasScrapedMetadata.set(true)
        }
    }

    //client
    @Synchronized
    internal fun getScreenScopes(): Set<String> {
        scrape()
        return validScopes + validCustomScopes
    }

    //client
    internal fun getSubScreenScopes(parentScope: String): Set<String> {
        return validSubScopes.get(parentScope)
    }

    //client
    internal fun openScreen(scope: String) {
        val namespaceScope = getValidScope(scope, true)
        if (namespaceScope == null) {
            FC.LOGGER.error("Failed to open a FzzyConfig screen. Invalid scope provided: [$scope]")
            return
        }
        val providers = screenProviders[namespaceScope]
        if (providers.any { it.openScreen(namespaceScope, scope) }) {
            return
        }
        val manager = configScreenManagers.computeIfAbsent(namespaceScope) {
            ConfigScreenManager(
                namespaceScope,
                validSubScopes[namespaceScope].map { "$namespaceScope.$it" },
                clientConfigs.filterKeys {
                    s -> s.startsWith(namespaceScope)
                }.mapValues {
                    ConfigSet(it.value.active, it.value.base, !SyncedConfigRegistry.hasConfig(it.key), ConfigApiImpl.isRootConfig(it.value.active::class))
                })
        }
        manager.openScreen(scope)
    }

    internal fun isScreenOpen(scope: String): Boolean {
        val namespaceScope = getValidScope(scope)
        if (namespaceScope == null) {
            FC.LOGGER.error("Failed to determine if a config screen is open. Invalid scope provided: [$scope]")
            return false
        }
        val manager = configScreenManagers[namespaceScope] ?: return false
        return manager.isScreenOpen(scope)
    }

    internal fun registerScreenProvider(namespace: String, provider: ConfigScreenProvider) {
        validScopes.add(namespace)
        screenProviders.put(namespace, provider)
    }

    internal fun provideUpdateManager(scope: String): ConfigBaseUpdateManager? {
        val namespaceScope = getValidScope(scope, true)
        if (namespaceScope == null) {
            FC.LOGGER.error("Failed to provide an update manager. Invalid scope provided: [$scope]")
            return null
        }
        val manager = configScreenManagers[namespaceScope] ?: return null
        return manager.provideUpdateManager(scope)
    }

    //client
    internal fun provideScreen(scope: String): Screen? {
        val namespaceScope = getValidScope(scope, true)
        if (namespaceScope == null) {
            FC.LOGGER.error("Failed to provide a FzzyConfig screen. Invalid scope provided: [$scope]")
            return null
        }
        val providers = screenProviders[namespaceScope]
        val providedScreen = providers.firstNotNullOfOrNull { it.provideScreen(namespaceScope, scope) }
        if (providedScreen != null) {
            return providedScreen
        }
        val manager = configScreenManagers.computeIfAbsent(namespaceScope) {
            ConfigScreenManager(
                namespaceScope,
                validSubScopes[namespaceScope].map { "$namespaceScope.$it" },
                clientConfigs.filterKeys {
                    s -> s.startsWith(namespaceScope)
                }.mapValues {
                    ConfigSet(it.value.active, it.value.base, !SyncedConfigRegistry.hasConfig(it.key), ConfigApiImpl.isRootConfig(it.value.active::class))
                })
        }
        return manager.provideScreen(scope)
    }

    internal fun hasScreen(scope: String): Boolean {
        return getValidScope(scope) != null
    }

    //client
    internal fun getPerms(): Map<String, Map<String, Boolean>> {
        return HashMap(customPermissions)
    }

    //client
    internal fun getPermsRef(): Map<String, Map<String, Boolean>> {
        return customPermissions
    }

    //client
    internal fun updatePerms(id: String, perms: Map<String, Boolean>) {
        customPermissions[id] = perms
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
    private fun getValidScope(scope: String, useCustom: Boolean = false): String? {
        val scopes = if (useCustom) getScreenScopes() else validScopes
        if(scopes.contains(scope)) return scope
        var validScopeTry = scope.substringBeforeLast('.')
        if (validScopeTry == scope) return null
        while(!scopes.contains(validScopeTry) && validScopeTry.contains('.')) {
            validScopeTry = validScopeTry.substringBeforeLast('.')
        }
        return if(scopes.contains(validScopeTry)) validScopeTry else null
    }

    //client
    internal fun registerConfig(config: Config, baseConfig: Config, noGui: Boolean) {
        if (!noGui) {
            val namespace = config.getId().namespace
            validScopes.add(namespace)
            validSubScopes.put(namespace, config.getId().path)
            if (configScreenManagers.containsKey(namespace)) { //invalidate config screen manager
                configScreenManagers.remove(namespace)
            }
        }
        clientConfigs[config.getId().toTranslationKey()] = ConfigPair(config, baseConfig)
        EventApiImpl.fireOnRegisteredClient(config.getId(), config)
    }

    private class ConfigPair(private val _active: Config, val base: Config) {
        var i: Boolean = false

        val active: Config
            get() {
                if (!i) {
                    i = true
                    UpdateManager.applyKeys(_active)
                }
                return _active
            }
    }
}