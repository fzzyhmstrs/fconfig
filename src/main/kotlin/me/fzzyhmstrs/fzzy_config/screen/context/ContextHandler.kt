/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.screen.context

/**
 * Handles a context input. This is the downstream-facing counterpart of [ContextProvider]; it passes unhandled context downstream for further handling. This handling doesn't have to use [ContextAction] to achieve its goals, but that may be a convenient method to do so (perhaps via requesting context action via children [ContextProvider])
 *
 * See the [Wiki](https://github.com/fzzyhmstrs/fconfig/wiki/Context-Actions) for a detailed overview of the ContextAction system in fzzy config.
 * @author fzzyhmstrs
 * @since 0.6.0
 */
interface ContextHandler {

    /**
     * Handle a context event. This will be triggered by a parent, typically on key press or mouse click. A parent object can of course also be a context handler, handle context as applicable and then pass the event downstream for further handling.
     *
     * See the [Wiki](https://github.com/fzzyhmstrs/fconfig/wiki/Context-Actions) for a detailed overview of the ContextAction system in fzzy config.
     * @param contextType [ContextType] the context event type for handling. This is effectively the "active keybind" to handle. For example, [ContextType.COPY] will be passed when ctrl+C is pressed, and can be handled as a copy action when relevant.
     * @param position [Position] a position context for acting against. This can be updated by a context handler before usage or before passing downstream. For example, if a parent widget receives a context request, it can add it's x/y/width/height context to the position before passing.
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    fun handleContext(contextType: ContextType, position: Position): Boolean
}