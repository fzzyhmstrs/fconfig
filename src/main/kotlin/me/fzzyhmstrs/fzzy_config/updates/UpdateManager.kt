package me.fzzyhmstrs.fzzy_config.updates

import net.minecraft.text.Text

interface UpdateManager {
    companion object {
        internal val INSTANCE  = UpdateManagerImpl()
    }
    fun update(updatable: Updatable, updateMessage: Text)
    fun hasUpdate(scope: String): Boolean
    fun changes(): Int
    fun revertAll()
    fun restoreAll()
    fun addUpdateMessage(key: String,text: Text)
}