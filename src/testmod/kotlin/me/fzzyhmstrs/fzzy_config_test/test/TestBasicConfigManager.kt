/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config_test.test

import me.fzzyhmstrs.fzzy_config.updates.Updatable
import me.fzzyhmstrs.fzzy_config.updates.UpdateManager
import net.minecraft.text.Text


class TestBasicConfigManager: UpdateManager {

    private var changes: Int = 2
    private val changeHistory: MutableList<String> = mutableListOf("Change 1", "Change 2")

    override fun apply(final: Boolean) {
        changes++
        changeHistory.add("Change $changes")
    }

    override fun flush(): List<String> {
        return emptyList()
    }

    override fun revert() {
        changeHistory.add("Reverting $changes Changes")
        changes = 0
    }

    override fun revertLast() {
    }

    override fun restore(scope: String) {
        changes = 2
        changeHistory.add("Restoring default change count")
    }

    override fun update(updatable: Updatable, updateMessage: Text) {
    }

    override fun hasUpdate(scope: String): Boolean {
        return false
    }

    override fun getUpdate(scope: String): Updatable? {
        return null
    }

    override fun addUpdateMessage(key: Updatable, text: Text) {
    }

    override fun hasChangeHistory(): Boolean {
        return true
    }

    override fun changeHistory(): List<String> {
        return changeHistory
    }


    override fun hasChanges(): Boolean {
        return changes > 0
    }

    override fun changeCount(): Int {
        return changes
    }

    override fun restoreCount(scope: String): Int {
        return if(changes == 0) 1 else 0
    }

    override fun hasForwards(): Boolean {
        return false
    }

    override fun forwardsCount(): Int {
        return 0
    }

    override fun forwardsHandler() {
        println("you wanted to open the forwards popup!")
    }
}