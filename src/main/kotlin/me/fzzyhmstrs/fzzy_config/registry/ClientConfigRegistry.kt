package me.fzzyhmstrs.fzzy_config.registry

import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.screen.ConfigScreenManager
import me.fzzyhmstrs.fzzy_config.updates.UpdateManager
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import java.util.*

/**
 * The registry for [Config] instances.
 *
 * This is not a "true" registry in the Minecraft since; as such there are not the typical helper methods like get(), getId(), etc. This registry's scope is much narrower, handling synchronization and updates of Configs.
 */
@Environment(EnvType.CLIENT)
object ClientConfigRegistry {

    private val clientConfigs : MutableMap<String, Config> = mutableMapOf()
    private val configScreenManagers: MutableMap<String, ConfigScreenManager> = mutableMapOf()
    private var validScopes: MutableSet<String> = mutableSetOf() //configs are sorted into Managers by namespace

    @Environment(EnvType.CLIENT)
    internal fun openScreen(scope: String){
        val namespaceScope = getValidScope(scope)
        if (namespaceScope == null){
            FC.LOGGER.error("Failed to open a FzzyConfig screen. Invalid scope provided: [$scope]")
            return
        }
        val manager = configScreenManagers.computeIfAbsent(namespaceScope) {
            ConfigScreenManager(
                namespaceScope,
                clientConfigs.filterKeys { s -> s.startsWith(namespaceScope) }.map { Pair(it.value, SyncedConfigRegistry.hasConfig(it.key)) })
        }
        manager.openScreen(scope)
    }

    @Environment(EnvType.CLIENT)
    internal fun handleForwardedUpdate(update: String, player: UUID, scope: String){
        val namespaceScope = getValidScope(scope)
        if (namespaceScope == null){
            FC.LOGGER.error("Failed to handle a forwarded setting. Invalid scope provided: [$scope]")
            return
        }
        val manager = configScreenManagers[namespaceScope]
        if (manager == null){
            FC.LOGGER.error("Failed to handle a forwarded setting. Unknown scope provided: [$scope]")
            return
        }
        manager.receiveForwardedUpdate(update, player, scope)
    }

    @Environment(EnvType.CLIENT)
    private fun getValidScope(scope: String): String?{
        if(validScopes.contains(scope)) return scope
        var validScopeTry = scope.substringBeforeLast('.')
        if (validScopeTry == scope) return null
        while(!validScopes.contains(validScopeTry) && validScopeTry.contains('.')){
            validScopeTry = validScopeTry.substringBeforeLast('.')
        }
        return if(validScopes.contains(validScopeTry)) validScopeTry else null
    }

    @Environment(EnvType.CLIENT)
    internal fun registerConfig(config: Config){
        validScopes.add(config.getId().namespace)
        UpdateManager.INSTANCE.applyKeys(config)
        clientConfigs[config.getId().toTranslationKey()] = config
    }
}