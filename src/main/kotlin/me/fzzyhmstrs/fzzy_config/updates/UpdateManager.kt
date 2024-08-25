/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.updates

import net.minecraft.text.Text
import org.jetbrains.annotations.ApiStatus.Internal

@Internal
@JvmDefaultWithCompatibility
interface UpdateManager {
    companion object Base: BaseUpdateManager()
    fun update(updatable: Updatable, updateMessage: Text)
    fun hasUpdate(scope: String): Boolean
    fun getUpdate(scope: String): Updatable?
    fun addUpdateMessage(key: Updatable, text: Text)
    fun hasChangeHistory(): Boolean
    fun changeHistory(): List<String>
    fun hasChanges(): Boolean {
        return changeCount() > 0
    }
    fun changeCount(): Int
    fun hasRestores(scope: String): Boolean {
        return restoreCount(scope) > 0
    }
    fun restoreCount(scope: String): Int
    fun restore(scope: String)
    fun hasForwards(): Boolean {
        return forwardsCount() > 0
    }
    fun forwardsCount(): Int
    fun forwardsHandler()
    fun revert()
    fun revertLast()
    fun apply(final: Boolean)
    fun flush(): List<String>
}