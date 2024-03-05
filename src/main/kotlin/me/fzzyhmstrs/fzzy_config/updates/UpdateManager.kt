package me.fzzyhmstrs.fzzy_config.updates

import me.fzzyhmstrs.fzzy_config.util.Update

object UpdateManager{

    private val updateMap: MutableMap<String, Updater>
    private val changeHistory: LinkedList<Text> = LinkedList()
    
    fun update(key: String, update: Update){
        updateMap[key].update(update)
    }

    fun addUpdateMessage(text: Text){
        changeHistory.push(text)
    }
    
}
