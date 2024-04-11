package me.fzzyhmstrs.fzzy_config.updates

import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.entry.EntryKeyed
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.validation.BasicValidationProvider
import net.minecraft.text.Text
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

open class BaseUpdateManager: UpdateManager, BasicValidationProvider {


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

    protected val updateMap: LinkedHashMap<String, Updatable> = LinkedHashMap()
    private val changeHistory: MutableMap<Updatable, SortedMap<Long, Text>> = mutableMapOf()

    override fun update(updatable: Updatable, updateMessage: Text) {
        updateMap.computeIfAbsent(updatable.getEntryKey()) { updatable }
        addUpdateMessage(updatable, updateMessage)
    }

    override fun hasUpdate(scope: String): Boolean{
        return updateMap[scope]?.popState() ?: false
    }

    override fun getUpdate(scope: String): Updatable?{
        return updateMap[scope]
    }

    override fun addUpdateMessage(key: Updatable, text: Text) {
        val updateLog = changeHistory.computeIfAbsent(key){ sortedMapOf() }
        var baseTime = System.currentTimeMillis()
        while(updateLog.containsKey(baseTime)){
            baseTime++
        }
        updateLog[baseTime] = text
    }

    override fun hasChangeHistory(): Boolean {
        return changeHistory.isNotEmpty()
    }

    override fun changeHistory(): List<String> {
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val list: MutableList<String> = mutableListOf()
        for ((_, updateLog) in changeHistory){
            for ((time, updates) in updateLog){
                list.add("[${formatter.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault()))}]: ${updates.string}")
            }
        }
        return list
    }

    override fun changeCount(): Int {
        return updateMap.filter { it.value.peekState() }.size
    }

    override fun restoreCount(scope: String): Int {
        return 0
    }

    override fun restore(scope: String) {
    }

    override fun forwardsCount(): Int {
        return 0
    }

    override fun forwardsHandler() {
    }

    override fun revert() {
        for (update in updateMap.values) {
            update.revert()
        }
    }

    override fun revertLast() {
        for ((_,update) in updateMap.entries.reversed()){
            if (update.peekState()){
                update.revert()
                return
            }
        }
    }

    override fun apply(final: Boolean) {
    }

    override fun flush(): List<String> {
        updateMap.clear()
        val updates = buildChangeHistoryLog()
        changeHistory.clear()
        return updates
    }

    private fun buildChangeHistoryLog(): List<String> {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm")
        val list: MutableList<String> = mutableListOf()
        for ((updatable, updateLog) in changeHistory){
            for ((time, updates) in updateLog){
                list.add("Updated scope [${updatable.getEntryKey()}] at [${formatter.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault()))}]: ${updates.string}")
            }
        }
        return list
    }

    fun applyKeys(config: Config) {
        ConfigApiImpl.walk(config,config.getId().toTranslationKey(),1) { _,_,str,v,_,_ -> if (v is EntryKeyed) v.setEntryKey(str)}
    }

    fun pushStates(config: Config) {
        ConfigApiImpl.walk(config,config.getId().toTranslationKey(),1) { _,_,_,v,_,_ -> if (v is Updatable) v.pushState()}
    }
}