/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.config

import org.jetbrains.annotations.ApiStatus.Internal
import org.jetbrains.annotations.ApiStatus.Experimental

@JvmDefaultWithoutCompatibility
@Internal
@Experimental
interface ConfigSpec {
    fun sidebar(): Sidebar {
        return Sidebar.NONE
    }

    fun actionBar(): ActionBar {
        return ActionBar.BOTTOM
    }

    fun closeAction(): CloseAction {
        return CloseAction.BACK
    }


    enum class Sidebar {
        NONE,
        KEEP_ENTRIES,
        REMOVE_ENTRIES
    }

    enum class ActionBar {
        BOTTOM,
        TOP,
        IN_SIDEBAR
    }

    enum class CloseAction {
        BACK,
        DONE
    }
}