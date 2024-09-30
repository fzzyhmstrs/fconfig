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

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import me.fzzyhmstrs.fzzy_config.theme.ThemeKey

internal class ThemeFile(val replace: Boolean, val themeContents: Map<ThemeKey<*>, Any>) {

    internal companion object {
        val codec = RecordCodecBuilder.create { instance: RecordCodecBuilder.Instance<ThemeFile> ->
            instance.group(
                Codec.BOOL.optionalFieldOf("replace", false).forGetter(ThemeFile::replace),
                ThemeKey.themeMapCodec.fieldOf("values").forGetter(ThemeFile::themeContents)
            ).apply(instance, ::ThemeFile)
        }
    }

}