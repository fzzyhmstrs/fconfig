package me.fzzyhmstrs.fzzy_config.updates

@JvmDefaultWithCompatibility
internal interface UpdateApplier {
    fun apply()
    fun revert()
    fun changes(): Int
    fun changesWidget()
    fun hasForwards(): Boolean
    fun forwardedWidget()
}