package me.fzzyhmstrs.fzzy_config.updates

import me.fzzyhmstrs.fzzy_config.util.Update

object UpdateManager{

    private val updateMap: MutableMap<String, Updater>
    
    fun update(key: String, update: Update){
        updateMap[key].update(update)
    }
    
}
