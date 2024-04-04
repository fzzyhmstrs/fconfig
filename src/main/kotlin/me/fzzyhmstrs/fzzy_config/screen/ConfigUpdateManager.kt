package me.fzzyhmstrs.fzzy_config.screen

import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.entry.Entry
import me.fzzyhmstrs.fzzy_config.entry.EntrySupplier
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.registry.SyncedConfigRegistry
import me.fzzyhmstrs.fzzy_config.updates.BaseUpdateManager
import me.fzzyhmstrs.fzzy_config.updates.Updatable
import org.jetbrains.annotations.ApiStatus

internal class ConfigUpdateManager(private val configs: List<Pair<Config,Boolean>>, private val configMap: Map<String,Set<Config>>, private val forwardedUpdates: MutableList<ConfigScreenManager.ForwardedUpdate>, private val perms: Int): BaseUpdateManager() {

    private val cachedRestoreCounts: MutableMap<String, Int> = mutableMapOf()

    override fun restoreCount(scope: String): Int {
        return cachedRestoreCounts.computeIfAbsent(scope) { computeRestoreCount(scope) }
    }
    private fun computeRestoreCount(scope: String): Int{
        val set = configMap[scope] ?: return 0
        var count = 0
        set.forEach { config ->
            ConfigApiImpl.walk(config,config.getId().toTranslationKey(), true) {_,_,v,_,_ ->
                if (v is Updatable && !v.isDefault())
                    count++
            }
        }
        return count
    }

    override fun restore(scope: String) {
        cachedRestoreCounts.remove(scope)
        val set = configMap[scope] ?: return
        set.forEach { config ->
            ConfigApiImpl.walk(config,config.getId().toTranslationKey(), true) {_,_,v,_,_ ->
                if (v is Updatable)
                    v.restore()
            }
        }
    }

    override fun forwardsCount(): Int {
        return forwardedUpdates.size
    }

    @ApiStatus.Internal
    override fun apply() {
        //push updates from basic validation to the configs
        for ((config,bool) in configs){
            ConfigApiImpl.walk(config,config.getId().toTranslationKey(),true) { _, new, thing, prop, _ ->
                if (!(thing is Updatable && thing is Entry<*, *>)){
                    val update = getUpdate(new)
                    if (update != null && update is EntrySupplier<*>){
                        try {
                            prop.setter.call(config, update.supplyEntry())
                        } catch (e: Exception){
                            FC.LOGGER.error("Error pushing update to simple property [$new]")
                        }
                    }
                }
            }
        }
        //save config updates locally
        for (config in configs){
            config.first.save()
        }
        //send updates to the server for distribution and saving there
        val updates = this.configs.filter { !it.second }.associate { it.first.getId().toTranslationKey() to ConfigApiImpl.serializeUpdate(it.first, this, mutableListOf()) }
        SyncedConfigRegistry.updateServer(updates, flush(), perms)
    }

    override fun flush(): List<String> {
        cachedRestoreCounts.clear()
        return super.flush()
    }
}