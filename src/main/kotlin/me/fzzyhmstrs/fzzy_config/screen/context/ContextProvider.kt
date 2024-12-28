package me.fzzyhmstrs.fzzy_config.screen.context

interface ContextProvider {
    fun provideContext(builder: ContextResultBuilder)

    companion object {

        fun empty(position: Position): ContextResultBuilder {
            return ContextResultBuilder(position)
        }
    }
}