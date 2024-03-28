package me.fzzyhmstrs.fzzy_config.test

import me.fzzyhmstrs.fzzy_config.updates.UpdateApplier

class TestBasicConfigManager: UpdateApplier {
    override fun apply() {
        println("Applying updated values!!")
    }

    override fun revert() {
        println("Reverting updated values :>")
    }

    override fun changes(): Int {
        return 0
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