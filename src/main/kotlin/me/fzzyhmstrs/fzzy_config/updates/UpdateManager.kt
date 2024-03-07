package me.fzzyhmstrs.fzzy_config.updates

import com.google.common.collect.ArrayListMultimap
import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.impl.FzzySerializable
import me.fzzyhmstrs.fzzy_config.util.Update
import net.minecraft.text.Text
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object UpdateManager{

    // Important Base Concept: SCOPE
    // basically a string mapping of the "location" of an element in a config layout, not disimilar to a file path
    //
    // Top level
    //   The namespace of the mod adding configs. The namespace of config.getId()
    //   ex. 'mymod'
    //
    // Config
    //   Next level is the config name, the path of the getId()
    //   ex. 'items'
    //
    // Subsection
    //   sections add a layer to the scope. stacks.
    //   ex. 'dropRates'
    //
    // Element
    //   finally the element terminates the scope
    //   ex. 'oceanChests'
    //
    // Built
    //   scopes are built into translation-key-like strings
    //   ex. 'mymod.items.dropRates.oceanChests'

    private val updateMap: MutableMap<String, Updater> = mutableMapOf()
    private val changeHistory: MutableMap<String, ArrayListMultimap<Long, Text>> = mutableMapOf()
    private var currentScope = ""

    fun setScope(scope: String){
        currentScope = scope
    }

    fun flush(){
        updateMap.clear()
        if (changeHistory.isNotEmpty()) {
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            FC.LOGGER.info("Completed config updates:")
            FC.LOGGER.info("∨∨∨∨∨∨∨∨∨∨∨∨∨∨∨∨∨∨∨∨∨∨∨∨∨")
            for ((scope, updateLog) in changeHistory){
                for ((time, updates) in updateLog.entries()){
                    FC.LOGGER.info("Updated scope [$scope] at [${formatter.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault()))}]: [${updates.string}]")
                }
            }
            FC.LOGGER.info("∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧∧")
        }
        changeHistory.clear()
    }

    fun needsUpdatePop(updatable: Updatable): Boolean {
        val key = updatable.getUpdateKey()
        val updater = updateMap[key] ?: return false
        if (!updater.canUndo()) return false
        return updatable.popState()
    }

    fun needsUpdatePeek(updatable: Updatable): Boolean {
        val updater = updateMap[updatable.getUpdateKey()] ?: return false
        if (!updater.canUndo()) return false
        return updatable.peekState()
    }

    fun update(key: String, update: Update) {
        updateMap[key]?.update(update)
    }

    fun getChangeCount(scope: String): Int {
        var count = 0
        for (update in getScopedUpdates(scope)){
            count += update.changeCount()
        }
        return count
    }

    fun updateCurrentChangeCount(): Int {
        return getChangeCount(currentScope)
    }

    fun revert(scope: String) {
        for (update in getScopedUpdates(scope)){
            update.revert()
        }
    }

    fun revertCurrent() {
        revert(currentScope)
    }

    fun addUpdateMessage(key: String,text: Text) {
        changeHistory.computeIfAbsent(key){ArrayListMultimap.create()}.put(System.currentTimeMillis(),text)
    }

    fun getScopedUpdates(scope: String): Collection<Updater> {
        return (if(scope.isEmpty()) updateMap.keys else updateMap.keys.filter{ it.startsWith(scope) }).mapNotNull { updateMap[it] }
    }

    internal fun<T: Config> applyKeys(config: T) {
        ConfigApiImpl.walk(config,config.getId().toTranslationKey(),true) {str, v -> if (v is Updatable) v.setUpdateKey(str)}
    }

    internal fun<T: Config> getSyncUpdates(config: T, ignoreNonSync: Boolean = false): Map<String, FzzySerializable> {
        val map: MutableMap<String, FzzySerializable> = mutableMapOf()
        ConfigApiImpl.walk(config,config.getId().toTranslationKey(), ignoreNonSync) {str, v -> if (v is Updatable && v is FzzySerializable) { if (needsUpdatePop(v)) map[str] = v }}
        return map
    }
}
