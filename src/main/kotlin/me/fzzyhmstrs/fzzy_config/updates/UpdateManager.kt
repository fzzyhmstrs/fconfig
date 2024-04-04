package me.fzzyhmstrs.fzzy_config.updates

import net.minecraft.text.Text

@JvmDefaultWithCompatibility
interface UpdateManager {
    companion object Base: BaseUpdateManager()
    fun update(updatable: Updatable, updateMessage: Text)
    fun hasUpdate(scope: String): Boolean
    fun getUpdate(scope: String): Updatable?
    fun addUpdateMessage(key: Updatable, text: Text)
    fun hasChangeHistory(): Boolean
    fun changeHistory(): List<String>
    fun hasChanges(): Boolean{
        return changeCount() > 0
    }
    fun changeCount(): Int
    fun hasRestores(scope: String): Boolean{
        return restoreCount(scope) > 0
    }
    fun restoreCount(scope: String): Int
    fun restore(scope: String)
    fun hasForwards(): Boolean{
        return forwardsCount() > 0
    }
    fun forwardsCount(): Int
    fun forwardsHandler()
    fun revert()
    fun apply()
    fun flush(): List<String>
}