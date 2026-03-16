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

import net.minecraft.network.chat.Component
import java.util.*

internal class FlagOnlyUpdateManager: UpdateManager, BasicValidationProvider {

    private val updates: MutableSet<String> = mutableSetOf()

    override fun update(updatable: Updatable, updateMessage: Component) {
        updates.add(updatable.getEntryKey())
    }

    override fun hasUpdate(scope: String): Boolean {
        return updates.contains(scope)
    }

    override fun getUpdate(scope: String): Updatable? {
        return null
    }

    override fun addUpdateMessage(key: Updatable, text: Component) {
    }

    fun getHistory(): Map<Updatable, SortedMap<Long, Component>> {
        return mapOf()
    }

    override fun hasChangeHistory(): Boolean {
        return false
    }

    override fun changeHistory(): List<String> {
        return emptyList()
    }

    override fun changeCount(): Int {
        return 0
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
    }

    override fun revertLast() {
    }

    override fun apply(final: Boolean) {
    }

    override fun flush(): List<String> {
        return emptyList()
    }
}