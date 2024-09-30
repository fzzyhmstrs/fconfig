/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.impl

import me.fzzyhmstrs.fzzy_config.theme.Theme
import me.fzzyhmstrs.fzzy_config.theme.ThemeKey
import java.util.*

internal class ThemeBuilder {
    private val themeContents: MutableMap<ThemeKey<*>, Any> = mutableMapOf()

    fun addFile(file: ThemeFile) {
        if (file.replace) {
            for ((key, value) in file.themeContents) {
                this.themeContents[key] = value
            }
        } else {
            for ((key, value) in file.themeContents) {
                if (!this.themeContents.containsKey(key))
                    this.themeContents[key] = value
            }
        }
    }

    fun build(): Theme {
        return Theme(IdentityHashMap(themeContents))
    }

}