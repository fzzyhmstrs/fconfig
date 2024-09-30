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
import me.fzzyhmstrs.fzzy_config.theme.ThemeKeys
import me.fzzyhmstrs.fzzy_config.theme.api.ThemeApi
import net.minecraft.util.Identifier
import java.util.LinkedList

internal object ThemeApiImpl: ThemeApi {

    private val stacks: MutableMap<Identifier, LinkedList<Theme>> = mutableMapOf()
    private var themes: Map<Identifier, Theme> = mapOf()
    private var configThemes: MutableMap<String, Array<out Identifier>> = mutableMapOf()

    internal fun updateThemes(themes: Map<Identifier, Theme>) {
        this.themes = themes
    }

    override fun registerConfigThemes(id: Identifier, vararg themeId: Identifier) {
        registerConfigThemes(id.toTranslationKey(), *themeId)
    }

    override fun registerConfigThemes(scope: String, vararg themeId: Identifier) {
        configThemes[scope] = themeId
    }

    override fun pushTheme(themeStack: Identifier, themeId: Identifier) {
        val theme = themes.getOrDefault(themeId, Theme.EMPTY)
        stacks.computeIfAbsent(themeStack) { _ -> LinkedList() }.push(theme)
    }

    override fun popTheme(themeStack: Identifier) {
        stacks[themeStack]?.takeIf { it.isNotEmpty() }?.pop()
    }

    override fun addThemes(themeStack: Identifier, vararg themeId: Identifier) {
        println("adding themes $themeId")
        stacks.computeIfAbsent(themeStack) { _ -> LinkedList() }.addAll(0, themeId.map { themes.getOrDefault(it, Theme.EMPTY) }.toSet())
    }

    override fun removeThemes(themeStack: Identifier, vararg themeId: Identifier) {
        println("removing themes $themeId")
        stacks[themeStack]?.removeAll(themeId.map { themes.getOrDefault(it, Theme.EMPTY) }.toSet())
    }

    internal fun addConfigScreen(scope: String) {
        val themes = configThemes[scope] ?: return
        addThemes(ThemeKeys.CONFIG_STACK, *themes)
    }

    internal fun removeConfigScreen(scope: String) {
        val themes = configThemes[scope] ?: return
        removeThemes(ThemeKeys.CONFIG_STACK, *themes)
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