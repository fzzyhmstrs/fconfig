package me.fzzyhmstrs.fzzy_config.updates

import com.google.common.collect.ArrayListMultimap
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.validated_field.entry.EntrySerializer
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
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

    private val updateMap: MutableMap<String, Updatable> = mutableMapOf()
    private val changeHistory: MutableMap<String, ArrayListMultimap<Long, Text>> = mutableMapOf()
    private var currentScope = ""

    fun setScope(scope: String) {
        currentScope = scope
    }

    fun flush(): List<String> {
        setScope("")
        updateMap.clear()
        val updates = buildChangeHistoryLog()
        changeHistory.clear()
        return updates
    }

    fun hasChangeHistory(): Boolean{
        return changeHistory.isNotEmpty()
    }

    fun buildChangeHistoryLog(): List<String> {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
        val list: MutableList<String> = mutableListOf()
        for ((scope, updateLog) in changeHistory){
            for ((time, updates) in updateLog.entries()){
                list.add("Updated scope [$scope] at [${formatter.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault()))}]: [${updates.string}]")
            }
        }
        return list
    }

    internal fun clear(updatable: Updatable) {
        updateMap.remove(updatable.getUpdateKey())
    }

    internal fun update(updatable: Updatable, updateMessage: Text) {
        updateMap.computeIfAbsent(updatable.getUpdateKey()) { updatable }
        addUpdateMessage(updatable.getUpdateKey(),updateMessage)
    }

    fun hasChanges(scope: String): Boolean{
        return getChangeCount(scope) > 0
    }

    fun getChangeCount(scope: String): Int {
        return getScopedUpdates(scope).size
    }

    fun getCurrentChangeCount(): Int {
        return getChangeCount(currentScope)
    }

    fun getTotalChangeCount(): Int {
        return getChangeCount("")
    }

    fun revert(scope: String) {
        for ((updateScope,update) in getScopedUpdates(scope)){
            update.revert()
        }
    }

    fun revertCurrent() {
        revert(currentScope)
    }

    fun restore(scope: String) {
        for ((updateScope,update) in getScopedUpdates(scope)){
            update.restore()
        }
    }

    fun restoreCurrent() {
        restore(currentScope)
    }

    fun addUpdateMessage(key: String,text: Text) {
        changeHistory.computeIfAbsent(key){ArrayListMultimap.create()}.put(System.currentTimeMillis(),text)
    }

    private fun getScopedUpdates(scope: String): Map<String,Updatable> {
        return if(scope.isEmpty()) updateMap else updateMap.filterKeys { it.startsWith(scope) }
    }

    internal fun<T: Config> applyKeys(config: T) {
        ConfigApiImpl.walk(config,config.getId().toTranslationKey(),true) {_, str, v, _ -> if (v is UpdateKeyed) v.setUpdateKey(str)}
    }

    internal fun<T: Config> pushStates(config: T) {
        ConfigApiImpl.walk(config,config.getId().toTranslationKey(),true) {_, _, v, _ -> if (v is Updatable) v.pushState()}
    }

    internal fun<T: Config> getSyncUpdates(config: T, ignoreNonSync: Boolean = false): Map<String, EntrySerializer<*>> {
        val map: MutableMap<String, EntrySerializer<*>> = mutableMapOf()
        ConfigApiImpl.walk(config,config.getId().toTranslationKey(), ignoreNonSync) {_, str, v, _ -> if (v is Updatable && v is EntrySerializer<*>) { if (v.popState()) map[str] = v }}
        return map
    }

    /*internal fun<T: Config> getSyncUpdates(config: T, ignoreNonSync: Boolean = false): Map<String, EntrySerializer<*>> {
        val map: MutableMap<String, EntrySerializer<*>> = mutableMapOf()
        for ((updateScope, update) in getScopedUpdates(config.getId().toTranslationKey())){
            if (update.popState()){
                if (update is EntrySerializer<*>)
                   map[updateScope] = update
            }
        }
        return map
    }*/
}