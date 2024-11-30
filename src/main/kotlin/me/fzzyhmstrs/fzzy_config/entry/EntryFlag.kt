/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.entry

/**
 * Handles flags stored by the inheritor
 * @author fzzyhmstrs
 * @since 0.5.6
 */
interface EntryFlag {

    fun hasFlag(flag: Flag): Boolean {
        return false
    }
    fun flags(): Byte {
        return 0
    }

    enum class Flag(internal val flag: Byte) {
        /**
         * Marks that the flagged object requires the player to be in-game for it to work
         * @author fzzyhmstrs
         * @since 0.5.6
         */
        REQUIRES_WORLD(1),
        /**
         * Marks that listeners should not be fired on change
         * @author fzzyhmstrs
         * @since 0.5.9
         */
        QUIET(2),
        /**
         * Marks that strong validation should be used. This should be used with caution. Strong validation often relies on game state being loaded. Ensure that is the case before setting with this field active.
         * @author fzzyhmstrs
         * @since 0.5.9
         */
        STRONG(4),
        /**
         * Marks that the field should update its state when set.
         * @author fzzyhmstrs
         * @since 0.5.9
         */
        UPDATE(8);

        companion object {
            val NONE = listOf<Flag>()
        }
    }
}