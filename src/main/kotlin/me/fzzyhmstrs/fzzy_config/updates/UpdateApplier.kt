package me.fzzyhmstrs.fzzy_config.updates

import org.jetbrains.annotations.VisibleForTesting

@VisibleForTesting
@JvmDefaultWithCompatibility
interface UpdateApplier {
    fun apply()
    fun revert()
    fun changes(): Int
    fun changesWidget()
    fun hasForwards(): Boolean
    fun forwardedWidget()
}