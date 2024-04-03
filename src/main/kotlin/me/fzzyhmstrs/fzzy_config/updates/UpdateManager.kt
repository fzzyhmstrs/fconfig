package me.fzzyhmstrs.fzzy_config.updates

import me.fzzyhmstrs.fzzy_config.config.Config
import net.minecraft.text.Text

interface UpdateManager {
    companion object {
        internal val INSTANCE  = UpdateManagerImpl()
    }
    fun update(updatable: Updatable, updateMessage: Text)
    fun hasUpdate(scope: String): Boolean
    fun changes(): Int
    fun revertAll()
    fun restoreAll(config: Config)
    fun addUpdateMessage(key: String,text: Text)
}