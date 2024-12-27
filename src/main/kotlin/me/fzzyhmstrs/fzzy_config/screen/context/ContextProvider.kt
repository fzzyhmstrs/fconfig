package me.fzzyhmstrs.fzzy_config.screen.context

interface ContextProvider {
    fun contextActions(position: Position): List<ContextApplier>
}