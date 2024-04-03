package me.fzzyhmstrs.fzzy_config.updates

import org.jetbrains.annotations.VisibleForTesting

@VisibleForTesting
@JvmDefaultWithCompatibility
interface UpdateApplier {
    fun apply()
    fun revert()
    fun restore(scope: String)
    fun hasChanges(): Boolean
    fun changes(): Int
    fun changeHistory(): List<String>
    fun hasForwards(): Boolean
    fun forwardsWidget()
}