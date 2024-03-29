package test

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

    override fun changes(): Int {
        return changes
    }

    override fun changesWidget() {
        println("you wanted to open the changes popup!")
    }

    override fun hasForwards(): Boolean {
        return false
    }

    override fun forwardedWidget() {
        println("you wanted to open the forwards popup!")
    }
}