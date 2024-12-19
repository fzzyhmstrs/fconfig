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

import org.jetbrains.annotations.ApiStatus.Experimental
import org.jetbrains.annotations.ApiStatus.Internal

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

    fun titleBar(): TitleBar {
        return TitleBar.TOP
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
        IN_SIDEBAR
    }

    enum class TitleBar {
        TOP,
        IN_SIDEBAR
    }

    enum class CloseAction {
        BACK,
        DONE
    }

    companion object {
        val DEFAULT = object: ConfigSpec {}
    }
}