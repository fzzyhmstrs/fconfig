package me.fzzyhmstrs.fzzy_config.updates

import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.util.Update
import net.minecraft.text.Text
import java.util.*

object UpdateManager{

    private val updateMap: MutableMap<String, Updater> = mutableMapOf()
    private val changeHistory: LinkedList<Text> = LinkedList()
    private var changeCount = 0

    fun needsUpdatePop(key: String, updatable: Updatable): Boolean {
        val updater = updateMap[key] ?: return false
        if (!updater.canUndo()) return false
        return updatable.popState()
    }

    fun needsUpdatePeek(key: String, updatable: Updatable): Boolean {
        val updater = updateMap[key] ?: return false
        if (!updater.canUndo()) return false
        return updatable.peekState()
    }

    fun update(key: String, update: Update) {
        updateMap[key]?.update(update)
        changeCount++
    }

    fun updateChangeCount(){
        var count = 0
        for (updater in updateMap.values){
            count += updater.changeCount()
        }
        changeCount = count
    }

    fun addUpdateMessage(text: Text){
        changeHistory.push(text)
    }

    fun<T: Config> applyKeys(config: T) {
        ConfigApiImpl.walk(config,config.getId().toShortTranslationKey(),true) {str, v -> if (v is Updatable) v.setUpdateKey(str)}
    }

}