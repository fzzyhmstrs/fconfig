/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.screen.internal

import me.fzzyhmstrs.fzzy_config.config.ConfigSpec
import me.fzzyhmstrs.fzzy_config.screen.context.ContextHandler
import me.fzzyhmstrs.fzzy_config.screen.widget.DynamicListWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.LayoutWidget
import me.fzzyhmstrs.fzzy_config.updates.UpdateManager
import net.minecraft.client.gui.Element

internal class ConfigScreenLayout(
    private val spec: ConfigSpec,
    private val updateManager: UpdateManager,
    private val builder: LayoutBuilder)
    :
    ContextHandler
{

    private var builtLayout: BuiltLayout? = null

    private fun buildLayout(screen: NewConfigScreen): BuiltLayout {
        TODO()
    }

    override fun handleContext(contextType: ContextHandler.ContextType): Boolean {
        TODO("Not yet implemented")
    }

    fun provideLayout(screen: NewConfigScreen): List<Element> {
        val built = builtLayout ?: buildLayout(screen)
        built.update(screen)
        return built.provideElements()
    }

    private class BuiltLayout(val settings: LayoutWidget, val header: LayoutWidget?, val footer: LayoutWidget?, val sidebar: LayoutWidget?) {

        private val headerHeight = 33
        private val footerHeight = 33
        private val sidebarWidth = 120
        private val sidebarCrushedWidth = 100

        fun update(screen: NewConfigScreen) {

        }

        fun provideElements(): List<Element> {
            TODO()
        }
    }

    interface LayoutBuilder {
        fun settingsList(): DynamicListWidget
        fun sidebarList(): DynamicListWidget?
    }

}