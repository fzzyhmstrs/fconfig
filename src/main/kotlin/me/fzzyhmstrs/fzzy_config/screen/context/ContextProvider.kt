package me.fzzyhmstrs.fzzy_config.screen.context

interface ContextProvider {
    fun provideContext(position: Position): ContextResult

    data class ContextResult(val appliers: List<ContextApplier>, val position: Position)

    companion object {

        fun empty(position: Position): ContextResult {
            return ContextResult(emptyList(), position)
        }
    }
}