package me.fzzyhmstrs.fzzy_config.screen

import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.entry.Entry
import me.fzzyhmstrs.fzzy_config.entry.EntrySupplier
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.impl.ConfigSet
import me.fzzyhmstrs.fzzy_config.registry.SyncedConfigRegistry
import me.fzzyhmstrs.fzzy_config.updates.BaseUpdateManager
import me.fzzyhmstrs.fzzy_config.updates.Updatable
import net.minecraft.client.MinecraftClient
import org.jetbrains.annotations.ApiStatus

internal class ConfigUpdateManager(private val configs: List<ConfigSet>, private val forwardedUpdates: MutableList<ConfigScreenManager.ForwardedUpdate>, private val perms: Int): BaseUpdateManager() {

    private val updatableEntries: MutableMap<String, Updatable> = mutableMapOf()

    fun setUpdatableEntry(entry: Updatable) {
        updatableEntries[entry.getEntryKey()] = entry
    }

    fun pushUpdatableStates(){
        for (updatable in updatableEntries.values){
            updatable.pushState()
        }
    }

    override fun restoreCount(scope: String): Int {
        var count = 0
        for ((key, updatable) in updatableEntries){
            if(!key.startsWith(scope)) continue
            if(updatable.isDefault()) continue
            count++
        }
        return count
    }

    override fun restore(scope: String) {
        for ((key, updatable) in updatableEntries){
            if(!key.startsWith(scope)) continue
            updatable.restore()
        }
    }

    override fun forwardsCount(): Int {
        return forwardedUpdates.size
    }

    @ApiStatus.Internal
    override fun apply(final: Boolean) {
        if (updateMap.isEmpty()) return
        //push updates from basic validation to the configs
        for ((config,base,bool) in configs) {
            ConfigApiImpl.walk(config,config.getId().toTranslationKey(),true) { walkable,_,new,thing,prop,_ ->
                if (!(thing is Updatable && thing is Entry<*, *>)){
                    val update = getUpdate(new)
                    if (update != null && update is EntrySupplier<*>){
                        try {
                            prop.setter.call(walkable, update.supplyEntry())
                        } catch (e: Exception){
                            FC.LOGGER.error("Error pushing update to simple property [$new]")
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
        //save config updates locally
        for (config in configs){
            config.active.save()
        }
        var count = 0
        for (config in configs){
            if (!config.clientOnly)
                count++
        }
        val log = flush()
        if (count > 0) {
            //send updates to the server for distribution and saving there
            val updates = this.configs.filter { !it.clientOnly }.associate { it.active.getId().toTranslationKey() to ConfigApiImpl.serializeUpdate(it.active, this, mutableListOf()) }
            SyncedConfigRegistry.updateServer(updates, log, perms)
        } else {
            ConfigApiImpl.printChangeHistory(log,configs.map { it.active }.toString(),MinecraftClient.getInstance().player)
        }
        if (!final)
            pushUpdatableStates()
    }
}