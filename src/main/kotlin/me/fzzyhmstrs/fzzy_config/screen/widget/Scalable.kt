/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.screen.widget

/**
 * Provides a set width/height method for parents of implementing widgets
 *
 * Expectation is that the set methods actually change the dimensions of the widget, much like [net.minecraft.client.gui.widget.ClickableWidget]
 * @author fzzyhmstrs
 * @since 0.2.0
 */
//client
interface Scalable {
    fun setWidth(width: Int)
    fun setHeight(height: Int)
}