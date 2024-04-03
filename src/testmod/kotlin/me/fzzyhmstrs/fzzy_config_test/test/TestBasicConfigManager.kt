package me.fzzyhmstrs.fzzy_config_test.test

import me.fzzyhmstrs.fzzy_config.updates.UpdateApplier

class TestBasicConfigManager: UpdateApplier {

    private var changes: Int = 0

    override fun apply() {
        changes++
        println("Applying updated values!!")
    }

    override fun revert() {
        changes = 0
        println("Reverting updated values :>")
    }

    override fun restore(scope: String) {
        changes = 0
        println("Restoring updated values :>")
    }

    override fun hasChanges(): Boolean {
        return changes > 0
    }

    override fun changes(): Int {
        return changes
    }

    override fun changeHistory(): List<String> {
        return listOf("Change 1", "Change 2")
    }

    override fun hasForwards(): Boolean {
        return false
    }

    override fun forwardsWidget() {
        println("you wanted to open the forwards popup!")
    }
}