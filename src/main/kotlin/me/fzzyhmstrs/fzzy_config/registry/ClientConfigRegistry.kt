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
import com.google.common.collect.Multimap
import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.impl.ConfigSet
import me.fzzyhmstrs.fzzy_config.screen.internal.ConfigScreenManager
import me.fzzyhmstrs.fzzy_config.updates.UpdateManager
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.metadata.CustomValue
import net.minecraft.client.gui.screen.Screen
import java.util.*
import kotlin.collections.HashMap

/**
 * Client registry for [Config] instances. Handles GUIs.
 *
 * This is not a "true" registry in the Minecraft since; as such there are not the typical helper methods like get(), getId(), etc. This registry's scope is much narrower, handling synchronization and updates of Configs.
 */
@Environment(EnvType.CLIENT)
internal object ClientConfigRegistry {

    private val clientConfigs : MutableMap<String, ConfigPair> = mutableMapOf()
    private val configScreenManagers: MutableMap<String, ConfigScreenManager> = mutableMapOf()
    private val customPermissions: MutableMap<String, Map<String, Boolean>> = mutableMapOf()
    private var validScopes: MutableSet<String> = mutableSetOf() //configs are sorted into Managers by namespace
    private var validSubScopes: HashMultimap<String, String> = HashMultimap.create()
    private var hasScrapedMetadata = false

    @Environment(EnvType.CLIENT)
    internal fun getScreenScopes(): Set<String> {
        if (!hasScrapedMetadata) {
            val set = mutableSetOf(*validScopes.toTypedArray())
            for (container in FabricLoader.getInstance().allMods) {
                val customValue = container.metadata.getCustomValue("fzzy_config") ?: continue
                if (customValue.type != CustomValue.CvType.ARRAY) continue
                val arrayValue = customValue.asArray
                for (thing in arrayValue) {
                    if (thing.type != CustomValue.CvType.STRING) continue
                    set.add(thing.asString)
                }
            }
            hasScrapedMetadata = true
            return set.toSet()
        } else {
            return validScopes
        }
    }

    @Environment(EnvType.CLIENT)
    internal fun getSubScreenScopes(parentScope: String): Set<String> {
        return validSubScopes.get(parentScope)
    }

    @Environment(EnvType.CLIENT)
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

    @Environment(EnvType.CLIENT)
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

    @Environment(EnvType.CLIENT)
    internal fun getPerms(): Map<String, Map<String, Boolean>> {
        return HashMap(customPermissions)
    }

    @Environment(EnvType.CLIENT)
    internal fun updatePerms(id: String, perms: Map<String, Boolean>) {
        customPermissions[id] = perms
    }

    @Environment(EnvType.CLIENT)
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

    @Environment(EnvType.CLIENT)
    private fun getValidScope(scope: String): String? {
        if(validScopes.contains(scope)) return scope
        var validScopeTry = scope.substringBeforeLast('.')
        if (validScopeTry == scope) return null
        while(!validScopes.contains(validScopeTry) && validScopeTry.contains('.')) {
            validScopeTry = validScopeTry.substringBeforeLast('.')
        }
        return if(validScopes.contains(validScopeTry)) validScopeTry else null
    }

    @Environment(EnvType.CLIENT)
    internal fun registerConfig(config: Config, baseConfig: Config) {
        validScopes.add(config.getId().namespace)
        validSubScopes.put(config.getId().namespace, config.getId().path)
        UpdateManager.applyKeys(config)
        clientConfigs[config.getId().toTranslationKey()] = ConfigPair(config, baseConfig)
    }

    private class ConfigPair(val active: Config, val base: Config)
}