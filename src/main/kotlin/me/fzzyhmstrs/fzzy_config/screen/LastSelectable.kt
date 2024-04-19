/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.screen

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.Element

/**
 * A parent element marked as LastSelectable will cache the last selected element when a popup or other overlay is rendered, and return to that selection when the popup/overlay is removed.
 * @sample me.fzzyhmstrs.fzzy_config.examples.PopupWidgetExamples.lastSelectable
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@Environment(EnvType.CLIENT)
interface LastSelectable {
    var lastSelected: Element?
    fun pushLast()
    fun popLast()
}