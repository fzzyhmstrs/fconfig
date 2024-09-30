/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.api

import com.mojang.serialization.Codec
import me.fzzyhmstrs.fzzy_config.theme.ThemeKey
import net.minecraft.util.Identifier

interface ThemeApi {

    fun registerConfigThemes(id: Identifier, vararg themeId: Identifier)

    fun registerConfigThemes(scope: String, vararg themeId: Identifier)

    fun pushTheme(themeStack: Identifier, themeId: Identifier)

    fun popTheme(themeStack: Identifier)

    fun addThemes(themeStack: Identifier, vararg themeId: Identifier)

    fun removeThemes(themeStack: Identifier, vararg themeId: Identifier)

    fun <T: Any> provide(themeStack: Identifier, key: ThemeKey<T>): T

    fun <T: Any> createAndRegister(id: Identifier, codec: Codec<T>, default: T): ThemeKey<T>

    fun createAndRegisterInt(id: Identifier, default: Int): ThemeKey<Int>

    fun createAndRegisterFloat(id: Identifier, default: Float): ThemeKey<Float>

    fun createAndRegisterString(id: Identifier, default: String): ThemeKey<String>

    fun createAndRegisterId(id: Identifier, default: Identifier): ThemeKey<Identifier>

}