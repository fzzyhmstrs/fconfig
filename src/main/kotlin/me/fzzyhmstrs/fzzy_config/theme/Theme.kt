/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme

import com.mojang.serialization.Codec
import java.util.IdentityHashMap

class Theme(private val themeContents: IdentityHashMap<ThemeKey<*>, Any>) {

    fun map(): Map<ThemeKey<*>, Any> {
        return themeContents
    }

    companion object {
        val EMPTY = Theme(IdentityHashMap())

        internal val codec: Codec<Theme> = ThemeKey.themeMapCodec.xmap(
            { map -> Theme(IdentityHashMap(map)) },
            { theme -> theme.themeContents }
        )
    }

}