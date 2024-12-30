package me.fzzyhmstrs.fzzy_config.screen.context

//TODO
interface ContextProvider {
    //TODO
    fun provideContext(builder: ContextResultBuilder)

    companion object {

        //TODO
        fun empty(position: Position): ContextResultBuilder {
            return ContextResultBuilder(position)
        }
    }
}