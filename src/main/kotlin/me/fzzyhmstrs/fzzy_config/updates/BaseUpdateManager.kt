package me.fzzyhmstrs.fzzy_config.updates

import com.google.common.collect.ArrayListMultimap
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.entry.EntryKeyed
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.util.FcText.transLit
import me.fzzyhmstrs.fzzy_config.validation.BasicValidationProvider
import net.minecraft.text.Text
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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

    private val updateMap: MutableMap<String, Updatable> = mutableMapOf()
    private val changeHistory: MutableMap<Updatable, ArrayListMultimap<Long, Text>> = mutableMapOf()

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

    override fun addUpdateMessage(key: Updatable,text: Text) {
        changeHistory.computeIfAbsent(key){ArrayListMultimap.create()}.put(System.currentTimeMillis(),text)
    }

    override fun hasChangeHistory(): Boolean {
        return changeHistory.isNotEmpty()
    }

    override fun changeHistory(): List<String> {
        val list: MutableList<String> = mutableListOf()
        for ((updatable, updateLog) in changeHistory){
            for ((time, updates) in updateLog.entries()){
                list.add("[${updatable.transLit(updatable.getEntryKey())}]: ${updates.string}")
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

    override fun apply() {
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
            for ((time, updates) in updateLog.entries()){
                list.add("Updated scope [${updatable.getEntryKey()}] at [${formatter.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault()))}]: ${updates.string}")
            }
        }
        return list
    }

    fun applyKeys(config: Config) {
        ConfigApiImpl.walk(config,config.getId().toTranslationKey(),true) {_,str,v,_,_ -> if (v is EntryKeyed) v.setEntryKey(str)}
    }

    fun pushStates(config: Config) {
        ConfigApiImpl.walk(config,config.getId().toTranslationKey(),true) {_,_,v,_,_ -> if (v is Updatable) v.pushState()}
    }
}