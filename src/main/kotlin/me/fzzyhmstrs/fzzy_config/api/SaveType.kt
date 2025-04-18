/*
 * Copyright (c) 2025 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.api

import me.fzzyhmstrs.fzzy_config.annotations.Action

/**
 * Defines the behavior of config saving when a config is received by a client from a server
 * @author fzzyhmstrs
 * @since 0.6.8
 */
enum class SaveType(private vararg val incompatibleActions: Action) {
    /**
     * Default behavior. Client configs will be overwritten when an update is received.
     * @author fzzyhmstrs
     * @since 0.6.8
     */
    OVERWRITE,
    /**
     * Client config files will be maintained separately from server configs.
     *
     * If you have [restart-causing settings][me.fzzyhmstrs.fzzy_config.annotations.Action.RESTART], this save type will throw an exception, as the client will have no way to maintain the proper config state to start the client in-sync with the server.
     * @author fzzyhmstrs
     * @since 0.6.8
     */
    SEPARATE(Action.RESTART, Action.RELOG);

    internal fun incompatibleWith(actions: Set<Action>?): Boolean {
        return incompatibleActions.any { actions?.contains(it) == true }
    }

    internal fun incompatibleWith(action: Action): Boolean {
        return incompatibleActions.contains(action)
    }
}