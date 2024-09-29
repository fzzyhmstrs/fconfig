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
import me.fzzyhmstrs.fzzy_config.theme.Theme
import me.fzzyhmstrs.fzzy_config.theme.ThemeKey
import me.fzzyhmstrs.fzzy_config.theme.api.ThemeApi
import net.minecraft.util.Identifier
import java.util.LinkedList

internal object ThemeApiImpl: ThemeApi {

    private val stacks: MutableMap<Identifier, LinkedList<Theme>> = mutableMapOf()
    private var themes: Map<Identifier, Theme> = mapOf()

    internal fun updateThemes(themes: Map<Identifier, Theme>) {
        this.themes = themes
    }

    override fun pushTheme(themeStack: Identifier, themeId: Identifier) {
        val theme = themes.getOrDefault(themeId, Theme.EMPTY)
        stacks.computeIfAbsent(themeStack) { _ -> LinkedList() }.push(theme)
    }

    override fun popTheme(themeStack: Identifier) {
        stacks[themeStack]?.takeIf { it.isNotEmpty() }?.pop()
    }

    override fun <T : Any> provide(themeStack: Identifier, key: ThemeKey<T>): T {
        val stack = stacks[themeStack] ?: return key.provideDefault()
        for (theme in stack) {
            return key.provideValue(theme.map()) ?: continue
        }
        return key.provideDefault()
    }

    override fun <T : Any> createAndRegister(id: Identifier, codec: Codec<T>, default: T): ThemeKey<T> {
        return ThemeKey.createAndRegister(id, codec, default)
    }

    override fun createAndRegisterInt(id: Identifier, default: Int): ThemeKey<Int> {
        return ThemeKey.createAndRegister(id, Codec.INT, default)
    }

    override fun createAndRegisterFloat(id: Identifier, default: Float): ThemeKey<Float> {
        return ThemeKey.createAndRegister(id, Codec.FLOAT, default)
    }

    override fun createAndRegisterString(id: Identifier, default: String): ThemeKey<String> {
        return ThemeKey.createAndRegister(id, Codec.STRING, default)
    }

    override fun createAndRegisterId(id: Identifier, default: Identifier): ThemeKey<Identifier> {
        return ThemeKey.createAndRegister(id, Identifier.CODEC, default)
    }


}