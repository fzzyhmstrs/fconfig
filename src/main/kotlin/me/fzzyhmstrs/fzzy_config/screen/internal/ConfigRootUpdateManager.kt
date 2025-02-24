package me.fzzyhmstrs.fzzy_config.screen.internal

import me.fzzyhmstrs.fzzy_config.screen.widget.DynamicListWidget
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.function.BiFunction

class ConfigRootUpdateManager: ConfigBaseUpdateManager() {

    private var managers: MutableList<ConfigBaseUpdateManager> = mutableListOf()

    fun addChild(manager: ConfigBaseUpdateManager) {
        managers.add(manager)
    }

    override fun hasChanges(): Boolean {
        return managers.any { it.hasChanges() }
    }

    override fun changeCount(): Int {
        var count = 0
        for (manager in managers) {
            count += manager.changeCount()
        }
        return count
    }

    override fun apply(final: Boolean) {
        for (manager in managers) {
            manager.apply(final)
        }
    }

    override fun revert() {
        for (manager in managers) {
            manager.revert()
        }
    }

    override fun hasRestores(scope: String): Boolean {
        return managers.any { it.hasRestores(scope) }
    }

    override fun restore(scope: String) {
        for (manager in managers) {
            manager.restore(scope)
        }
    }

    override fun hasChangeHistory(): Boolean {
        return managers.any { it.hasChangeHistory() }
    }

    override fun changeHistory(): List<String> {
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val changes: SortedMap<Long, MutableList<String>> = sortedMapOf()
        for (manager in managers) {
            for ((_, updateLog) in manager.getHistory()) {
                for ((time, updates) in updateLog) {
                    changes.computeIfAbsent(time) {_ -> mutableListOf()}.add("[${formatter.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault()))}]: <${manager.managerId()}> ${updates.string}")
                }
            }
        }
        return changes.values.flatten()
    }

    override fun forwardsCount(): Int {
        var count = 0
        for (manager in managers) {
            count += manager.forwardsCount()
        }
        return count
    }

    override fun forwardHandlerEntries(): List<BiFunction<DynamicListWidget, Int, out DynamicListWidget.Entry>> {
        return managers.flatMap { it.forwardHandlerEntries() }
    }
}