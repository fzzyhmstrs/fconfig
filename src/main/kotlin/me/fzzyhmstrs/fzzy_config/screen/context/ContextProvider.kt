package me.fzzyhmstrs.fzzy_config.screen.context

/**
 * Generates a set of grouped context actions upon request. This is the upstream-facing counterpart of [ContextHandler]; it applies its results to a builder provided from upstream for further amendment and usage upstream. This may be used together with a handler (handler requests potential context inputs by requesting for them from it's children provider(s))
 *
 * See the [Wiki](https://github.com/fzzyhmstrs/fconfig/wiki/Context-Actions) for a detailed overview of the ContextAction system in fzzy config.
 * @author fzzyhmstrs
 * @since 0.6.0
 */
interface ContextProvider {
    /**
     * Add a set of grouped context action builders to a result
     *
     * You can also modify existing builders in this call if a downstream provider set an initial state that needs changing. For example:
     * ```
     * val builder = child.provideContext(builder)
     * builder.apply("child_group", ContextType.YOUR_TYPE) { actionBuilder: ContextAction.Builder -> /* apply needed tweaks here */ }
     * ```
     *
     * [ValidatedCondition.contextActionBuilder][me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedCondition.contextActionBuilder] is an example of a similar process (albeit using the validation systems method for collecting inputs to this provider process)
     * @param builder [ContextResultBuilder] builder to add or apply new context actions to
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    fun provideContext(builder: ContextResultBuilder)

    companion object {

        /**
         * Empty [ContextResultBuilder], useful for input into a context provision request from the parent/head of the context chain.
         * @param position [Position] initial position context for this builder. This may be modified by downstream providers as needed to scope in the final context position used by all actions provided to this builder.
         * @return [ContextResultBuilder] with initial position state and no actions.
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        @JvmStatic
        fun empty(position: Position): ContextResultBuilder {
            return ContextResultBuilder(position)
        }
    }
}
