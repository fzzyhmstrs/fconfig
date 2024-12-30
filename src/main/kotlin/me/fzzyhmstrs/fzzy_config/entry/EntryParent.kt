/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.entry

import me.fzzyhmstrs.fzzy_config.annotations.Action

/**
 * An entry that is a parent of other entries
 * @author fzzyhmstrs
 * @since 0.1.1, added continue 0.6.0
 */
fun interface EntryParent {

    /**
     * Return a set of [Action] that this parents children are annotated with. This is used to show the total set of actions relevant to this parent to the user in-game.
     * @see [me.fzzyhmstrs.fzzy_config.api.ConfigApi.actions]
     * @author fzzyhmstrs
     * @since 0.1.1
     */
    fun actions(): Set<Action>

    /**
     * Whether the screen manager should skip over analyzing this parents internals.
     * @return True if entry creation is handled separately for this parents children, false if the standard screen manager should build entries in a new screen for them.
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    fun continueWalk(): Boolean {
        return false
    }
}